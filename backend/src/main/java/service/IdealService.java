package service;

import java.sql.*;
import java.util.*;
import util.DBUtil;
import util.BookImageUrlUtil;

public class IdealService {
  public record Match(
      int roundSize, int matchOrder, long leftBookId, long rightBookId, long winnerBookId) {}

  public List<Map<String, Object>> findTemplates(String keyword, Long memberId) {
    return findTemplates(keyword, memberId, null, false);
  }

  public List<Map<String, Object>> findTemplates(String keyword, Long memberId, Long bookId) {
    return findTemplates(keyword, memberId, bookId, false);
  }

  public List<Map<String, Object>> findTemplates(
      String keyword, Long memberId, Long bookId, boolean includePending) {
    String sql =
        """
        WITH RATING_COUNT AS (
          SELECT R.book_id, COUNT(*) rating_count
            FROM RATING R
           WHERE EXISTS (SELECT 1 FROM IDEAL_TEMPLATE_ITEM X WHERE X.book_id=R.book_id)
           GROUP BY R.book_id
        ), COVER_BOOK AS (
          SELECT I.template_id, B.image_url,
                 ROW_NUMBER() OVER (
                   PARTITION BY I.template_id
                   ORDER BY NVL(R.rating_count, 0) DESC, I.sort_order, B.book_id
                 ) cover_rank
            FROM IDEAL_TEMPLATE_ITEM I
            JOIN BOOK B ON B.book_id = I.book_id
            LEFT JOIN RATING_COUNT R ON R.book_id = B.book_id
        ), COVER_SUMMARY AS (
          SELECT template_id,
                 MAX(CASE WHEN cover_rank = 1 THEN image_url END) cover_image_1,
                 MAX(CASE WHEN cover_rank = 2 THEN image_url END) cover_image_2
            FROM COVER_BOOK WHERE cover_rank <= 2 GROUP BY template_id
        )
        SELECT T.template_id,T.title,T.description,T.category,T.status,T.created_at,M.nickname,COUNT(DISTINCT I.book_id) item_count,
               MAX(C.cover_image_1) cover_image_1, MAX(C.cover_image_2) cover_image_2,
               CASE WHEN COUNT(R.run_id)>0 THEN 'Y' ELSE 'N' END participated
          FROM IDEAL_TEMPLATE T JOIN MEMBER M ON M.member_id=T.member_id
          JOIN IDEAL_TEMPLATE_ITEM I ON I.template_id=T.template_id
          LEFT JOIN COVER_SUMMARY C ON C.template_id=T.template_id
          LEFT JOIN IDEAL_RUN R ON R.template_id=T.template_id AND R.member_id=?
         WHERE (? = 'Y' OR T.status='APPROVED')
           AND (? IS NULL OR LOWER(T.title) LIKE '%'||LOWER(?)||'%' OR LOWER(T.description) LIKE '%'||LOWER(?)||'%')
           AND (? IS NULL OR EXISTS (
               SELECT 1 FROM IDEAL_TEMPLATE_ITEM BI WHERE BI.template_id=T.template_id AND BI.book_id=?
           ))
         GROUP BY T.template_id,T.title,T.description,T.category,T.status,M.nickname,T.created_at
         ORDER BY CASE T.status WHEN 'PENDING' THEN 0 ELSE 1 END, T.created_at DESC
        """;
    List<Map<String, Object>> out = new ArrayList<>();
    try (Connection c = DBUtil.getConnection();
        PreparedStatement ps = c.prepareStatement(sql)) {
      if (memberId == null) ps.setNull(1, Types.NUMERIC);
      else ps.setLong(1, memberId);
      ps.setString(2, includePending ? "Y" : "N");
      String q = keyword == null || keyword.isBlank() ? null : keyword.trim();
      for (int i = 3; i <= 5; i++) ps.setString(i, q);
      if (bookId == null) {
        ps.setNull(6, Types.NUMERIC);
        ps.setNull(7, Types.NUMERIC);
      } else {
        ps.setLong(6, bookId);
        ps.setLong(7, bookId);
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> template = new LinkedHashMap<>();
          template.put("templateId", rs.getLong("template_id"));
          template.put("title", rs.getString("title"));
          template.put("description", Objects.toString(rs.getString("description"), ""));
          template.put("category", rs.getString("category"));
          template.put("status", rs.getString("status"));
          template.put("requestedAt", rs.getTimestamp("created_at"));
          template.put("creatorNickname", rs.getString("nickname"));
          template.put("itemCount", rs.getInt("item_count"));
          List<String> coverImages = new ArrayList<>();
          for (int index = 1; index <= 2; index++) {
            String imageUrl = rs.getString("cover_image_" + index);
            if (imageUrl != null && !imageUrl.isBlank()) coverImages.add(BookImageUrlUtil.thumbnail(imageUrl));
          }
          template.put("coverImages", coverImages);
          template.put("participated", "Y".equals(rs.getString("participated")));
          out.add(template);
        }
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("월드컵 템플릿을 불러오지 못했습니다.", e);
    }
  }

