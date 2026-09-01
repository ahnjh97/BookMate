package service;

import dao.PreferenceDAO;
import dao.PreferenceDAO.WinStat;
import dto.preference.PreferenceSimilarityDTO;
import java.sql.Connection;
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
    private final PreferenceDAO preferenceDAO = new PreferenceDAO();

    public PreferenceSimilarityDTO findMemberSimilarity(long memberId, long targetMemberId) {
        if (memberId <= 0 || targetMemberId <= 0 || memberId == targetMemberId) {
            throw new IllegalArgumentException("비교할 회원 정보가 올바르지 않습니다.");
        }

        try (Connection connection = DBUtil.getConnection()) {
            String nickname = preferenceDAO.selectCandidateNickname(connection, memberId, targetMemberId);
            if (nickname == null) return null;
            Map<Long, Map<String, Double>> ratings = preferenceDAO.selectPairRatings(connection, memberId, targetMemberId);
            Map<Long, Map<String, Double>> tiers = preferenceDAO.selectPairTiers(connection, memberId, targetMemberId);
            Map<Long, Map<String, WinStat>> ideals = preferenceDAO.selectPairIdeals(connection, memberId, targetMemberId);
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

    public List<PreferenceSimilarityDTO> findSimilarMembers(long memberId, int limit) {
        if (memberId <= 0) throw new IllegalArgumentException("로그인 정보가 올바르지 않습니다.");
        int safeLimit = Math.max(1, Math.min(limit, 10));

        try (Connection connection = DBUtil.getConnection()) {
            Map<Long, String> candidates = preferenceDAO.selectCandidates(connection, memberId);
            if (candidates.isEmpty()) return List.of();

            Map<Long, Map<String, Double>> ratings = preferenceDAO.selectRatings(connection, memberId);
            Map<Long, Map<String, Double>> tiers = preferenceDAO.selectTiers(connection, memberId);
            Map<Long, Map<String, WinStat>> ideals = preferenceDAO.selectIdeals(connection, memberId);
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

    private PreferenceSimilarityDTO toMap(Similarity value) {
        return PreferenceSimilarityDTO.builder()
                .memberId(value.memberId()).nickname(value.nickname())
                .similarityScore(round(value.score())).confidence(value.confidence())
                .ratingSimilarity(value.rating().available() ? round(value.rating().score()) : null)
                .tierSimilarity(value.tier().available() ? round(value.tier().score()) : null)
                .worldcupSimilarity(value.ideal().available() ? round(value.ideal().score()) : null)
                .commonRatingCount(value.rating().commonCount())
                .commonTierBookCount(value.tier().commonCount())
                .commonWorldcupBookCount(value.ideal().commonCount()).build();
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


}
