package dao.ideal;

import java.sql.*;
import java.util.*;
import util.DBUtil;
import util.BookImageUrlUtil;

public class IdealStatsDAO {

  public Map<String, Object> stats(long templateId) {
    Map<String, Object> out = new IdealTemplateDAO().findTemplate(templateId);
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
}

