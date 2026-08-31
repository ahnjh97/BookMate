package service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import util.DBUtil;
import util.BookImageUrlUtil;

public class BookshelfService {
    public Map<String, Object> findBookshelf(long memberId) {
        if (memberId <= 0) throw new IllegalArgumentException("회원 번호가 올바르지 않습니다.");

        try (Connection connection = DBUtil.getConnection()) {
            Map<String, Object> bookshelf = loadMember(connection, memberId);
            if (bookshelf == null) return null;
            bookshelf.put("counts", loadCounts(connection, memberId));
            Map<String, Object> ratingPage = loadRatingPage(connection, memberId, null, 1, 10);
            bookshelf.put("favoriteBooks", ratingPage.get("books"));
            bookshelf.put("favoriteBooksTotal", ratingPage.get("totalCount"));
            bookshelf.put("tierLists", loadTierLists(connection, memberId));
            bookshelf.put("worldcupResults", loadWorldcupResults(connection, memberId));
            return bookshelf;
        } catch (SQLException exception) {
            throw new RuntimeException("회원 책장을 불러오지 못했습니다.", exception);
        }
    }

    private Map<String, Object> loadMember(Connection connection, long memberId) throws SQLException {
        String sql = "SELECT member_id,nickname FROM MEMBER WHERE member_id=? AND role='USER'"
                + " AND login_id<>'bookmate_system'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                Map<String, Object> member = new LinkedHashMap<>();
                member.put("memberId", resultSet.getLong("member_id"));
                member.put("nickname", resultSet.getString("nickname"));
                return member;
            }
        }
    }

    private Map<String, Object> loadCounts(Connection connection, long memberId) throws SQLException {
        String sql = """
                SELECT (SELECT COUNT(*) FROM RATING WHERE member_id=?) rating_count,
                       (SELECT NVL(AVG(score),0) FROM RATING WHERE member_id=?) rating_average,
                       (SELECT COUNT(*) FROM TIER_LIST WHERE member_id=?) tier_count,
                       (SELECT COUNT(*) FROM IDEAL_RUN WHERE member_id=?) worldcup_count
                  FROM DUAL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            statement.setLong(2, memberId);
            statement.setLong(3, memberId);
            statement.setLong(4, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                Map<String, Object> counts = new LinkedHashMap<>();
                counts.put("ratings", resultSet.getInt("rating_count"));
                counts.put("ratingAverage", resultSet.getDouble("rating_average"));
                counts.put("tierLists", resultSet.getInt("tier_count"));
                counts.put("worldcups", resultSet.getInt("worldcup_count"));
                return counts;
            }
        }
    }

    public Map<String, Object> findRatingPage(long memberId, Integer score, int page, int size) {
        if (memberId <= 0) throw new IllegalArgumentException("회원 번호가 올바르지 않습니다.");
        if (score != null && (score < 1 || score > 5)) throw new IllegalArgumentException("별점은 1점부터 5점까지 선택할 수 있습니다.");
        int safeSize = Math.min(Math.max(size, 1), 50);
        try (Connection connection = DBUtil.getConnection()) {
            return loadRatingPage(connection, memberId, score, Math.max(page, 1), safeSize);
        } catch (SQLException exception) {
            throw new RuntimeException("후기를 남긴 책을 불러오지 못했습니다.", exception);
        }
    }

    private Map<String, Object> loadRatingPage(Connection connection, long memberId, Integer score,
                                                int page, int size) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM RATING R JOIN BOOK B ON B.book_id=R.book_id WHERE R.member_id=? AND B.status='APPROVED' AND R.comment_text IS NOT NULL AND TRIM(R.comment_text) IS NOT NULL AND (? IS NULL OR R.score=?)";
        int totalCount;
        try (PreparedStatement statement = connection.prepareStatement(countSql)) {
            statement.setLong(1, memberId);
            if (score == null) { statement.setNull(2, Types.NUMERIC); statement.setNull(3, Types.NUMERIC); }
            else { statement.setInt(2, score); statement.setInt(3, score); }
            try (ResultSet rs = statement.executeQuery()) { rs.next(); totalCount = rs.getInt(1); }
        }
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) size));
        int safePage = Math.min(page, totalPages);
        String sql = """
                SELECT R.book_id,R.score,R.comment_text,B.title,B.image_url,A.author_name
                  FROM RATING R
                  JOIN BOOK B ON B.book_id=R.book_id
                  JOIN AUTHOR A ON A.author_id=B.author_id
                 WHERE R.member_id=? AND B.status='APPROVED'
                   AND R.comment_text IS NOT NULL AND TRIM(R.comment_text) IS NOT NULL
                   AND (? IS NULL OR R.score=?)
                 ORDER BY R.score DESC,B.title ASC,B.book_id ASC
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        List<Map<String, Object>> books = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            if (score == null) { statement.setNull(2, Types.NUMERIC); statement.setNull(3, Types.NUMERIC); }
            else { statement.setInt(2, score); statement.setInt(3, score); }
            statement.setInt(4, (safePage - 1) * size);
            statement.setInt(5, size);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, Object> book = new LinkedHashMap<>();
                    book.put("bookId", resultSet.getLong("book_id"));
                    book.put("score", resultSet.getInt("score"));
                    book.put("comment", resultSet.getString("comment_text"));
                    book.put("title", resultSet.getString("title"));
                    book.put("imageUrl", BookImageUrlUtil.thumbnail(resultSet.getString("image_url")));
                    book.put("authorName", resultSet.getString("author_name"));
                    books.add(book);
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("books", books);
        result.put("page", safePage);
        result.put("pageSize", size);
        result.put("totalCount", totalCount);
        result.put("totalPages", totalPages);
        return result;
    }

    private List<Map<String, Object>> loadTierLists(
            Connection connection, long memberId)
            throws SQLException {
        String sql = """
                SELECT L.tier_list_id,L.template_id,L.title,T.title template_title,L.created_at
                  FROM TIER_LIST L
                  JOIN TIER_TEMPLATE T ON T.template_id=L.template_id
                 WHERE L.member_id=?
                 ORDER BY L.created_at DESC,L.tier_list_id DESC
                """;
        List<Map<String, Object>> lists = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, Object> list = new LinkedHashMap<>();
                    list.put("tierListId", resultSet.getLong("tier_list_id"));
                    list.put("templateId", resultSet.getLong("template_id"));
                    list.put("title", resultSet.getString("title"));
                    list.put("templateTitle", resultSet.getString("template_title"));
                    lists.add(list);
                }
            }
        }
        return lists;
    }

    private List<Map<String, Object>> loadWorldcupResults(Connection connection, long memberId)
            throws SQLException {
        String sql = """
                SELECT R.run_id,T.title template_title,B.book_id,B.title winner_title,B.image_url
                  FROM IDEAL_RUN R
                  JOIN IDEAL_TEMPLATE T ON T.template_id=R.template_id
                  JOIN BOOK B ON B.book_id=R.winner_book_id
                 WHERE R.member_id=?
                 ORDER BY R.created_at DESC,R.run_id DESC
                """;
        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("runId", resultSet.getLong("run_id"));
                    result.put("templateTitle", resultSet.getString("template_title"));
                    result.put("winnerBookId", resultSet.getLong("book_id"));
                    result.put("winnerTitle", resultSet.getString("winner_title"));
                    result.put("winnerImageUrl", BookImageUrlUtil.thumbnail(resultSet.getString("image_url")));
                    results.add(result);
                }
            }
        }
        return results;
    }
}
