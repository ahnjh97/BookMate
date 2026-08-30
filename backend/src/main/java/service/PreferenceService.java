package service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import util.DBUtil;

public class PreferenceService {
    private static final double RATING_WEIGHT = 0.50;
    private static final double TIER_WEIGHT = 0.25;
    private static final double IDEAL_WEIGHT = 0.25;

    public Map<String, Object> findMemberSimilarity(long memberId, long targetMemberId) {
        if (memberId <= 0 || targetMemberId <= 0 || memberId == targetMemberId) {
            throw new IllegalArgumentException("비교할 회원 정보가 올바르지 않습니다.");
        }

        try (Connection connection = DBUtil.getConnection()) {
            String nickname = loadCandidateNickname(connection, memberId, targetMemberId);
            if (nickname == null) return null;
            Map<Long, Map<String, Double>> ratings = loadPairRatings(connection, memberId, targetMemberId);
            Map<Long, Map<String, Double>> tiers = loadPairTiers(connection, memberId, targetMemberId);
            Map<Long, Map<String, WinStat>> ideals = loadPairIdeals(connection, memberId, targetMemberId);
            Similarity similarity = new Similarity(
                    targetMemberId,
                    nickname,
                    ratingSimilarity(
                            ratings.getOrDefault(memberId, Map.of()),
                            ratings.getOrDefault(targetMemberId, Map.of())),
                    distanceSimilarity(
                            tiers.getOrDefault(memberId, Map.of()),
                            tiers.getOrDefault(targetMemberId, Map.of()),
                            3),
                    idealSimilarity(
                            ideals.getOrDefault(memberId, Map.of()),
                            ideals.getOrDefault(targetMemberId, Map.of())));
            return similarity.activeWeight() == 0 ? null : toMap(similarity);
        } catch (SQLException exception) {
            throw new RuntimeException("회원 취향 일치율을 계산하지 못했습니다.", exception);
        }
    }

    public List<Map<String, Object>> findSimilarMembers(long memberId, int limit) {
        if (memberId <= 0) throw new IllegalArgumentException("로그인 정보가 올바르지 않습니다.");
        int safeLimit = Math.max(1, Math.min(limit, 10));

        try (Connection connection = DBUtil.getConnection()) {
            Map<Long, String> candidates = loadCandidates(connection, memberId);
            if (candidates.isEmpty()) return List.of();

            Map<Long, Map<String, Double>> ratings = loadRatings(connection, memberId);
            Map<Long, Map<String, Double>> tiers = loadTiers(connection, memberId);
            Map<Long, Map<String, WinStat>> ideals = loadIdeals(connection, memberId);
            Map<String, Double> myRatings = ratings.getOrDefault(memberId, Map.of());
            Map<String, Double> myTiers = tiers.getOrDefault(memberId, Map.of());
            Map<String, WinStat> myIdeals = ideals.getOrDefault(memberId, Map.of());

            List<Similarity> similarities = new ArrayList<>();
            for (Map.Entry<Long, String> candidate : candidates.entrySet()) {
                long candidateId = candidate.getKey();
                Component rating = ratingSimilarity(myRatings, ratings.getOrDefault(candidateId, Map.of()));
                Component tier = distanceSimilarity(myTiers, tiers.getOrDefault(candidateId, Map.of()), 3);
                Component ideal = idealSimilarity(myIdeals, ideals.getOrDefault(candidateId, Map.of()));
                similarities.add(new Similarity(candidateId, candidate.getValue(), rating, tier, ideal));
            }

            return similarities.stream()
                    .filter(value -> value.activeWeight() > 0)
                    .sorted(Comparator.comparingDouble(Similarity::score).reversed()
                            .thenComparing(Comparator.comparingInt(Similarity::evidenceCount).reversed())
                            .thenComparing(Similarity::nickname))
                    .limit(safeLimit)
                    .map(this::toMap)
                    .toList();
        } catch (SQLException exception) {
            throw new RuntimeException("비슷한 취향의 사용자를 찾지 못했습니다.", exception);
        }
    }

