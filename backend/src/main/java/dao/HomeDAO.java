package dao;

import dto.HomeSummaryDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HomeDAO {
    public HomeSummaryDTO selectSummary(Connection connection) throws SQLException {
        String sql = """
                SELECT (SELECT COUNT(*) FROM BOOK WHERE status = 'APPROVED') AS book_count,
                       (SELECT COUNT(*) FROM RATING) AS rating_count,
                       (SELECT COUNT(*) FROM TIER_LIST WHERE is_public = 'Y') AS tier_list_count
                  FROM DUAL
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return new HomeSummaryDTO(0, 0, 0);
            }
            return new HomeSummaryDTO(
                    resultSet.getLong("book_count"),
                    resultSet.getLong("rating_count"),
                    resultSet.getLong("tier_list_count")
            );
        }
    }
}
