package dao.tier;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import util.DBUtil;
import util.BookImageUrlUtil;

public class TierTemplateDAO {
  public List<Map<String, Object>> findTemplates(String keyword, boolean includePending) {
    return findTemplates(keyword, includePending, null, null);
  }


  public List<Map<String, Object>> findTemplates(
      String keyword, boolean includePending, Long memberId, Long bookId) {
    String sql =
        """
        WITH RATING_COUNT AS (
          SELECT R.book_id, COUNT(*) rating_count
            FROM RATING R
           WHERE EXISTS (SELECT 1 FROM TIER_TEMPLATE_ITEM X WHERE X.book_id=R.book_id)
           GROUP BY R.book_id
        ), COVER_BOOK AS (
          SELECT I.template_id, B.image_url,
                 ROW_NUMBER() OVER (
                   PARTITION BY I.template_id
                   ORDER BY NVL(R.rating_count, 0) DESC, I.sort_order, B.book_id
                 ) cover_rank
            FROM TIER_TEMPLATE_ITEM I
            JOIN BOOK B ON B.book_id = I.book_id
            LEFT JOIN RATING_COUNT R ON R.book_id = B.book_id
        ), COVER_SUMMARY AS (
          SELECT template_id,
                 MAX(CASE WHEN cover_rank = 1 THEN image_url END) cover_image_1,
                 MAX(CASE WHEN cover_rank = 2 THEN image_url END) cover_image_2
            FROM COVER_BOOK WHERE cover_rank <= 2 GROUP BY template_id
        )
        SELECT T.template_id, T.title, T.description, T.category, T.status,
               T.requested_at, M.nickname, COUNT(DISTINCT I.template_item_id) item_count,
               MAX(C.cover_image_1) cover_image_1, MAX(C.cover_image_2) cover_image_2,
               CASE WHEN COUNT(DISTINCT L.tier_list_id) > 0 THEN 'Y' ELSE 'N' END participated
          FROM TIER_TEMPLATE T
          JOIN MEMBER M ON M.member_id = T.member_id
          LEFT JOIN TIER_TEMPLATE_ITEM I ON I.template_id = T.template_id
          LEFT JOIN COVER_SUMMARY C ON C.template_id = T.template_id
          LEFT JOIN TIER_LIST L ON L.template_id = T.template_id AND L.member_id = ?
         WHERE (? = 'Y' OR T.status = 'APPROVED')
           AND (? IS NULL OR LOWER(T.title) LIKE ?)
           AND (? IS NULL OR EXISTS (
               SELECT 1 FROM TIER_TEMPLATE_ITEM BI WHERE BI.template_id = T.template_id AND BI.book_id = ?
           ))
         GROUP BY T.template_id, T.title, T.description, T.category, T.status,
                  T.requested_at, M.nickname
         ORDER BY CASE T.status WHEN 'PENDING' THEN 0 ELSE 1 END,
                  T.requested_at DESC,
                  T.template_id DESC
        """;
    String normalized = normalize(keyword);
    String pattern = normalized == null ? null : "%" + normalized.toLowerCase() + "%";
    List<Map<String, Object>> result = new ArrayList<>();
    try (Connection connection = DBUtil.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      if (memberId == null) statement.setNull(1, Types.NUMERIC);
      else statement.setLong(1, memberId);
      statement.setString(2, includePending ? "Y" : "N");
      statement.setString(3, normalized);
      statement.setString(4, pattern);
      if (bookId == null) {
        statement.setNull(5, Types.NUMERIC);
        statement.setNull(6, Types.NUMERIC);
      } else {
        statement.setLong(5, bookId);
        statement.setLong(6, bookId);
      }
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) result.add(templateSummary(rs));
      }
      return result;
    } catch (SQLException e) {
      throw new RuntimeException("티어리스트 템플릿을 불러오지 못했습니다.", e);
    }
  }


  public Map<String, Object> findTemplate(long templateId) {
    if (templateId <= 0) throw new IllegalArgumentException("올바른 템플릿 번호가 필요합니다.");
    String headerSql =
        """
        SELECT T.template_id, T.title, T.description, T.category, T.status, M.nickname
          FROM TIER_TEMPLATE T JOIN MEMBER M ON M.member_id = T.member_id
         WHERE T.template_id = ? AND T.status = 'APPROVED'
        """;
    String itemSql =
        """
        SELECT B.book_id, B.title, A.author_name, B.image_url, B.genre
          FROM TIER_TEMPLATE_ITEM I
          JOIN BOOK B ON B.book_id = I.book_id
          JOIN AUTHOR A ON A.author_id = B.author_id
         WHERE I.template_id = ? ORDER BY I.sort_order
        """;
    try (Connection connection = DBUtil.getConnection()) {
      Map<String, Object> template;
      try (PreparedStatement statement = connection.prepareStatement(headerSql)) {
        statement.setLong(1, templateId);
        try (ResultSet rs = statement.executeQuery()) {
          if (!rs.next()) throw new NoSuchElementException("승인된 템플릿을 찾을 수 없습니다.");
          template = new LinkedHashMap<>();
          template.put("templateId", rs.getLong("template_id"));
          template.put("title", rs.getString("title"));
          template.put("description", rs.getString("description"));
          template.put("category", rs.getString("category"));
          template.put("creatorNickname", rs.getString("nickname"));
        }
      }
      List<Map<String, Object>> items = new ArrayList<>();
      try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
        statement.setLong(1, templateId);
        try (ResultSet rs = statement.executeQuery()) {
          while (rs.next()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bookId", rs.getLong("book_id"));
            item.put("title", rs.getString("title"));
            item.put("authorName", rs.getString("author_name"));
            item.put("imageUrl", BookImageUrlUtil.thumbnail(rs.getString("image_url")));
            item.put("genre", rs.getString("genre"));
            items.add(item);
          }
        }
      }
      template.put("items", items);
      return template;
    } catch (SQLException e) {
      throw new RuntimeException("템플릿을 불러오지 못했습니다.", e);
    }
  }


  public long requestTemplate(
      long memberId, String title, String description, String category, List<Long> bookIds) {
    String safeTitle = required(title, "템플릿 제목", 200);
    String safeCategory = required(category, "카테고리", 50);
    String safeDescription = optional(description, 1000);
    LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(bookIds == null ? List.of() : bookIds);
    uniqueIds.removeIf(id -> id == null || id <= 0);
    if (uniqueIds.size() < 3) throw new IllegalArgumentException("책을 3권 이상 선택해 주세요.");
    if (uniqueIds.size() > 100)
      throw new IllegalArgumentException("한 템플릿에는 책을 최대 100권까지 담을 수 있습니다.");

    try (Connection connection = DBUtil.getConnection()) {
      connection.setAutoCommit(false);
      try {
        long templateId;
        try (PreparedStatement statement =
            connection.prepareStatement(
                "INSERT INTO TIER_TEMPLATE(template_id, member_id, title, description, category)"
                    + " VALUES(SEQ_TIER_TEMPLATE.NEXTVAL, ?, ?, ?, ?)",
                new String[] {"template_id"})) {
          statement.setLong(1, memberId);
          statement.setString(2, safeTitle);
          statement.setString(3, safeDescription);
          statement.setString(4, safeCategory);
          statement.executeUpdate();
          try (ResultSet keys = statement.getGeneratedKeys()) {
            keys.next();
            templateId = keys.getLong(1);
          }
        }
        try (PreparedStatement statement =
            connection.prepareStatement(
                "INSERT INTO TIER_TEMPLATE_ITEM(template_item_id, template_id, book_id, sort_order)"
                    + " SELECT SEQ_TIER_TEMPLATE_ITEM.NEXTVAL, ?, book_id, ? FROM BOOK WHERE"
                    + " book_id = ? AND status = 'APPROVED'")) {
          int order = 0;
          for (Long bookId : uniqueIds) {
            statement.setLong(1, templateId);
            statement.setInt(2, order++);
            statement.setLong(3, bookId);
            statement.addBatch();
          }
          int[] inserted = statement.executeBatch();
          if (Arrays.stream(inserted).anyMatch(count -> count == 0))
            throw new IllegalArgumentException("선택한 책 중 사용할 수 없는 항목이 있습니다.");
        }
        connection.commit();
        return templateId;
      } catch (Exception e) {
        connection.rollback();
        if (e instanceof IllegalArgumentException iae) throw iae;
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("템플릿 신청을 저장하지 못했습니다.", e);
    }
  }


  public void reviewTemplate(long templateId, long adminId, boolean approved, String reason) {
    if (templateId <= 0) throw new IllegalArgumentException("올바른 템플릿 번호가 필요합니다.");
    if (!approved && normalize(reason) == null)
      throw new IllegalArgumentException("반려 사유를 입력해 주세요.");
    String sql =
        "UPDATE TIER_TEMPLATE SET status=?, admin_id=?, reject_reason=?, processed_at=SYSDATE WHERE"
            + " template_id=? AND status='PENDING'";
    try (Connection connection = DBUtil.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, approved ? "APPROVED" : "REJECTED");
      statement.setLong(2, adminId);
      statement.setString(3, approved ? null : optional(reason, 1000));
      statement.setLong(4, templateId);
      if (statement.executeUpdate() != 1) throw new NoSuchElementException("대기 중인 템플릿을 찾을 수 없습니다.");
    } catch (SQLException e) {
      throw new RuntimeException("템플릿 검토 결과를 저장하지 못했습니다.", e);
    }
  }


  private Map<String, Object> templateSummary(ResultSet rs) throws SQLException {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("templateId", rs.getLong("template_id"));
    item.put("title", rs.getString("title"));
    item.put("description", rs.getString("description"));
    item.put("category", rs.getString("category"));
    item.put("status", rs.getString("status"));
    item.put("creatorNickname", rs.getString("nickname"));
    item.put("itemCount", rs.getInt("item_count"));
    List<String> coverImages = new ArrayList<>();
    for (int index = 1; index <= 2; index++) {
      String imageUrl = rs.getString("cover_image_" + index);
      if (imageUrl != null && !imageUrl.isBlank()) coverImages.add(BookImageUrlUtil.thumbnail(imageUrl));
    }
    item.put("coverImages", coverImages);
    item.put("participated", "Y".equals(rs.getString("participated")));
    item.put("requestedAt", rs.getTimestamp("requested_at"));
    return item;
  }


  private String required(String value, String label, int max) {
    String normalized = normalize(value);
    if (normalized == null) throw new IllegalArgumentException(label + "을(를) 입력해 주세요.");
    if (normalized.length() > max)
      throw new IllegalArgumentException(label + "은(는) " + max + "자 이하여야 합니다.");
    return normalized;
  }


  private String optional(String value, int max) {
    String normalized = normalize(value);
    if (normalized != null && normalized.length() > max)
      throw new IllegalArgumentException("설명은 " + max + "자 이하여야 합니다.");
    return normalized;
  }


  private String normalize(String value) {
    return value == null || value.trim().isEmpty() ? null : value.trim();
  }


  private String repairLegacyUtf8(String value) {
    if (value == null
        || !(value.contains("Ã")
            || value.contains("ì")
            || value.contains("ë")
            || value.contains("ê")
            || value.contains("í"))) return value;
    String repaired =
        new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    return repaired.indexOf('\uFFFD') >= 0 ? value : repaired;
  }
}

