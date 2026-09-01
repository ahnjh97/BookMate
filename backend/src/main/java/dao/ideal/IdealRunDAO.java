package dao.ideal;

import java.sql.*;
import java.util.*;
import util.DBUtil;
import util.BookImageUrlUtil;

public class IdealRunDAO {

  public long saveRun(long memberId, long templateId, int size, List<Match> matches) {
    if (size != 8 && size != 16) throw new IllegalArgumentException("8강 또는 16강만 진행할 수 있습니다.");
    int expected = size - 1;
    if (matches == null || matches.size() != expected)
      throw new IllegalArgumentException("완료되지 않은 대진표입니다.");
    Set<Long> allowed = new HashSet<>();
    for (Object o : (List<?>) new IdealTemplateDAO().findTemplate(templateId).get("items"))
      allowed.add(((Number) ((Map<?, ?>) o).get("bookId")).longValue());
    Map<Integer, List<Match>> rounds = new HashMap<>();
    for (Match m : matches) {
      if (!Set.of(2, 4, 8, 16).contains(m.roundSize())
          || m.roundSize() > size
          || m.matchOrder() < 0
          || !allowed.containsAll(List.of(m.leftBookId(), m.rightBookId(), m.winnerBookId()))
          || (m.winnerBookId() != m.leftBookId() && m.winnerBookId() != m.rightBookId()))
        throw new IllegalArgumentException("잘못된 경기 결과가 포함되어 있습니다.");
      rounds.computeIfAbsent(m.roundSize(), x -> new ArrayList<>()).add(m);
    }
    for (int r = size; r >= 2; r /= 2)
      if (rounds.getOrDefault(r, List.of()).size() != r / 2)
        throw new IllegalArgumentException("라운드별 경기 수가 맞지 않습니다.");
    long winner = rounds.get(2).get(0).winnerBookId();
    try (Connection c = DBUtil.getConnection()) {
      c.setAutoCommit(false);
      try {
        Long existingRun = null;
        try (PreparedStatement find =
            c.prepareStatement(
                "SELECT run_id FROM IDEAL_RUN WHERE member_id=? AND template_id=? FOR UPDATE")) {
          find.setLong(1, memberId);
          find.setLong(2, templateId);
          try (ResultSet resultSet = find.executeQuery()) {
            if (resultSet.next()) existingRun = resultSet.getLong("run_id");
          }
        }

        long run;
        if (existingRun == null) {
          try (PreparedStatement insert =
              c.prepareStatement(
                  "INSERT INTO IDEAL_RUN(run_id,template_id,member_id,bracket_size,winner_book_id)"
                      + " VALUES(SEQ_IDEAL_RUN.NEXTVAL,?,?,?,?)",
                  new String[] {"run_id"})) {
            insert.setLong(1, templateId);
            insert.setLong(2, memberId);
            insert.setInt(3, size);
            insert.setLong(4, winner);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
              keys.next();
              run = keys.getLong(1);
            }
          }
        } else {
          run = existingRun;
          try (PreparedStatement update =
              c.prepareStatement(
                  "UPDATE IDEAL_RUN SET bracket_size=?, winner_book_id=?, created_at=SYSDATE"
                      + " WHERE run_id=?")) {
            update.setInt(1, size);
            update.setLong(2, winner);
            update.setLong(3, run);
            update.executeUpdate();
          }
          try (PreparedStatement delete =
              c.prepareStatement("DELETE FROM IDEAL_MATCH WHERE run_id=?")) {
            delete.setLong(1, run);
            delete.executeUpdate();
          }
        }

        try (PreparedStatement p =
            c.prepareStatement(
                "INSERT INTO"
                    + " IDEAL_MATCH(match_id,run_id,round_size,match_order,left_book_id,right_book_id,winner_book_id)"
                    + " VALUES(SEQ_IDEAL_MATCH.NEXTVAL,?,?,?,?,?,?)")) {
          for (Match m : matches) {
            p.setLong(1, run);
            p.setInt(2, m.roundSize());
            p.setInt(3, m.matchOrder());
            p.setLong(4, m.leftBookId());
            p.setLong(5, m.rightBookId());
            p.setLong(6, m.winnerBookId());
            p.addBatch();
          }
          p.executeBatch();
        }
        c.commit();
        return run;
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("월드컵 결과를 저장하지 못했습니다.", e);
    }
  }


  public Map<String, Object> result(long runId) {
    String sql =
        """
        SELECT R.template_id,R.bracket_size,R.created_at,T.title,M.round_size,M.match_order,
        L.book_id,L.title,L.image_url,RR.book_id,RR.title,RR.image_url,W.book_id,W.title,W.image_url
        FROM IDEAL_RUN R JOIN IDEAL_TEMPLATE T ON T.template_id=R.template_id JOIN IDEAL_MATCH M ON M.run_id=R.run_id
        JOIN BOOK L ON L.book_id=M.left_book_id JOIN BOOK RR ON RR.book_id=M.right_book_id JOIN BOOK W ON W.book_id=M.winner_book_id
        WHERE R.run_id=? ORDER BY M.round_size DESC,M.match_order
        """;
    try (Connection c = DBUtil.getConnection();
        PreparedStatement p = c.prepareStatement(sql)) {
      p.setLong(1, runId);
      Map<String, Object> out = new LinkedHashMap<>();
      List<Map<String, Object>> games = new ArrayList<>();
      try (ResultSet rs = p.executeQuery()) {
        while (rs.next()) {
          if (out.isEmpty()) {
            out.put("runId", runId);
            out.put("templateId", rs.getLong(1));
            out.put("bracketSize", rs.getInt(2));
            out.put("createdAt", rs.getTimestamp(3));
            out.put("title", rs.getString(4));
          }
          games.add(
              Map.of(
                  "roundSize",
                  rs.getInt(5),
                  "matchOrder",
                  rs.getInt(6),
                  "left",
                  book(rs, 7),
                  "right",
                  book(rs, 10),
                  "winner",
                  book(rs, 13)));
        }
      }
      if (out.isEmpty()) throw new NoSuchElementException("월드컵 결과를 찾을 수 없습니다.");
      out.put("matches", games);
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("월드컵 결과를 불러오지 못했습니다.", e);
    }
  }


