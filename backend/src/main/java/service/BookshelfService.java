package service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import util.DBUtil;

public class BookshelfService {
    public Map<String, Object> findBookshelf(long memberId) {
        if (memberId <= 0) throw new IllegalArgumentException("회원 번호가 올바르지 않습니다.");

        try (Connection connection = DBUtil.getConnection()) {
            Map<String, Object> bookshelf = loadMember(connection, memberId);
            if (bookshelf == null) return null;
            bookshelf.put("counts", loadCounts(connection, memberId));
            bookshelf.put("favoriteBooks", loadFavoriteBooks(connection, memberId));
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

    private List<Map<String, Object>> loadFavoriteBooks(Connection connection, long memberId)
            throws SQLException {
        String sql = """
                SELECT R.book_id,R.score,R.comment_text,B.title,B.image_url,A.author_name
                  FROM RATING R
                  JOIN BOOK B ON B.book_id=R.book_id
                  JOIN AUTHOR A ON A.author_id=B.author_id
                 WHERE R.member_id=? AND B.status='APPROVED'
                   AND R.comment_text IS NOT NULL AND TRIM(R.comment_text) IS NOT NULL
                 ORDER BY R.rating_id DESC
                """;
        List<Map<String, Object>> books = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, Object> book = new LinkedHashMap<>();
                    book.put("bookId", resultSet.getLong("book_id"));
                    book.put("score", resultSet.getInt("score"));
                    book.put("comment", resultSet.getString("comment_text"));
                    book.put("title", resultSet.getString("title"));
                    book.put("imageUrl", resultSet.getString("image_url"));
                    book.put("authorName", resultSet.getString("author_name"));
                    books.add(book);
                }
            }
        }
        return books;
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
                    result.put("winnerImageUrl", resultSet.getString("image_url"));
                    results.add(result);
                }
            }
        }
        return results;
    }
}
