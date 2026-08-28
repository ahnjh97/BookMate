package dao;

import dto.BookDTO;
import dto.SearchSuggestionDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {
    public List<SearchSuggestionDTO> selectSearchSuggestions(Connection connection, String keyword)
            throws SQLException {
        String sql = """
                SELECT result_type, result_id, result_name, result_detail, image_url
                  FROM (
                        SELECT 'BOOK' AS result_type,
                               B.book_id AS result_id,
                               B.title AS result_name,
                               A.author_name AS result_detail,
                               B.image_url,
                               1 AS type_order,
                               CASE
                                   WHEN LOWER(B.title) = ? THEN 1
                                   WHEN LOWER(B.title) LIKE ? THEN 2
                                   ELSE 3
                               END AS match_priority,
                               INSTR(LOWER(B.title), ?) AS match_position
                          FROM BOOK B
                          JOIN AUTHOR A ON A.author_id = B.author_id
                         WHERE B.status = 'APPROVED'
                           AND LOWER(B.title) LIKE ?
                        UNION ALL
                        SELECT 'AUTHOR' AS result_type,
                               A.author_id AS result_id,
                               A.author_name AS result_name,
                               '작가' AS result_detail,
                               NULL AS image_url,
                               2 AS type_order,
                               CASE
                                   WHEN LOWER(A.author_name) = ? THEN 1
                                   WHEN LOWER(A.author_name) LIKE ? THEN 2
                                   ELSE 3
                               END AS match_priority,
                               INSTR(LOWER(A.author_name), ?) AS match_position
                          FROM AUTHOR A
                         WHERE LOWER(A.author_name) LIKE ?
                           AND EXISTS (
                               SELECT 1
                                 FROM BOOK B
                                WHERE B.author_id = A.author_id
                                  AND B.status = 'APPROVED'
                           )
                       )
                 ORDER BY match_priority, match_position, type_order, result_name
                 FETCH FIRST 10 ROWS ONLY
                """;

        String normalizedKeyword = keyword.trim().toLowerCase();
        String prefixPattern = normalizedKeyword + "%";
        String containsPattern = "%" + normalizedKeyword + "%";
        List<SearchSuggestionDTO> suggestions = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedKeyword);
            statement.setString(2, prefixPattern);
            statement.setString(3, normalizedKeyword);
            statement.setString(4, containsPattern);
            statement.setString(5, normalizedKeyword);
            statement.setString(6, prefixPattern);
            statement.setString(7, normalizedKeyword);
            statement.setString(8, containsPattern);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    suggestions.add(new SearchSuggestionDTO(
                            resultSet.getString("result_type"),
                            resultSet.getLong("result_id"),
                            resultSet.getString("result_name"),
                            resultSet.getString("result_detail"),
                            normalizeImageUrl(resultSet.getString("image_url"))
                    ));
                }
            }
        }
        return suggestions;
    }

    public List<BookDTO> selectBooks(
            Connection connection,
            String keyword,
            String genre,
            Long authorId,
            String sort,
            int offset,
            int limit
    )
            throws SQLException {
        String orderBy = switch (sort) {
            case "title" -> "B.title ASC, B.book_id DESC";
            case "newest" -> "B.published_date DESC NULLS LAST, B.book_id DESC";
            default -> "NVL(ROUND(AVG(R.score), 2), 0) DESC, COUNT(R.rating_id) DESC, B.title ASC";
        };
        String sql = """
                SELECT B.book_id, B.author_id, A.author_name, B.title, B.genre,
                       B.publisher, B.published_date, B.description, B.image_url, B.status,
                       NVL(ROUND(AVG(R.score), 2), 0) AS average_rating,
                       COUNT(R.rating_id) AS rating_count
                  FROM BOOK B
                  JOIN AUTHOR A ON A.author_id = B.author_id
                  LEFT JOIN RATING R ON R.book_id = B.book_id
                 WHERE B.status = 'APPROVED'
                   AND (? IS NULL OR LOWER(B.title) LIKE ? OR LOWER(A.author_name) LIKE ?)
                   AND (? IS NULL OR B.genre = ?)
                   AND (? IS NULL OR B.author_id = ?)
                 GROUP BY B.book_id, B.author_id, A.author_name, B.title, B.genre,
                          B.publisher, B.published_date, B.description, B.image_url, B.status
                 ORDER BY """ + " " + orderBy + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        String normalizedKeyword = normalize(keyword);
        String keywordPattern = normalizedKeyword == null ? null : "%" + normalizedKeyword.toLowerCase() + "%";
        String normalizedGenre = normalize(genre);
        List<BookDTO> books = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedKeyword);
            statement.setString(2, keywordPattern);
            statement.setString(3, keywordPattern);
            statement.setString(4, normalizedGenre);
            statement.setString(5, normalizedGenre);
            if (authorId == null) {
                statement.setNull(6, java.sql.Types.NUMERIC);
                statement.setNull(7, java.sql.Types.NUMERIC);
            } else {
                statement.setLong(6, authorId);
                statement.setLong(7, authorId);
            }
            statement.setInt(8, offset);
            statement.setInt(9, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    books.add(mapBook(resultSet));
                }
            }
        }
        return books;
    }

    public int countBooks(Connection connection, String keyword, String genre, Long authorId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                  FROM BOOK B
                  JOIN AUTHOR A ON A.author_id = B.author_id
                 WHERE B.status = 'APPROVED'
                   AND (? IS NULL OR LOWER(B.title) LIKE ? OR LOWER(A.author_name) LIKE ?)
                   AND (? IS NULL OR B.genre = ?)
                   AND (? IS NULL OR B.author_id = ?)
                """;
        String normalizedKeyword = normalize(keyword);
        String keywordPattern = normalizedKeyword == null ? null : "%" + normalizedKeyword.toLowerCase() + "%";
        String normalizedGenre = normalize(genre);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedKeyword);
            statement.setString(2, keywordPattern);
            statement.setString(3, keywordPattern);
            statement.setString(4, normalizedGenre);
            statement.setString(5, normalizedGenre);
            if (authorId == null) {
                statement.setNull(6, java.sql.Types.NUMERIC);
                statement.setNull(7, java.sql.Types.NUMERIC);
            } else {
                statement.setLong(6, authorId);
                statement.setLong(7, authorId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public BookDTO selectBookById(Connection connection, long bookId) throws SQLException {
        String sql = """
                SELECT B.book_id, B.author_id, A.author_name, B.title, B.genre,
                       B.publisher, B.published_date, B.description, B.image_url, B.status,
                       NVL(ROUND(AVG(R.score), 2), 0) AS average_rating,
                       COUNT(R.rating_id) AS rating_count
                  FROM BOOK B
                  JOIN AUTHOR A ON A.author_id = B.author_id
                  LEFT JOIN RATING R ON R.book_id = B.book_id
                 WHERE B.book_id = ? AND B.status = 'APPROVED'
                 GROUP BY B.book_id, B.author_id, A.author_name, B.title, B.genre,
                          B.publisher, B.published_date, B.description, B.image_url, B.status
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapBook(resultSet) : null;
            }
        }
    }

    public List<BookDTO> selectBookRankings(
            Connection connection,
            String genre,
            String sort,
            int minimumRatings,
            int limit
    ) throws SQLException {
        String averageExpression = "NVL(ROUND(AVG(R.score), 2), 0)";
        String countExpression = "COUNT(R.rating_id)";
        String orderBy = "count".equals(sort)
                ? countExpression + " DESC, " + averageExpression + " DESC, B.title ASC"
                : averageExpression + " DESC, " + countExpression + " DESC, B.title ASC";
        String sql = """
                SELECT B.book_id, B.author_id, A.author_name, B.title, B.genre,
                       B.publisher, B.published_date, B.description, B.image_url, B.status,
                       NVL(ROUND(AVG(R.score), 2), 0) AS average_rating,
                       COUNT(R.rating_id) AS rating_count
                  FROM BOOK B
                  JOIN AUTHOR A ON A.author_id = B.author_id
                  LEFT JOIN RATING R ON R.book_id = B.book_id
                 WHERE B.status = 'APPROVED'
                   AND B.genre = ?
                 GROUP BY B.book_id, B.author_id, A.author_name, B.title, B.genre,
                          B.publisher, B.published_date, B.description, B.image_url, B.status
                HAVING COUNT(R.rating_id) >= ?
                 ORDER BY """ + " " + orderBy + " OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";

        List<BookDTO> rankings = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, genre);
            statement.setInt(2, minimumRatings);
            statement.setInt(3, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    BookDTO book = mapBook(resultSet);
                    book.setAverageRating(resultSet.getDouble("average_rating"));
                    book.setRatingCount(resultSet.getInt("rating_count"));
                    rankings.add(book);
                }
            }
        }
        return rankings;
    }

    private BookDTO mapBook(ResultSet resultSet) throws SQLException {
        BookDTO book = new BookDTO();
        book.setBookId(resultSet.getLong("book_id"));
        book.setAuthorId(resultSet.getLong("author_id"));
        book.setAuthorName(resultSet.getString("author_name"));
        book.setTitle(resultSet.getString("title"));
        book.setGenre(resultSet.getString("genre"));
        book.setPublisher(resultSet.getString("publisher"));
        book.setPublishedDate(resultSet.getDate("published_date"));
        book.setDescription(resultSet.getString("description"));
        String imageUrl = normalizeImageUrl(resultSet.getString("image_url"));
        book.setImageUrl(imageUrl);
        book.setDetailImageUrl(toDetailImageUrl(imageUrl));
        book.setStatus(resultSet.getString("status"));
        book.setAverageRating(resultSet.getDouble("average_rating"));
        book.setRatingCount(resultSet.getInt("rating_count"));
        return book;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl != null && imageUrl.startsWith("/bookmate/")) {
            return imageUrl.substring("/bookmate".length());
        }
        return imageUrl;
    }

    private String toDetailImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }
        return imageUrl.replace("-240.webp", "-520.webp");
    }
}
