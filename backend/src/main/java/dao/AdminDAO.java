package dao;

import dto.AdminMemberDTO;
import dto.PostDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    /* 1. 관리자 회원 목록 조회 */
    public List<AdminMemberDTO> selectMemberList(Connection conn) throws SQLException {
        String sql = """
                SELECT
                    member_id,
                    login_id,
                    nickname,
                    email,
                    role,
                    fail_count,
                    is_locked,
                    last_login_at,
                    created_at
                FROM MEMBER
                ORDER BY member_id DESC
                """;

        List<AdminMemberDTO> memberList = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                AdminMemberDTO member = new AdminMemberDTO();

                member.setMemberId(rs.getLong("member_id"));
                member.setLoginId(rs.getString("login_id"));
                member.setNickname(rs.getString("nickname"));
                member.setEmail(rs.getString("email"));
                member.setRole(rs.getString("role"));
                member.setFailCount(rs.getInt("fail_count"));
                member.setIsLocked(rs.getString("is_locked"));
                member.setLastLoginAt(rs.getTimestamp("last_login_at"));
                member.setCreatedAt(rs.getTimestamp("created_at"));

                memberList.add(member);
            }
        }

        return memberList;
    }

    /* 2. 회원 권한 조회 */
    public String selectMemberRole(Connection conn, long memberId) throws SQLException {
        String sql = """
                SELECT role
                FROM MEMBER
                WHERE member_id = ?
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, memberId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        }

        return null;
    }

    /* 3. 회원 잠금 및 잠금 해제 */
    public int updateMemberLock(Connection conn, long memberId, boolean locked) throws SQLException {
        String sql = """
                UPDATE MEMBER
                SET
                    is_locked = ?,
                    fail_count =
                        CASE
                            WHEN ? = 'N'
                            THEN 0
                            ELSE fail_count
                        END
                WHERE member_id = ?
                """;

        String lockValue = locked ? "Y" : "N";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, lockValue);
            pstmt.setString(2, lockValue);
            pstmt.setLong(3, memberId);

            return pstmt.executeUpdate();
        }
    }

    /* 4. 관리자 게시글 전체 목록 조회 */
    public List<PostDTO> selectPostList(Connection conn) throws SQLException {
        String sql = """
                SELECT
                    P.post_id,
                    P.member_id,
                    M.nickname AS member_nickname,
                    P.category,
                    P.title,
                    P.genre,
                    P.view_count,
                    P.is_pinned,
                    P.status,
                    P.created_at,
                    P.updated_at,
                    (
                        SELECT COUNT(*)
                        FROM POST_LIKE PL
                        WHERE PL.post_id = P.post_id
                    ) AS like_count
                FROM POST P
                JOIN MEMBER M
                  ON P.member_id = M.member_id
                ORDER BY
                    CASE
                        WHEN P.status = 'ACTIVE' AND P.is_pinned = 'Y' THEN 0
                        ELSE 1
                    END,
                    P.post_id DESC
                """;

        List<PostDTO> postList = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                PostDTO post = new PostDTO();

                post.setPostId(rs.getLong("post_id"));
                post.setMemberId(rs.getLong("member_id"));
                post.setMemberNickname(rs.getString("member_nickname"));
                post.setCategory(rs.getString("category"));
                post.setTitle(rs.getString("title"));
                post.setGenre(rs.getString("genre"));
                post.setViewCount(rs.getInt("view_count"));
                post.setLikeCount(rs.getInt("like_count"));
                post.setIsPinned(rs.getString("is_pinned"));
                post.setStatus(rs.getString("status"));
                post.setCreatedAt(rs.getTimestamp("created_at"));
                post.setUpdatedAt(rs.getTimestamp("updated_at"));

                postList.add(post);
            }
        }

        return postList;
    }

    /* 5. 게시글 상단 고정 및 해제 */
    public int updatePostPin(Connection conn, long postId, boolean pinned) throws SQLException {
        String sql = """
                UPDATE POST
                SET
                    is_pinned = ?,
                    updated_at = SYSDATE
                WHERE post_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pinned ? "Y" : "N");
            pstmt.setLong(2, postId);

            return pstmt.executeUpdate();
        }
    }
}