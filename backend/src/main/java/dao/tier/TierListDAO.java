package dao.tier;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import util.DBUtil;
import util.BookImageUrlUtil;

public class TierListDAO {

  public Map<String, Object> findLatestTierList(long memberId, long templateId) {
    if (memberId <= 0 || templateId <= 0)
      throw new IllegalArgumentException("올바른 회원과 템플릿 번호가 필요합니다.");
    String listSql =
        """
        SELECT * FROM (
            SELECT L.tier_list_id, L.title, L.description, L.created_at,
                   CASE WHEN EXISTS (
                     SELECT 1 FROM POST P
                      WHERE P.tier_list_id=L.tier_list_id AND P.status='ACTIVE'
                   ) THEN 'Y' ELSE 'N' END publish_to_community
              FROM TIER_LIST L
             WHERE L.member_id = ? AND L.template_id = ?
             ORDER BY L.created_at DESC, L.tier_list_id DESC
        ) WHERE ROWNUM = 1
        """;
    String itemSql =
        """
        SELECT book_id, tier_grade, sort_order
          FROM TIER_ITEM
         WHERE tier_list_id = ?
         ORDER BY sort_order, tier_item_id
        """;
    try (Connection connection = DBUtil.getConnection();
        PreparedStatement listStatement = connection.prepareStatement(listSql)) {
      listStatement.setLong(1, memberId);
      listStatement.setLong(2, templateId);
      try (ResultSet resultSet = listStatement.executeQuery()) {
        if (!resultSet.next()) return null;
        long tierListId = resultSet.getLong("tier_list_id");
        Map<String, Object> tierList = new LinkedHashMap<>();
        tierList.put("tierListId", tierListId);
        tierList.put("title", repairLegacyUtf8(resultSet.getString("title")));
        tierList.put("description", repairLegacyUtf8(resultSet.getString("description")));
        tierList.put(
            "publishToCommunity",
            "Y".equals(resultSet.getString("publish_to_community")));
        tierList.put("createdAt", resultSet.getTimestamp("created_at"));
        List<Map<String, Object>> placements = new ArrayList<>();
        try (PreparedStatement itemStatement = connection.prepareStatement(itemSql)) {
          itemStatement.setLong(1, tierListId);
          try (ResultSet itemResult = itemStatement.executeQuery()) {
            while (itemResult.next()) {
              placements.add(
                  Map.of(
                      "bookId", itemResult.getLong("book_id"),
                      "grade", itemResult.getString("tier_grade"),
                      "sortOrder", itemResult.getInt("sort_order")));
            }
          }
        }
        tierList.put("placements", placements);
        return tierList;
      }
    } catch (SQLException e) {
      throw new RuntimeException("저장된 티어리스트를 불러오지 못했습니다.", e);
    }
  }