    private Map<Long, String> loadCandidates(Connection connection, long memberId) throws SQLException {
        String sql = "SELECT member_id,nickname FROM MEMBER WHERE member_id<>? AND role='USER'"
                + " AND login_id<>'bookmate_system'";
        Map<Long, String> candidates = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) candidates.put(resultSet.getLong(1), resultSet.getString(2));
            }
        }
        return candidates;
    }

    private String loadCandidateNickname(Connection connection, long memberId, long targetMemberId)
            throws SQLException {
        String sql = "SELECT nickname FROM MEMBER WHERE member_id=? AND member_id<>? AND role='USER'"
                + " AND login_id<>'bookmate_system'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, targetMemberId);
            statement.setLong(2, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("nickname") : null;
            }
        }
    }

    private Map<Long, Map<String, Double>> loadPairRatings(
            Connection connection, long memberId, long targetMemberId) throws SQLException {
        String sql = """
                SELECT member_id,book_id,score FROM RATING
                 WHERE member_id IN (?,?)
                """;
        Map<Long, Map<String, Double>> values = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            statement.setLong(2, targetMemberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.computeIfAbsent(resultSet.getLong(1), ignored -> new HashMap<>())
                            .put(Long.toString(resultSet.getLong(2)), resultSet.getDouble(3));
                }
            }
        }
        return values;
    }

    private Map<Long, Map<String, Double>> loadPairTiers(
            Connection connection, long memberId, long targetMemberId) throws SQLException {
        String sql = """
                SELECT L.member_id,L.template_id,I.book_id,I.tier_grade
                  FROM TIER_LIST L JOIN TIER_ITEM I ON I.tier_list_id=L.tier_list_id
                 WHERE L.member_id IN (?,?)
                """;
        Map<Long, Map<String, Double>> values = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            statement.setLong(2, targetMemberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String key = resultSet.getLong(2) + ":" + resultSet.getLong(3);
                    values.computeIfAbsent(resultSet.getLong(1), ignored -> new HashMap<>())
                            .put(key, tierValue(resultSet.getString(4)));
                }
            }
        }
        return values;
    }

    private Map<Long, Map<String, WinStat>> loadPairIdeals(
            Connection connection, long memberId, long targetMemberId) throws SQLException {
        String sql = """
                SELECT R.member_id,R.template_id,M.left_book_id,M.right_book_id,M.winner_book_id
                  FROM IDEAL_RUN R JOIN IDEAL_MATCH M ON M.run_id=R.run_id
                 WHERE R.member_id IN (?,?)
                """;
        Map<Long, Map<String, WinStat>> values = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            statement.setLong(2, targetMemberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long userId = resultSet.getLong(1);
                    long templateId = resultSet.getLong(2);
                    long left = resultSet.getLong(3);
                    long right = resultSet.getLong(4);
                    long winner = resultSet.getLong(5);
                    Map<String, WinStat> user = values.computeIfAbsent(userId, ignored -> new HashMap<>());
                    recordMatch(user, templateId, left, left == winner);
                    recordMatch(user, templateId, right, right == winner);
                }
            }
        }
        return values;
    }

    private Map<Long, Map<String, Double>> loadRatings(Connection connection, long memberId)
            throws SQLException {
        String sql = """
                WITH MY_BOOK AS (SELECT book_id FROM RATING WHERE member_id=? )
                SELECT R.member_id,R.book_id,R.score
                    FROM RATING R JOIN MY_BOOK B ON B.book_id=R.book_id
                """;
        Map<Long, Map<String, Double>> values = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.computeIfAbsent(resultSet.getLong(1), ignored -> new HashMap<>())
                            .put(Long.toString(resultSet.getLong(2)), resultSet.getDouble(3));
                }
            }
        }
        return values;
    }

    private Map<Long, Map<String, Double>> loadTiers(Connection connection, long memberId)
            throws SQLException {
        String sql = """
                WITH MY_TEMPLATE AS (SELECT template_id FROM TIER_LIST WHERE member_id=? )
                SELECT L.member_id,L.template_id,I.book_id,I.tier_grade
                    FROM TIER_LIST L
                    JOIN MY_TEMPLATE T ON T.template_id=L.template_id
                    JOIN TIER_ITEM I ON I.tier_list_id=L.tier_list_id
                """;
        Map<Long, Map<String, Double>> values = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String key = resultSet.getLong(2) + ":" + resultSet.getLong(3);
                    values.computeIfAbsent(resultSet.getLong(1), ignored -> new HashMap<>())
                            .put(key, tierValue(resultSet.getString(4)));
                }
            }
        }
        return values;
    }

    private Map<Long, Map<String, WinStat>> loadIdeals(Connection connection, long memberId)
            throws SQLException {
        String sql = """
                WITH MY_TEMPLATE AS (SELECT template_id FROM IDEAL_RUN WHERE member_id=? )
                SELECT R.member_id,R.template_id,M.left_book_id,M.right_book_id,M.winner_book_id
                    FROM IDEAL_RUN R
                    JOIN MY_TEMPLATE T ON T.template_id=R.template_id
                    JOIN IDEAL_MATCH M ON M.run_id=R.run_id
                """;
        Map<Long, Map<String, WinStat>> values = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long userId = resultSet.getLong(1);
                    long templateId = resultSet.getLong(2);
                    long left = resultSet.getLong(3);
                    long right = resultSet.getLong(4);
                    long winner = resultSet.getLong(5);
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

    private Component ratingSimilarity(Map<String, Double> mine, Map<String, Double> other) {
        List<String> common = mine.keySet().stream().filter(other::containsKey).toList();
        if (common.size() < 5) return Component.empty();
        double mineAverage = common.stream().mapToDouble(mine::get).average().orElse(0);
        double otherAverage = common.stream().mapToDouble(other::get).average().orElse(0);
        double numerator = 0;
        double mineSquare = 0;
        double otherSquare = 0;
        for (String key : common) {
            double mineCentered = mine.get(key) - mineAverage;
            double otherCentered = other.get(key) - otherAverage;
            numerator += mineCentered * otherCentered;
            mineSquare += mineCentered * mineCentered;
            otherSquare += otherCentered * otherCentered;
        }
        double correlation;
        if (mineSquare == 0 || otherSquare == 0) {
            correlation = 1 - common.stream()
                    .mapToDouble(key -> Math.abs(mine.get(key) - other.get(key)) / 4.0)
                    .average().orElse(1);
            return new Component(clamp(correlation * 100), common.size());
        }
        correlation = numerator / Math.sqrt(mineSquare * otherSquare);
        return new Component(clamp((correlation + 1) * 50), common.size());
    }

    private Component distanceSimilarity(
            Map<String, Double> mine, Map<String, Double> other, int minimumCommon) {
        List<String> common = mine.keySet().stream().filter(other::containsKey).toList();
        if (common.size() < minimumCommon) return Component.empty();
        double score = common.stream()
                .mapToDouble(key -> 1 - Math.abs(mine.get(key) - other.get(key)))
                .average().orElse(0) * 100;
        return new Component(clamp(score), common.size());
    }

    private Component idealSimilarity(Map<String, WinStat> mine, Map<String, WinStat> other) {
        Map<String, Double> mineRates = new HashMap<>();
        Map<String, Double> otherRates = new HashMap<>();
        mine.forEach((key, value) -> mineRates.put(key, value.rate()));
        other.forEach((key, value) -> otherRates.put(key, value.rate()));
        return distanceSimilarity(mineRates, otherRates, 4);
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

    private Map<String, Object> toMap(Similarity value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("memberId", value.memberId());
        result.put("nickname", value.nickname());
        result.put("similarityScore", round(value.score()));
        result.put("confidence", value.confidence());
        result.put("ratingSimilarity", value.rating().available() ? round(value.rating().score()) : null);
        result.put("tierSimilarity", value.tier().available() ? round(value.tier().score()) : null);
        result.put("worldcupSimilarity", value.ideal().available() ? round(value.ideal().score()) : null);
        result.put("commonRatingCount", value.rating().commonCount());
        result.put("commonTierBookCount", value.tier().commonCount());
        result.put("commonWorldcupBookCount", value.ideal().commonCount());
        return result;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private record Component(double score, int commonCount) {
        static Component empty() {
            return new Component(0, 0);
        }

        boolean available() {
            return commonCount > 0;
        }
    }

    private record Similarity(
            long memberId, String nickname, Component rating, Component tier, Component ideal) {
        double activeWeight() {
            return (rating.available() ? RATING_WEIGHT : 0)
                    + (tier.available() ? TIER_WEIGHT : 0)
                    + (ideal.available() ? IDEAL_WEIGHT : 0);
        }

        double score() {
            double total = (rating.available() ? rating.score() * RATING_WEIGHT : 0)
                    + (tier.available() ? tier.score() * TIER_WEIGHT : 0)
                    + (ideal.available() ? ideal.score() * IDEAL_WEIGHT : 0);
            return activeWeight() == 0 ? 0 : total / activeWeight();
        }

        int evidenceCount() {
            return rating.commonCount() + tier.commonCount() + ideal.commonCount();
        }

        String confidence() {
            int evidence = evidenceCount();
            if (evidence >= 100 && tier.available() && ideal.available()) return "HIGH";
            if (evidence >= 30) return "MEDIUM";
            return "LOW";
        }
    }

    private static class WinStat {
        private int wins;
        private int matches;

        void record(boolean won) {
            matches++;
            if (won) wins++;
        }

        double rate() {
            return matches == 0 ? 0 : (double) wins / matches;
        }
    }
}
