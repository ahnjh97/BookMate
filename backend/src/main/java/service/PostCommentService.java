package service;

import dao.PostCommentDAO;
import dao.PostDAO;
import dto.PostCommentDTO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class PostCommentService {

    private final PostCommentDAO postCommentDAO;
    private final PostDAO postDAO;

    public PostCommentService() {
        postCommentDAO = new PostCommentDAO();
        postDAO = new PostDAO();
    }

    /* 1. 게시글 댓글 목록 조회 */
    public List<PostCommentDTO> getCommentList(long postId) {
        validatePostId(postId);

        try (Connection conn = DBUtil.getConnection()) {
            if (!postDAO.existsPost(conn, postId)) {
                throw new NoSuchElementException("존재하지 않거나 조회할 수 없는 게시글입니다.");
            }

            return postCommentDAO.selectCommentList(conn, postId);
        } catch (SQLException e) {
            throw new RuntimeException("댓글 목록을 불러오지 못했습니다.", e);
        }
    }

    /* 2. 댓글 등록 */
    public long createComment(long postId, long loginMemberId, String content) {
        validatePostId(postId);
        validateMemberId(loginMemberId);
        validateContent(content);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (!postDAO.existsPost(conn, postId)) {
                throw new NoSuchElementException("존재하지 않거나 조회할 수 없는 게시글입니다.");
            }

            PostCommentDTO comment = new PostCommentDTO();
            comment.setPostId(postId);
            comment.setMemberId(loginMemberId);
            comment.setParentCommentId(null);
            comment.setContent(content.trim());

            long commentId =
                    postCommentDAO.insertComment(conn, comment);

            conn.commit();

            return commentId;
        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("댓글 등록에 실패했습니다.", e);
        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } finally {
            close(conn);
        }
    }

    /* 3. 댓글 수정 */
    public void updateComment(
            long commentId,
            long loginMemberId,
            String content
    ) {
        validateCommentId(commentId);
        validateMemberId(loginMemberId);
        validateContent(content);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            Long writerId =
                    postCommentDAO.selectWriterId(conn, commentId);

            if (writerId == null) {
                throw new NoSuchElementException("존재하지 않거나 삭제된 댓글입니다.");
            }

            if (writerId != loginMemberId) {
                throw new SecurityException("본인이 작성한 댓글만 수정할 수 있습니다.");
            }

            int result = postCommentDAO.updateComment(
                    conn,
                    commentId,
                    content.trim()
            );

            if (result == 0) {
                throw new NoSuchElementException("수정할 댓글을 찾을 수 없습니다.");
            }

            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("댓글 수정에 실패했습니다.", e);
        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } finally {
            close(conn);
        }
    }

    /* 4. 댓글 삭제 */
    public void deleteComment(long commentId, long loginMemberId) {
        validateCommentId(commentId);
        validateMemberId(loginMemberId);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            Long writerId =
                    postCommentDAO.selectWriterId(conn, commentId);

            if (writerId == null) {
                throw new NoSuchElementException("존재하지 않거나 삭제된 댓글입니다.");
            }

            if (writerId != loginMemberId) {
                throw new SecurityException("본인이 작성한 댓글만 삭제할 수 있습니다.");
            }

            int result =
                    postCommentDAO.deleteComment(conn, commentId);

            if (result == 0) {
                throw new NoSuchElementException("삭제할 댓글을 찾을 수 없습니다.");
            }

            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("댓글 삭제에 실패했습니다.", e);
        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } finally {
            close(conn);
        }
    }

    /* 5. 게시글 번호 검증 */
    private void validatePostId(long postId) {
        if (postId <= 0) {
            throw new IllegalArgumentException("올바르지 않은 게시글 번호입니다.");
        }
    }

    /* 6. 댓글 번호 검증 */
    private void validateCommentId(long commentId) {
        if (commentId <= 0) {
            throw new IllegalArgumentException("올바르지 않은 댓글 번호입니다.");
        }
    }

    /* 7. 회원 번호 검증 */
    private void validateMemberId(long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("로그인 회원 정보가 올바르지 않습니다.");
        }
    }

    /* 8. 댓글 내용 검증 */
    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("댓글 내용을 입력해주세요.");
        }

        if (content.trim().length() > 1000) {
            throw new IllegalArgumentException("댓글은 1000자 이하로 입력해주세요.");
        }
    }

    /* 9. 트랜잭션 롤백 */
    private void rollback(Connection conn) {
        if (conn == null) return;

        try {
            conn.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* 10. DB 연결 종료 */
    private void close(Connection conn) {
        if (conn == null) return;

        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}