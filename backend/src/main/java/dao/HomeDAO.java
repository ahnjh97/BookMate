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
                       (SELECT COUNT(*) FROM TIER_TEMPLATE WHERE status = 'APPROVED') AS tier_template_count,
                       (SELECT COUNT(*) FROM TIER_LIST) AS tier_participation_count,
                       (SELECT COUNT(*) FROM IDEAL_TEMPLATE WHERE status = 'APPROVED') AS worldcup_template_count,
                       (SELECT COUNT(*) FROM IDEAL_RUN) AS worldcup_participation_count
                  FROM DUAL
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return new HomeSummaryDTO(0, 0, 0, 0, 0, 0);
            }
            return new HomeSummaryDTO(
                    resultSet.getLong("book_count"),
                    resultSet.getLong("rating_count"),
                    resultSet.getLong("tier_template_count"),
                    resultSet.getLong("tier_participation_count"),
                    resultSet.getLong("worldcup_template_count"),
                    resultSet.getLong("worldcup_participation_count")
            );
        }
    }
}
