package dto;

import java.sql.Timestamp;

public class PostDTO {
    //게시글 데이터를 담을 저장소
    private long postId;
    private long memberId;
    private String memberNickname;
    private String category;
    private String title;
    private String content;
    private int viewCount;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public PostDTO() {
    }

    public PostDTO(
            long postId,
            long memberId,
            String memberNickname,
            String category,
            String title,
            String content,
            int viewCount,
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
        this.viewCount = viewCount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public String getMemberNickname() {
        return memberNickname;
    }

    public void setMemberNickname(String memberNickname) {
        this.memberNickname = memberNickname;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}