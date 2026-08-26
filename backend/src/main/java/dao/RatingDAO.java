package dao;

import dto.RatingDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RatingDAO {

    public boolean existsApprovedBook(Connection connection, long bookId) throws SQLException {
        String sql = """
                SELECT 1
                  FROM BOOK
                 WHERE book_id = ?
                   AND status = 'APPROVED'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean existsRating(Connection connection, long bookId, long memberId) throws SQLException {
        String sql = """
                SELECT 1
                  FROM RATING
                 WHERE book_id = ?
                   AND member_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            statement.setLong(2, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public RatingDTO selectRatingByBookAndMember(
            Connection connection,
            long bookId,
            long memberId
    ) throws SQLException {
        String sql = """
                SELECT rating_id, book_id, member_id, score, comment_text
                  FROM RATING
                 WHERE book_id = ?
                   AND member_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, bookId);
            statement.setLong(2, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRating(resultSet) : null;
            }
        }
    }

    public long insertRating(Connection connection, RatingDTO rating) throws SQLException {
        String sql = """
                INSERT INTO RATING (
                    rating_id,
                    book_id,
                    member_id,
                    score,
                    comment_text
                ) VALUES (
                    SEQ_RATING.NEXTVAL,
                    ?,
                    ?,
                    ?,
                    ?
                )
                """;
        String[] generatedColumns = {"RATING_ID"};

        try (PreparedStatement statement = connection.prepareStatement(sql, generatedColumns)) {
            statement.setLong(1, rating.getBookId());
            statement.setLong(2, rating.getMemberId());
            statement.setInt(3, rating.getScore());
            statement.setString(4, rating.getCommentText());

            if (statement.executeUpdate() == 0) {
                throw new SQLException("평점 등록에 실패했습니다.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }

        throw new SQLException("생성된 평점 번호를 가져오지 못했습니다.");
    }

    public int updateRating(Connection connection, RatingDTO rating) throws SQLException {
        String sql = """
                UPDATE RATING
                   SET score = ?,
                       comment_text = ?
                 WHERE rating_id = ?
                   AND book_id = ?
                   AND member_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, rating.getScore());
            statement.setString(2, rating.getCommentText());
            statement.setLong(3, rating.getRatingId());
            statement.setLong(4, rating.getBookId());
            statement.setLong(5, rating.getMemberId());
            return statement.executeUpdate();
        }
    }

    private RatingDTO mapRating(ResultSet resultSet) throws SQLException {
        RatingDTO rating = new RatingDTO();
        rating.setRatingId(resultSet.getLong("rating_id"));
        rating.setBookId(resultSet.getLong("book_id"));
        rating.setMemberId(resultSet.getLong("member_id"));
        rating.setScore(resultSet.getInt("score"));
        rating.setCommentText(resultSet.getString("comment_text"));
        return rating;
    }
}
