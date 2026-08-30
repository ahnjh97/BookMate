package dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
public class PostLikeDTO {
    private long postLikeId;
    private long postId;
    private long memberId;
    private Timestamp createdAt;
}