package dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
public class PostCommentDTO {

    /* 댓글 데이터를 담을 저장소 */
    private long commentId;
    private long postId;
    private long memberId;
    private Long parentCommentId;
    private String memberNickname;
    private String content;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}