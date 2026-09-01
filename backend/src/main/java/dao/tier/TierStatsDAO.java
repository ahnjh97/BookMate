package dao.tier;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import util.DBUtil;
import util.BookImageUrlUtil;

public class TierStatsDAO {

  public Map<String, Object> findTemplateStats(long templateId) {
    Map<String, Object> template = new TierTemplateDAO().findTemplate(templateId);
    String sql =
        """
        SELECT B.book_id, B.title, A.author_name, B.image_url, I.tier_grade, COUNT(I.tier_item_id) grade_count
          FROM TIER_TEMPLATE_ITEM TI
          JOIN BOOK B ON B.book_id = TI.book_id
          JOIN AUTHOR A ON A.author_id = B.author_id
          LEFT JOIN TIER_LIST L ON L.template_id = TI.template_id
          LEFT JOIN TIER_ITEM I ON I.tier_list_id = L.tier_list_id AND I.book_id = TI.book_id
         WHERE TI.template_id = ?
         GROUP BY B.book_id, B.title, A.author_name, B.image_url, I.tier_grade, TI.sort_order
         ORDER BY TI.sort_order
        """;
    LinkedHashMap<Long, Map<String, Object>> books = new LinkedHashMap<>();
    try (Connection connection = DBUtil.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, templateId);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          long bookId = rs.getLong("book_id");
          Map<String, Object> book =
              books.computeIfAbsent(
                  bookId,
                  ignored -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    try {
                      value.put("bookId", bookId);
                      value.put("title", rs.getString("title"));
                      value.put("authorName", rs.getString("author_name"));
                      value.put("imageUrl", BookImageUrlUtil.compactThumbnail(rs.getString("image_url")));
                    } catch (SQLException e) {
                      throw new RuntimeException(e);
                    }
                    value.put("counts", new LinkedHashMap<String, Integer>());
                    return value;
                  });
          String grade = rs.getString("tier_grade");
          if (grade != null)
            ((Map<String, Integer>) book.get("counts")).put(grade, rs.getInt("grade_count"));
        }
      }
      for (Map<String, Object> book : books.values()) {
        Map<String, Integer> counts = (Map<String, Integer>) book.get("counts");
        String dominant =
            List.of("S", "A", "B", "C", "D").stream()
                .max(Comparator.comparingInt(g -> counts.getOrDefault(g, 0)))
                .orElse("-");
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        book.put("dominantGrade", total == 0 ? "-" : dominant);
        book.put("totalPlacements", total);
      }
      try (PreparedStatement countStatement =
          connection.prepareStatement("SELECT COUNT(*) FROM TIER_LIST WHERE template_id = ?")) {
        countStatement.setLong(1, templateId);
        try (ResultSet countResult = countStatement.executeQuery()) {
          countResult.next();
          template.put("communityListCount", countResult.getInt(1));
        }
      }
      template.put("stats", new ArrayList<>(books.values()));
      return template;
    } catch (SQLException e) {
      throw new RuntimeException("티어리스트 통계를 불러오지 못했습니다.", e);
    }
  }
}

