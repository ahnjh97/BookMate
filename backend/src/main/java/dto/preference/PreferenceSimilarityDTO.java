package dto.preference;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PreferenceSimilarityDTO {
    private long memberId;
    private String nickname;
    private double similarityScore;
    private String confidence;
    private Double ratingSimilarity;
    private Double tierSimilarity;
    private Double worldcupSimilarity;
    private int commonRatingCount;
    private int commonTierBookCount;
    private int commonWorldcupBookCount;
}