  public Map<String, Object> findTierList(long tierListId) {
    if (tierListId <= 0) return null;
    String headerSql =
        """
        SELECT L.tier_list_id, L.template_id, L.member_id, L.title, L.description,
               T.description template_description, M.nickname,
               CASE WHEN EXISTS (
                 SELECT 1 FROM POST P
                  WHERE P.tier_list_id=L.tier_list_id AND P.status='ACTIVE'
               ) THEN 'Y' ELSE 'N' END published_to_community
          FROM TIER_LIST L
          JOIN MEMBER M ON M.member_id = L.member_id
          JOIN TIER_TEMPLATE T ON T.template_id = L.template_id
         WHERE L.tier_list_id = ?
        """;
    String itemSql =
        """
        SELECT I.book_id, I.tier_grade, I.sort_order, B.title, B.image_url, A.author_name
          FROM TIER_ITEM I
          JOIN BOOK B ON B.book_id = I.book_id
          JOIN AUTHOR A ON A.author_id = B.author_id
         WHERE I.tier_list_id = ?
         ORDER BY CASE I.tier_grade WHEN 'S' THEN 1 WHEN 'A' THEN 2 WHEN 'B' THEN 3 WHEN 'C' THEN 4 ELSE 5 END,
                  I.sort_order, I.tier_item_id
        """;
    try (Connection connection = DBUtil.getConnection();
        PreparedStatement header = connection.prepareStatement(headerSql)) {
      header.setLong(1, tierListId);
      Map<String, Object> result = new LinkedHashMap<>();
      try (ResultSet rs = header.executeQuery()) {
        if (!rs.next()) return null;
        result.put("tierListId", rs.getLong("tier_list_id"));
        result.put("templateId", rs.getLong("template_id"));
        result.put("memberId", rs.getLong("member_id"));
        result.put(
            "publishedToCommunity",
            "Y".equals(rs.getString("published_to_community")));
        result.put("title", rs.getString("title"));
        result.put("description", rs.getString("description"));
        result.put("templateDescription", rs.getString("template_description"));
        result.put("memberNickname", rs.getString("nickname"));
      }
      List<Map<String, Object>> items = new ArrayList<>();
      try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
        statement.setLong(1, tierListId);
        try (ResultSet rs = statement.executeQuery()) {
          while (rs.next()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bookId", rs.getLong("book_id"));
            item.put("grade", rs.getString("tier_grade"));
            item.put("sortOrder", rs.getInt("sort_order"));
            item.put("title", rs.getString("title"));
            item.put("imageUrl", BookImageUrlUtil.thumbnail(rs.getString("image_url")));
            item.put("authorName", rs.getString("author_name"));
            items.add(item);
          }
        }
      }
      result.put("items", items);
      return result;
    } catch (SQLException e) {
      throw new RuntimeException("티어리스트를 불러오지 못했습니다.", e);
    }
  }


  public long saveTierList(
      long memberId,
      long templateId,
      String description,
      boolean publishToCommunity,
      List<Placement> placements) {
    String safeDescription = optional(description, 1000);
    if (placements == null || placements.isEmpty())
      throw new IllegalArgumentException("배치한 책이 없습니다.");
    Set<String> grades = Set.of("S", "A", "B", "C", "D");
    Set<Long> seen = new HashSet<>();
    for (Placement p : placements) {
      if (p == null || p.bookId() <= 0 || !grades.contains(p.grade()) || !seen.add(p.bookId()))
        throw new IllegalArgumentException("책 배치 정보가 올바르지 않습니다.");
    }
    try (Connection connection = DBUtil.getConnection()) {
      connection.setAutoCommit(false);
      try {
        String safeTitle;
        try (PreparedStatement check =
            connection.prepareStatement(
                "SELECT title FROM TIER_TEMPLATE WHERE template_id=? AND status='APPROVED'")) {
          check.setLong(1, templateId);
          try (ResultSet rs = check.executeQuery()) {
            if (!rs.next()) throw new IllegalArgumentException("사용할 수 없는 템플릿입니다.");
            safeTitle = required(rs.getString("title"), "티어리스트 제목", 200);
          }
        }
        Long existingListId = null;
        try (PreparedStatement statement =
            connection.prepareStatement(
                "SELECT tier_list_id FROM TIER_LIST WHERE member_id=? AND template_id=? FOR"
                    + " UPDATE")) {
          statement.setLong(1, memberId);
          statement.setLong(2, templateId);
          try (ResultSet rs = statement.executeQuery()) {
            if (rs.next()) existingListId = rs.getLong(1);
          }
        }
        long listId;
        if (existingListId == null) {
          try (PreparedStatement statement =
              connection.prepareStatement(
                  "INSERT INTO TIER_LIST(tier_list_id, member_id, template_id, title, description)"
                      + " VALUES(SEQ_TIER_LIST.NEXTVAL,?,?,?,?)",
                  new String[] {"tier_list_id"})) {
            statement.setLong(1, memberId);
            statement.setLong(2, templateId);
            statement.setString(3, safeTitle);
            statement.setString(4, safeDescription);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
              keys.next();
              listId = keys.getLong(1);
            }
          }
        } else {
          listId = existingListId;
          try (PreparedStatement statement =
              connection.prepareStatement(
                  "UPDATE TIER_LIST SET title=?, description=? WHERE"
                      + " tier_list_id=?")) {
            statement.setString(1, safeTitle);
            statement.setString(2, safeDescription);
            statement.setLong(3, listId);
            statement.executeUpdate();
          }
          try (PreparedStatement statement =
              connection.prepareStatement("DELETE FROM TIER_ITEM WHERE tier_list_id=?")) {
            statement.setLong(1, listId);
            statement.executeUpdate();
          }
        }
        try (PreparedStatement statement =
            connection.prepareStatement(
                "INSERT INTO TIER_ITEM(tier_item_id,tier_list_id,book_id,tier_grade,sort_order)"
                    + " SELECT SEQ_TIER_ITEM.NEXTVAL,?,?,?,? FROM TIER_TEMPLATE_ITEM WHERE"
                    + " template_id=? AND book_id=?")) {
          int order = 0;
          for (Placement p : placements) {
            statement.setLong(1, listId);
            statement.setLong(2, p.bookId());
            statement.setString(3, p.grade());
            statement.setInt(4, order++);
            statement.setLong(5, templateId);
            statement.setLong(6, p.bookId());
            if (statement.executeUpdate() != 1)
              throw new IllegalArgumentException("템플릿에 없는 책이 포함되어 있습니다.");
          }
        }
        syncTierPost(
            connection,
            memberId,
            listId,
            safeTitle,
            safeDescription,
            publishToCommunity);
        connection.commit();
        return listId;
      } catch (Exception e) {
        connection.rollback();
        if (e instanceof IllegalArgumentException iae) throw iae;
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException("티어리스트를 저장하지 못했습니다.", e);
    }
  }


  private void syncTierPost(
      Connection connection,
      long memberId,
      long tierListId,
      String title,
      String description,
      boolean publishToCommunity)
      throws SQLException {
    if (!publishToCommunity) {
      try (PreparedStatement statement =
          connection.prepareStatement("DELETE FROM POST WHERE tier_list_id=?")) {
        statement.setLong(1, tierListId);
        statement.executeUpdate();
      }
      return;
    }
    String content = description == null || description.isBlank()
        ? title + " 티어리스트를 완성했습니다."
        : description;
    String postTitle = title + " 결과";
    String sql =
        """
        MERGE INTO POST P
        USING (SELECT ? tier_list_id FROM DUAL) S
           ON (P.tier_list_id = S.tier_list_id)
         WHEN MATCHED THEN UPDATE SET
              P.member_id = ?, P.category = 'TIER', P.title = ?, P.content = ?,
              P.genre = NULL, P.status = 'ACTIVE', P.updated_at = SYSDATE
         WHEN NOT MATCHED THEN INSERT (
              post_id, member_id, tier_list_id, category, title, content, genre,
              view_count, is_pinned, status, created_at
         ) VALUES (
              SEQ_POST.NEXTVAL, ?, ?, 'TIER', ?, ?, NULL, 0, 'N', 'ACTIVE', SYSDATE
         )
        """;
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, tierListId);
      statement.setLong(2, memberId);
      statement.setString(3, postTitle);
      statement.setString(4, content);
      statement.setLong(5, memberId);
      statement.setLong(6, tierListId);
      statement.setString(7, postTitle);
      statement.setString(8, content);
      statement.executeUpdate();
    }
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

  public record Placement(long bookId, String grade) {}
}