  public Map<String, Object> result(long runId, Long viewerId) {
    Map<String, Object> result = result(runId);
    String sql =
        "SELECT R.member_id,P.post_id FROM IDEAL_RUN R LEFT JOIN POST P ON P.ideal_run_id=R.run_id"
            + " AND P.status='ACTIVE' WHERE R.run_id=?";
    try (Connection connection = DBUtil.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, runId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) throw new NoSuchElementException("월드컵 결과를 찾을 수 없습니다.");
        long ownerId = resultSet.getLong("member_id");
        long postId = resultSet.getLong("post_id");
        boolean published = !resultSet.wasNull();
        result.put("owner", viewerId != null && viewerId == ownerId);
        result.put("publishedToCommunity", published);
        result.put("postId", published ? postId : null);
      }
      return result;
    } catch (SQLException e) {
      throw new RuntimeException("월드컵 공유 상태를 확인하지 못했습니다.", e);
    }
  }


  public long publishResult(long runId, long memberId, String postContent) {
    String sharedContent = required(postContent, "게시글 내용", 1000);
    String resultSql =
        "SELECT T.title,B.title winner_title FROM IDEAL_RUN R"
            + " JOIN IDEAL_TEMPLATE T ON T.template_id=R.template_id"
            + " JOIN BOOK B ON B.book_id=R.winner_book_id"
            + " WHERE R.run_id=? AND R.member_id=?";
    String mergeSql =
        """
        MERGE INTO POST P
        USING (SELECT ? ideal_run_id FROM DUAL) S
           ON (P.ideal_run_id=S.ideal_run_id)
        WHEN MATCHED THEN UPDATE SET P.category='WORLDCUP',P.title=?,P.content=?,P.genre=NULL,
             P.status='ACTIVE',P.updated_at=SYSDATE
        WHEN NOT MATCHED THEN INSERT (
             post_id,member_id,ideal_run_id,category,title,content,genre,view_count,is_pinned,status,created_at
        ) VALUES (SEQ_POST.NEXTVAL,?,?,'WORLDCUP',?,?,NULL,0,'N','ACTIVE',SYSDATE)
        """;
    try (Connection connection = DBUtil.getConnection()) {
      connection.setAutoCommit(false);
      try {
        String title;
        String content;
        try (PreparedStatement statement = connection.prepareStatement(resultSql)) {
          statement.setLong(1, runId);
          statement.setLong(2, memberId);
          try (ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) throw new NoSuchElementException("공유할 수 있는 월드컵 결과를 찾을 수 없습니다.");
            title = resultSet.getString("title") + " 결과";
            content = sharedContent;
          }
        }
        try (PreparedStatement statement = connection.prepareStatement(mergeSql)) {
          statement.setLong(1, runId);
          statement.setString(2, title);
          statement.setString(3, content);
          statement.setLong(4, memberId);
          statement.setLong(5, runId);
          statement.setString(6, title);
          statement.setString(7, content);
          statement.executeUpdate();
        }
        long postId;
        try (PreparedStatement statement =
            connection.prepareStatement("SELECT post_id FROM POST WHERE ideal_run_id=?")) {
          statement.setLong(1, runId);
          try (ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) throw new SQLException("생성된 게시글을 찾을 수 없습니다.");
            postId = resultSet.getLong(1);
          }
        }
        connection.commit();
        return postId;
      } catch (Exception e) {
        connection.rollback();
        throw e;
      }
    } catch (NoSuchElementException e) {
      throw e;
    } catch (SQLException e) {
      throw new RuntimeException("월드컵 결과를 커뮤니티에 공유하지 못했습니다.", e);
    }
  }


  private Map<String, Object> book(ResultSet rs, int i) throws SQLException {
    return Map.of(
        "bookId",
        rs.getLong(i),
        "title",
        rs.getString(i + 1),
        "imageUrl",
        Objects.toString(BookImageUrlUtil.thumbnail(rs.getString(i + 2)), ""));
  }


  private String required(String v, String name, int max) {
    if (v == null || v.isBlank()) throw new IllegalArgumentException(name + "을(를) 입력해 주세요.");
    String s = v.trim();
    if (s.length() > max) throw new IllegalArgumentException(name + "은(는) " + max + "자 이하여야 합니다.");
    return s;
  }


  private String optional(String v, int max) {
    if (v == null || v.isBlank()) return null;
    String s = v.trim();
    if (s.length() > max) throw new IllegalArgumentException("소개는 " + max + "자 이하여야 합니다.");
    return s;
  }

  public record Match(int roundSize, int matchOrder, long leftBookId, long rightBookId, long winnerBookId) {}
}
