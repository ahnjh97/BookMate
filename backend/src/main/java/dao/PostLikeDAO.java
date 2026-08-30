package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostLikeDAO {

    /* 게시글 좋아요 등록 */
    public int insertPostLike(Connection conn, long postId, long memberId) throws SQLException {
        String sql = """
                INSERT INTO POST_LIKE (
                    post_like_id,
                    post_id,
                    member_id,
                    created_at
                ) VALUES (
                    seq_post_like.NEXTVAL,
                    ?,
                    ?,
                    SYSDATE
                )
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            pstmt.setLong(2, memberId);
            return pstmt.executeUpdate();
        }
    }

    /* 게시글 좋아요 취소 */
    public int deletePostLike(Connection conn, long postId, long memberId) throws SQLException {
        String sql = """
                DELETE FROM POST_LIKE
                WHERE post_id = ?
                  AND member_id = ?
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            pstmt.setLong(2, memberId);
            return pstmt.executeUpdate();
        }
    }

    /* 회원의 게시글 좋아요 여부 확인 */
    public boolean existsPostLike(Connection conn, long postId, long memberId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM POST_LIKE
                WHERE post_id = ?
                  AND member_id = ?
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            pstmt.setLong(2, memberId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /* 게시글 좋아요 개수 조회 */
    public int countPostLikes(Connection conn, long postId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM POST_LIKE
                WHERE post_id = ?
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }
}