  public Map<String, Object> findTemplate(long id) {
    if (id <= 0) throw new IllegalArgumentException("올바른 템플릿 번호가 필요합니다.");
    String head =
        "SELECT T.title,T.description,T.category,M.nickname FROM IDEAL_TEMPLATE T JOIN MEMBER M ON"
            + " M.member_id=T.member_id WHERE T.template_id=? AND T.status='APPROVED'";
    String items =
        "SELECT B.book_id,B.title,A.author_name,B.image_url FROM IDEAL_TEMPLATE_ITEM I JOIN BOOK B"
            + " ON B.book_id=I.book_id JOIN AUTHOR A ON A.author_id=B.author_id WHERE"
            + " I.template_id=? ORDER BY I.sort_order";
    try (Connection c = DBUtil.getConnection();
        PreparedStatement h = c.prepareStatement(head);
        PreparedStatement p = c.prepareStatement(items)) {
      h.setLong(1, id);
      Map<String, Object> result = new LinkedHashMap<>();
      try (ResultSet rs = h.executeQuery()) {
        if (!rs.next()) throw new NoSuchElementException("월드컵 템플릿을 찾을 수 없습니다.");
        result.put("templateId", id);
        result.put("title", rs.getString(1));
        result.put("description", Objects.toString(rs.getString(2), ""));
        result.put("category", rs.getString(3));
        result.put("creatorNickname", rs.getString(4));
      }
      p.setLong(1, id);
      List<Map<String, Object>> books = new ArrayList<>();
      try (ResultSet rs = p.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> b = new LinkedHashMap<>();
          b.put("bookId", rs.getLong(1));
          b.put("title", rs.getString(2));
          b.put("authorName", rs.getString(3));
          b.put("imageUrl", BookImageUrlUtil.thumbnail(rs.getString(4)));
          books.add(b);
        }
      }
      result.put("items", books);
      return result;
    } catch (SQLException e) {
      throw new RuntimeException("월드컵 템플릿을 불러오지 못했습니다.", e);
    }
  }

  public Map<String, Object> findTemplate(long id, Long memberId) {
    Map<String, Object> template = findTemplate(id);
    Long runId = memberId == null ? null : findRunId(memberId, id);
    template.put("participated", runId != null);
    template.put("runId", runId);
    return template;
  }

  private Long findRunId(long memberId, long templateId) {
    String sql = "SELECT run_id FROM IDEAL_RUN WHERE member_id=? AND template_id=?";
    try (Connection connection = DBUtil.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, memberId);
      statement.setLong(2, templateId);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? resultSet.getLong("run_id") : null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("월드컵 참여 여부를 확인하지 못했습니다.", e);
    }
  }

  public long createTemplate(
      long memberId, String title, String description, String category, List<Long> ids) {
    String safe = required(title, "템플릿 이름", 200);
    String safeCategory = required(category, "카테고리", 20);
    if (!Set.of("자유", "장르").contains(safeCategory)) {
      throw new IllegalArgumentException("카테고리는 자유 또는 장르만 선택할 수 있습니다.");
    }
    LinkedHashSet<Long> unique = new LinkedHashSet<>(ids == null ? List.of() : ids);
    if (unique.size() < 16) throw new IllegalArgumentException("16강 진행을 위해 책을 16권 이상 선택해 주세요.");
    if (unique.size() > 64) throw new IllegalArgumentException("책은 최대 64권까지 담을 수 있습니다.");
    try (Connection c = DBUtil.getConnection()) {
      c.setAutoCommit(false);
      try (PreparedStatement h =
          c.prepareStatement(
              "INSERT INTO IDEAL_TEMPLATE(template_id,member_id,title,description,category,status)"
                  + " VALUES(SEQ_IDEAL_TEMPLATE.NEXTVAL,?,?,?,?, 'PENDING')",
              new String[] {"template_id"})) {
        h.setLong(1, memberId);
        h.setString(2, safe);
        h.setString(3, optional(description, 1000));
        h.setString(4, safeCategory);
        h.executeUpdate();
        long id;
        try (ResultSet k = h.getGeneratedKeys()) {
          k.next();
          id = k.getLong(1);
        }
        try (PreparedStatement p =
            c.prepareStatement(
                "INSERT INTO IDEAL_TEMPLATE_ITEM(template_item_id,template_id,book_id,sort_order)"
                    + " SELECT SEQ_IDEAL_TEMPLATE_ITEM.NEXTVAL,?,?,? FROM BOOK WHERE book_id=? AND"
                    + " status='APPROVED'")) {
          int order = 0;
          for (long book : unique) {
            p.setLong(1, id);
            p.setLong(2, book);
            p.setInt(3, order++);
            p.setLong(4, book);
            p.addBatch();
          }
          int[] inserted = p.executeBatch();
          if (Arrays.stream(inserted).anyMatch(count -> count == 0))
            throw new IllegalArgumentException("사용할 수 없는 책이 포함되어 있습니다.");
        }
        c.commit();
        return id;
      } catch (Exception e) {
        c.rollback();
        if (e instanceof IllegalArgumentException i) throw i;
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("월드컵 템플릿을 만들지 못했습니다.", e);
    }
  }

  public void reviewTemplate(long templateId, long adminId, boolean approved, String reason) {
    if (templateId <= 0) throw new IllegalArgumentException("올바른 템플릿 번호가 필요합니다.");
    if (!approved && (reason == null || reason.isBlank()))
      throw new IllegalArgumentException("반려 사유를 입력해 주세요.");
    String sql =
        "UPDATE IDEAL_TEMPLATE SET status=?,admin_id=?,reject_reason=?,processed_at=SYSDATE"
            + " WHERE template_id=? AND status='PENDING'";
    try (Connection connection = DBUtil.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, approved ? "APPROVED" : "REJECTED");
      statement.setLong(2, adminId);
      statement.setString(3, approved ? null : optional(reason, 1000));
      statement.setLong(4, templateId);
      if (statement.executeUpdate() != 1)
        throw new NoSuchElementException("대기 중인 월드컵 템플릿을 찾을 수 없습니다.");
    } catch (SQLException e) {
      throw new RuntimeException("월드컵 템플릿 검토 결과를 저장하지 못했습니다.", e);
    }
  }

  public long saveRun(long memberId, long templateId, int size, List<Match> matches) {
    if (size != 8 && size != 16) throw new IllegalArgumentException("8강 또는 16강만 진행할 수 있습니다.");
    int expected = size - 1;
    if (matches == null || matches.size() != expected)
      throw new IllegalArgumentException("완료되지 않은 대진표입니다.");
    Set<Long> allowed = new HashSet<>();
    for (Object o : (List<?>) findTemplate(templateId).get("items"))
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

  public Map<String, Object> stats(long templateId) {
    Map<String, Object> out = findTemplate(templateId);
    String sql =
        """
        SELECT B.book_id,B.title,A.author_name,B.image_url,
        COUNT(DISTINCT M.run_id) appearances,COUNT(DISTINCT CASE WHEN R.winner_book_id=B.book_id THEN R.run_id END) championships,
        COUNT(DISTINCT CASE WHEN M.round_size=2 THEN M.run_id END) finals,
        COUNT(M.match_id) matches,COUNT(CASE WHEN M.winner_book_id=B.book_id THEN 1 END) wins
        FROM IDEAL_TEMPLATE_ITEM I JOIN BOOK B ON B.book_id=I.book_id JOIN AUTHOR A ON A.author_id=B.author_id
        LEFT JOIN IDEAL_RUN R ON R.template_id=I.template_id
        LEFT JOIN IDEAL_MATCH M ON M.run_id=R.run_id AND (M.left_book_id=B.book_id OR M.right_book_id=B.book_id)
        WHERE I.template_id=? GROUP BY B.book_id,B.title,A.author_name,B.image_url,I.sort_order ORDER BY championships DESC,wins DESC,I.sort_order
        """;
    List<Map<String, Object>> list = new ArrayList<>();
    try (Connection c = DBUtil.getConnection();
        PreparedStatement p = c.prepareStatement(sql)) {
      p.setLong(1, templateId);
      try (ResultSet rs = p.executeQuery()) {
        while (rs.next()) {
          int app = rs.getInt(5), champ = rs.getInt(6), matches = rs.getInt(8), wins = rs.getInt(9);
          Map<String, Object> x = new LinkedHashMap<>();
          x.put("bookId", rs.getLong(1));
          x.put("title", rs.getString(2));
          x.put("authorName", rs.getString(3));
          x.put("imageUrl", BookImageUrlUtil.compactThumbnail(rs.getString(4)));
          x.put("appearances", app);
          x.put("championships", champ);
          x.put("finals", rs.getInt(7));
          x.put("matches", matches);
          x.put("wins", wins);
          x.put("championshipRate", app == 0 ? 0 : champ * 100.0 / app);
          x.put("winRate", matches == 0 ? 0 : wins * 100.0 / matches);
          list.add(x);
        }
      }
      try (PreparedStatement q =
          c.prepareStatement("SELECT COUNT(*) FROM IDEAL_RUN WHERE template_id=?")) {
        q.setLong(1, templateId);
        try (ResultSet rs = q.executeQuery()) {
          rs.next();
          out.put("totalRuns", rs.getInt(1));
        }
      }
      out.put("stats", list);
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("전체 통계를 불러오지 못했습니다.", e);
    }
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
}
