package dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
public class PostDTO {
    /* 게시글 데이터를 담을 저장소 */
    private long postId;
    private long memberId;
    private Long tierListId;
    private Long idealRunId;
    private String memberNickname;
    private String category;
    private String title;
    private String content;
    private String genre;
    private int viewCount;
    private int likeCount;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String isPinned;

    public PostDTO(
            long postId,
            long memberId,
            String memberNickname,
            String category,
            String title,
            String content,
            String genre,
            int viewCount,
            String isPinned,
            String status,
            Timestamp createdAt,
            Timestamp updatedAt
    ) {
        this.postId = postId;
        this.memberId = memberId;
        this.memberNickname = memberNickname;
        this.category = category;
        this.title = title;
        this.content = content;
        this.genre = genre;
        this.viewCount = viewCount;
        this.isPinned = isPinned;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
