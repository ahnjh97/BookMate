package dao;

import dto.PostDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostDAO {

    /* 게시글 전체 목록 조회: ACTIVE 게시글만 조회하고 고정 게시글을 먼저 정렬 */
    public List<PostDTO> selectPostList(Connection conn) throws SQLException {
        String sql = """
                SELECT
                    P.post_id,
                    P.member_id,
                    P.tier_list_id,
                    M.nickname AS member_nickname,
                    P.category,
                    P.title,
                    P.genre,
                    P.view_count,
                    P.is_pinned,
                    P.status,
                    P.created_at,
                    P.updated_at
                FROM POST P
                JOIN MEMBER M
                  ON P.member_id = M.member_id
                WHERE P.status = 'ACTIVE'
                ORDER BY
                    P.is_pinned DESC,
                    P.post_id DESC
                """;

        List<PostDTO> postList = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                postList.add(mapPostListRow(rs));
            }
        }

        return postList;
    }

    /* 게시글 상세 조회 */
    public PostDTO selectPostById(Connection conn, long postId) throws SQLException {
        String sql = """
                SELECT
                    P.post_id,
                    P.member_id,
                    P.tier_list_id,
                    M.nickname AS member_nickname,
                    P.category,
                    P.title,
                    P.content,
                    P.genre,
                    P.view_count,
                    P.is_pinned,
                    P.status,
                    P.created_at,
                    P.updated_at
                FROM POST P
                JOIN MEMBER M
                  ON P.member_id = M.member_id
                WHERE P.post_id = ?
                  AND P.status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapPostDetailRow(rs);
                }
            }
        }

        return null;
    }

    /* 게시글 등록: 새 게시글은 IS_PINNED = 'N'으로 등록 */
    public long insertPost(Connection conn, PostDTO post) throws SQLException {
        String sql = """
                INSERT INTO POST (
                    post_id,
                    member_id,
                    category,
                    title,
                    content,
                    genre,
                    view_count,
                    is_pinned,
                    status,
                    created_at
                ) VALUES (
                    seq_post.NEXTVAL,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    0,
                    'N',
                    'ACTIVE',
                    SYSDATE
                )
                """;

        String[] generatedColumns = {"POST_ID"};

        try (PreparedStatement pstmt = conn.prepareStatement(sql, generatedColumns)) {
            pstmt.setLong(1, post.getMemberId());
            pstmt.setString(2, post.getCategory());
            pstmt.setString(3, post.getTitle());
            pstmt.setString(4, post.getContent());
            pstmt.setString(5, post.getGenre());

            int result = pstmt.executeUpdate();

            if (result == 0) {
                throw new SQLException("게시글 등록에 실패했습니다.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }

        throw new SQLException("생성된 게시글 번호를 가져오지 못했습니다.");
    }

    /* 게시글 수정 */
    public int updatePost(Connection conn, PostDTO post) throws SQLException {
        String sql = """
                UPDATE POST
                SET
                    category = ?,
                    title = ?,
                    content = ?,
                    genre = ?,
                    updated_at = SYSDATE
                WHERE post_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, post.getCategory());
            pstmt.setString(2, post.getTitle());
            pstmt.setString(3, post.getContent());
            pstmt.setString(4, post.getGenre());
            pstmt.setLong(5, post.getPostId());

            return pstmt.executeUpdate();
        }
    }

    /* 작성자에 의한 게시글 소프트 삭제 */
    public int deletePostByWriter(Connection conn, long postId) throws SQLException {
        String sql = """
                UPDATE POST
                SET
                    status = 'DELETED_BY_WRITER',
                    updated_at = SYSDATE
                WHERE post_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            return pstmt.executeUpdate();
        }
    }

    /* 관리자에 의한 게시글 소프트 삭제 */
    public int deletePostByAdmin(Connection conn, long postId) throws SQLException {
        String sql = """
                UPDATE POST
                SET
                    status = 'DELETED_BY_ADMIN',
                    updated_at = SYSDATE
                WHERE post_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            return pstmt.executeUpdate();
        }
    }

    /* 조회수 증가 */
    public int increaseViewCount(Connection conn, long postId) throws SQLException {
        String sql = """
                UPDATE POST
                SET view_count = view_count + 1
                WHERE post_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            return pstmt.executeUpdate();
        }
    }

    /* 게시글 존재 여부 확인 */
    public boolean existsPost(Connection conn, long postId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM POST
                WHERE post_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /* 게시글 작성자 번호 조회 */
    public Long selectWriterId(Connection conn, long postId) throws SQLException {
        String sql = """
                SELECT member_id
                FROM POST
                WHERE post_id = ?
                  AND status = 'ACTIVE'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("member_id");
                }
            }
        }

        return null;
    }

    /* 게시글 목록 결과를 DTO로 변환 */
    private PostDTO mapPostListRow(ResultSet rs) throws SQLException {
        PostDTO post = new PostDTO();
        post.setPostId(rs.getLong("post_id"));
        post.setMemberId(rs.getLong("member_id"));
        long tierListId = rs.getLong("tier_list_id");
        post.setTierListId(rs.wasNull() ? null : tierListId);
        post.setMemberNickname(rs.getString("member_nickname"));
        post.setCategory(rs.getString("category"));
        post.setTitle(rs.getString("title"));
        post.setGenre(rs.getString("genre"));
        post.setViewCount(rs.getInt("view_count"));
        post.setIsPinned(rs.getString("is_pinned"));
        post.setStatus(rs.getString("status"));
        post.setCreatedAt(rs.getTimestamp("created_at"));
        post.setUpdatedAt(rs.getTimestamp("updated_at"));
        return post;
    }

    /* 게시글 상세 결과를 DTO로 변환 */
    private PostDTO mapPostDetailRow(ResultSet rs) throws SQLException {
        PostDTO post = mapPostListRow(rs);
        post.setContent(rs.getString("content"));
        return post;
    }
}
