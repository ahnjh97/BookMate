package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PreferenceDAO {
    public Map<Long, String> selectCandidates(Connection connection, long memberId) throws SQLException {
        String sql = "SELECT member_id,nickname FROM MEMBER WHERE member_id<>? AND role='USER' AND login_id<>'bookmate_system'";
        Map<Long, String> candidates = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) candidates.put(rs.getLong(1), rs.getString(2));
            }
        }
        return candidates;
    }

    public String selectCandidateNickname(Connection connection, long memberId, long targetMemberId)
            throws SQLException {
        String sql = "SELECT nickname FROM MEMBER WHERE member_id=? AND member_id<>? AND role='USER' AND login_id<>'bookmate_system'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, targetMemberId);
            statement.setLong(2, memberId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public Map<Long, Map<String, Double>> selectPairRatings(
            Connection connection, long memberId, long targetMemberId) throws SQLException {
        return selectRatings(connection,
                "SELECT member_id,book_id,score FROM RATING WHERE member_id IN (?,?)",
                memberId, targetMemberId);
    }

    public Map<Long, Map<String, Double>> selectPairTiers(
            Connection connection, long memberId, long targetMemberId) throws SQLException {
        String sql = "SELECT L.member_id,L.template_id,I.book_id,I.tier_grade FROM TIER_LIST L JOIN TIER_ITEM I ON I.tier_list_id=L.tier_list_id WHERE L.member_id IN (?,?)";
        return selectTiers(connection, sql, memberId, targetMemberId);
    }

    public Map<Long, Map<String, WinStat>> selectPairIdeals(
            Connection connection, long memberId, long targetMemberId) throws SQLException {
        String sql = "SELECT R.member_id,R.template_id,M.left_book_id,M.right_book_id,M.winner_book_id FROM IDEAL_RUN R JOIN IDEAL_MATCH M ON M.run_id=R.run_id WHERE R.member_id IN (?,?)";
        return selectIdeals(connection, sql, memberId, targetMemberId);
    }

    public Map<Long, Map<String, Double>> selectRatings(Connection connection, long memberId)
            throws SQLException {
        String sql = "WITH MY_BOOK AS (SELECT book_id FROM RATING WHERE member_id=?) SELECT R.member_id,R.book_id,R.score FROM RATING R JOIN MY_BOOK B ON B.book_id=R.book_id";
        return selectRatings(connection, sql, memberId);
    }

    public Map<Long, Map<String, Double>> selectTiers(Connection connection, long memberId)
            throws SQLException {
        String sql = "WITH MY_TEMPLATE AS (SELECT template_id FROM TIER_LIST WHERE member_id=?) SELECT L.member_id,L.template_id,I.book_id,I.tier_grade FROM TIER_LIST L JOIN MY_TEMPLATE T ON T.template_id=L.template_id JOIN TIER_ITEM I ON I.tier_list_id=L.tier_list_id";
        return selectTiers(connection, sql, memberId);
    }

    public Map<Long, Map<String, WinStat>> selectIdeals(Connection connection, long memberId)
            throws SQLException {
        String sql = "WITH MY_TEMPLATE AS (SELECT template_id FROM IDEAL_RUN WHERE member_id=?) SELECT R.member_id,R.template_id,M.left_book_id,M.right_book_id,M.winner_book_id FROM IDEAL_RUN R JOIN MY_TEMPLATE T ON T.template_id=R.template_id JOIN IDEAL_MATCH M ON M.run_id=R.run_id";
        return selectIdeals(connection, sql, memberId);
    }

    private Map<Long, Map<String, Double>> selectRatings(
            Connection connection, String sql, long... memberIds) throws SQLException {
        Map<Long, Map<String, Double>> values = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < memberIds.length; i++) statement.setLong(i + 1, memberIds[i]);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) values.computeIfAbsent(rs.getLong(1), ignored -> new HashMap<>())
                        .put(Long.toString(rs.getLong(2)), rs.getDouble(3));
            }
        }
        return values;
    }

    private Map<Long, Map<String, Double>> selectTiers(
            Connection connection, String sql, long... memberIds) throws SQLException {
        Map<Long, Map<String, Double>> values = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < memberIds.length; i++) statement.setLong(i + 1, memberIds[i]);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getLong(2) + ":" + rs.getLong(3);
                    values.computeIfAbsent(rs.getLong(1), ignored -> new HashMap<>())
                            .put(key, tierValue(rs.getString(4)));
                }
            }
        }
        return values;
    }

    private Map<Long, Map<String, WinStat>> selectIdeals(
            Connection connection, String sql, long... memberIds) throws SQLException {
        Map<Long, Map<String, WinStat>> values = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < memberIds.length; i++) statement.setLong(i + 1, memberIds[i]);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long userId = rs.getLong(1);
                    long templateId = rs.getLong(2);
                    long left = rs.getLong(3);
                    long right = rs.getLong(4);
                    long winner = rs.getLong(5);
                    Map<String, WinStat> user = values.computeIfAbsent(userId, ignored -> new HashMap<>());
                    recordMatch(user, templateId, left, left == winner);
                    recordMatch(user, templateId, right, right == winner);
                }
            }
        }
        return values;
    }

    private void recordMatch(Map<String, WinStat> values, long templateId, long bookId, boolean won) {
        values.computeIfAbsent(templateId + ":" + bookId, ignored -> new WinStat()).record(won);
    }

    private double tierValue(String grade) {
        return switch (grade) {
            case "S" -> 1.0;
            case "A" -> 0.75;
            case "B" -> 0.50;
            case "C" -> 0.25;
            default -> 0.0;
        };
    }

    public static final class WinStat {
        private int wins;
        private int matches;

        private void record(boolean won) {
            matches++;
            if (won) wins++;
        }

        public double rate() {
            return matches == 0 ? 0 : (double) wins / matches;
        }
    }
}
