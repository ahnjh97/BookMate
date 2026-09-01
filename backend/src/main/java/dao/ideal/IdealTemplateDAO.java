package dao.ideal;

import java.sql.*;
import java.util.*;
import util.DBUtil;
import util.BookImageUrlUtil;

public class IdealTemplateDAO {

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
         ORDER BY CASE T.status WHEN 'PENDING' THEN 0 ELSE 1 END,
                  T.created_at DESC,
                  T.template_id DESC
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

