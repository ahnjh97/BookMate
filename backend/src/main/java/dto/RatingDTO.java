package dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatingDTO {
    private long ratingId;
    private long bookId;
    private long memberId;
    private String nickname;
    private int score;
    private String commentText;
}
