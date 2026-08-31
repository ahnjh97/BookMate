package service;

import util.DBUtil;

import java.sql.*;
import java.util.*;

public class PostCommentService {
    public List<Map<String, Object>> findComments(long postId) {
        validatePostId(postId);
        String sql = """
                SELECT C.comment_id,C.post_id,C.member_id,C.parent_comment_id,C.content,C.status,
                       C.created_at,C.updated_at,M.nickname
                  FROM POST_COMMENT C JOIN MEMBER M ON M.member_id=C.member_id
                 WHERE C.post_id=?
                 ORDER BY NVL(C.parent_comment_id,C.comment_id),
                          CASE WHEN C.parent_comment_id IS NULL THEN 0 ELSE 1 END,
                          C.created_at,C.comment_id
                """;
        List<Map<String, Object>> comments = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, postId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> comment = new LinkedHashMap<>();
                    comment.put("commentId", rs.getLong("comment_id"));
                    comment.put("postId", rs.getLong("post_id"));
                    comment.put("memberId", rs.getLong("member_id"));
                    long parentId = rs.getLong("parent_comment_id");
                    comment.put("parentCommentId", rs.wasNull() ? null : parentId);
                    boolean active = "ACTIVE".equals(rs.getString("status"));
                    comment.put("content", active ? rs.getString("content") : null);
                    comment.put("status", rs.getString("status"));
                    comment.put("createdAt", rs.getTimestamp("created_at"));
                    comment.put("updatedAt", rs.getTimestamp("updated_at"));
                    comment.put("memberNickname", rs.getString("nickname"));
                    comments.add(comment);
                }
            }
            return comments;
        } catch (SQLException exception) {
            throw new RuntimeException("댓글을 불러오지 못했습니다.", exception);
        }
    }

    public long createComment(long postId, long memberId, Long parentCommentId, String content) {
        validatePostId(postId);
        if (memberId <= 0) throw new IllegalArgumentException("로그인이 필요한 기능입니다.");
        String safeContent = normalizeContent(content);
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!activePostExists(connection, postId)) throw new NoSuchElementException("존재하지 않는 게시글입니다.");
                Long rootParentId = resolveRootParent(connection, postId, parentCommentId);
                String sql = "INSERT INTO POST_COMMENT(comment_id,post_id,member_id,parent_comment_id,content) VALUES(SEQ_POST_COMMENT.NEXTVAL,?,?,?,?)";
                long commentId;
                try (PreparedStatement statement = connection.prepareStatement(sql, new String[]{"comment_id"})) {
                    statement.setLong(1, postId);
                    statement.setLong(2, memberId);
                    if (rootParentId == null) statement.setNull(3, Types.NUMERIC); else statement.setLong(3, rootParentId);
                    statement.setString(4, safeContent);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) { keys.next(); commentId = keys.getLong(1); }
                }
                connection.commit();
                return commentId;
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("댓글을 등록하지 못했습니다.", exception);
        }
    }

    public void updateComment(long commentId, long memberId, String content) {
        if (commentId <= 0 || memberId <= 0) throw new IllegalArgumentException("올바른 댓글 정보가 필요합니다.");
        String sql = "UPDATE POST_COMMENT SET content=?,updated_at=SYSDATE WHERE comment_id=? AND member_id=? AND status='ACTIVE'";
        try (Connection connection = DBUtil.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeContent(content));
            statement.setLong(2, commentId);
            statement.setLong(3, memberId);
            if (statement.executeUpdate() != 1) throw new NoSuchElementException("수정할 수 있는 댓글을 찾지 못했습니다.");
        } catch (SQLException exception) {
            throw new RuntimeException("댓글을 수정하지 못했습니다.", exception);
        }
    }

    public void deleteComment(long commentId, long memberId, String role) {
        if (commentId <= 0 || memberId <= 0) throw new IllegalArgumentException("올바른 댓글 정보가 필요합니다.");
        String sql = "ADMIN".equals(role)
                ? "UPDATE POST_COMMENT SET status='DELETED',updated_at=SYSDATE WHERE comment_id=? AND status='ACTIVE'"
                : "UPDATE POST_COMMENT SET status='DELETED',updated_at=SYSDATE WHERE comment_id=? AND member_id=? AND status='ACTIVE'";
        try (Connection connection = DBUtil.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, commentId);
            if (!"ADMIN".equals(role)) statement.setLong(2, memberId);
            if (statement.executeUpdate() != 1) throw new NoSuchElementException("삭제할 수 있는 댓글을 찾지 못했습니다.");
        } catch (SQLException exception) {
            throw new RuntimeException("댓글을 삭제하지 못했습니다.", exception);
        }
    }

    private boolean activePostExists(Connection connection, long postId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM POST WHERE post_id=? AND status='ACTIVE'")) {
            statement.setLong(1, postId);
            try (ResultSet rs = statement.executeQuery()) { return rs.next(); }
        }
    }

    private Long resolveRootParent(Connection connection, long postId, Long parentId) throws SQLException {
        if (parentId == null) return null;
        String sql = "SELECT parent_comment_id FROM POST_COMMENT WHERE comment_id=? AND post_id=? AND status='ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parentId);
            statement.setLong(2, postId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("답글을 작성할 댓글을 찾지 못했습니다.");
                long rootId = rs.getLong(1);
                return rs.wasNull() ? parentId : rootId;
            }
        }
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("댓글 내용을 입력해 주세요.");
        if (normalized.length() > 1000) throw new IllegalArgumentException("댓글은 1,000자 이하로 작성해 주세요.");
        return normalized;
    }

    private void validatePostId(long postId) {
        if (postId <= 0) throw new IllegalArgumentException("올바른 게시글 번호가 필요합니다.");
    }
}
