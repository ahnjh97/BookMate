package dao;

import dto.PostCommentDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostCommentDAO {

    /* 1. 게시글 댓글 목록 조회 */
    public List<PostCommentDTO> selectCommentList(Connection conn, long postId) throws SQLException {
        String sql = """
                SELECT
                    C.comment_id,
                    C.post_id,
                    C.member_id,
                    C.parent_comment_id,
                    M.nickname AS member_nickname,
                    C.content,
                    C.status,
                    C.created_at,
                    C.updated_at
                FROM POST_COMMENT C
                JOIN MEMBER M
                  ON C.member_id = M.member_id
                WHERE C.post_id = ?
                  AND C.status = 'ACTIVE'
                ORDER BY C.comment_id ASC
                """;

        List<PostCommentDTO> commentList = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    commentList.add(mapCommentRow(rs));
                }
            }
        }

        return commentList;
    }

    /* 2. 댓글 등록 */
    public long insertComment(Connection conn, PostCommentDTO comment) throws SQLException {
        String sql = """
                INSERT INTO POST_COMMENT (
                    comment_id,
                    post_id,
                    member_id,
                    parent_comment_id,
                    content,
                    status,
                    created_at
                ) VALUES (
                    SEQ_POST_COMMENT.NEXTVAL,
                    ?,
                    ?,
                    ?,
                    ?,
                    'ACTIVE',
                    SYSDATE
                )
                """;

        String[] generatedColumns = {"COMMENT_ID"};

        try (PreparedStatement pstmt = conn.prepareStatement(sql, generatedColumns)) {
            pstmt.setLong(1, comment.getPostId());
            pstmt.setLong(2, comment.getMemberId());

            if (comment.getParentCommentId() == null) {
                pstmt.setNull(3, java.sql.Types.NUMERIC);
            } else {
                pstmt.setLong(3, comment.getParentCommentId());
            }

            pstmt.setString(4, comment.getContent());

            int result = pstmt.executeUpdate();

            if (result == 0) {
                throw new SQLException("댓글 등록에 실패했습니다.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }

        throw new SQLException("생성된 댓글 번호를 가져오지 못했습니다.");
    }

    /* 3. 댓글 수정 */
    public int updateComment(Connection conn, long commentId, String content) throws SQLException {
        String sql = """
                UPDATE POST_COMMENT
                SET
                    content = ?,
                    updated_at = SYSDATE
                WHERE comment_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, content);
            pstmt.setLong(2, commentId);

            return pstmt.executeUpdate();
        }
    }

    /* 4. 댓글 소프트 삭제 */
    public int deleteComment(Connection conn, long commentId) throws SQLException {
        String sql = """
                UPDATE POST_COMMENT
                SET
                    status = 'DELETED',
                    updated_at = SYSDATE
                WHERE comment_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, commentId);

            return pstmt.executeUpdate();
        }
    }

    /* 5. 댓글 존재 여부 확인 */
    public boolean existsComment(Connection conn, long commentId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM POST_COMMENT
                WHERE comment_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, commentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /* 6. 댓글 작성자 번호 조회 */
    public Long selectWriterId(Connection conn, long commentId) throws SQLException {
        String sql = """
                SELECT member_id
                FROM POST_COMMENT
                WHERE comment_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, commentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("member_id");
                }
            }
        }

        return null;
    }

    /* 7. 관리자 댓글 전체 목록 조회 */
    public List<PostCommentDTO> selectAdminCommentList(Connection conn) throws SQLException {
        String sql = """
            SELECT
                C.comment_id,
                C.post_id,
                C.member_id,
                C.parent_comment_id,
                M.nickname AS member_nickname,
                C.content,
                C.status,
                C.created_at,
                C.updated_at
            FROM POST_COMMENT C
            JOIN MEMBER M
              ON C.member_id = M.member_id
            ORDER BY C.comment_id DESC
            """;

        List<PostCommentDTO> commentList = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                commentList.add(mapCommentRow(rs));
            }
        }

        return commentList;
    }

    /* 8. 댓글 조회 결과를 DTO로 변환 */
    private PostCommentDTO mapCommentRow(ResultSet rs) throws SQLException {
        PostCommentDTO comment = new PostCommentDTO();

        comment.setCommentId(rs.getLong("comment_id"));
        comment.setPostId(rs.getLong("post_id"));
        comment.setMemberId(rs.getLong("member_id"));

        long parentCommentId = rs.getLong("parent_comment_id");
        comment.setParentCommentId(
                rs.wasNull() ? null : parentCommentId
        );

        comment.setMemberNickname(rs.getString("member_nickname"));
        comment.setContent(rs.getString("content"));
        comment.setStatus(rs.getString("status"));
        comment.setCreatedAt(rs.getTimestamp("created_at"));
        comment.setUpdatedAt(rs.getTimestamp("updated_at"));

        return comment;
    }
}