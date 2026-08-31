package service;

import dao.AdminDAO;
import dao.PostCommentDAO;
import dto.AdminMemberDTO;
import dto.PostCommentDTO;
import dto.PostDTO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class AdminService {

    private final AdminDAO adminDAO;
    private final PostCommentDAO postCommentDAO;

    public AdminService() {
        this.adminDAO = new AdminDAO();
        this.postCommentDAO = new PostCommentDAO();
    }

    /* 1. 관리자 회원 목록 조회 */
    public List<AdminMemberDTO> getMemberList() {
        try (Connection conn = DBUtil.getConnection()) {
            return adminDAO.selectMemberList(conn);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "회원 목록을 불러오지 못했습니다.",
                    e
            );
        }
    }

    /* 2. 관리자 게시글 전체 목록 조회 */
    public List<PostDTO> getPostList() {
        try (Connection conn = DBUtil.getConnection()) {
            return adminDAO.selectPostList(conn);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "게시글 목록을 불러오지 못했습니다.",
                    e
            );
        }
    }

    /* 3. 관리자 댓글 전체 목록 조회 */
    public List<PostCommentDTO> getCommentList() {
        try (Connection conn = DBUtil.getConnection()) {
            return postCommentDAO.selectAdminCommentList(conn);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "댓글 목록을 불러오지 못했습니다.",
                    e
            );
        }
    }

    /* 4. 회원 잠금 / 잠금 해제 */
    public void changeMemberLock(
            long memberId,
            boolean locked,
            long loginAdminMemberId
    ) {
        validateMemberId(memberId);
        validateMemberId(loginAdminMemberId);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            String targetRole = adminDAO.selectMemberRole(
                    conn,
                    memberId
            );

            if (targetRole == null) {
                throw new NoSuchElementException(
                        "존재하지 않는 회원입니다."
                );
            }

            if ("ADMIN".equals(targetRole)) {
                throw new IllegalArgumentException(
                        "관리자 계정은 잠금 또는 잠금 해제할 수 없습니다."
                );
            }

            int result = adminDAO.updateMemberLock(
                    conn,
                    memberId,
                    locked
            );

            if (result == 0) {
                throw new NoSuchElementException(
                        "존재하지 않는 회원입니다."
                );
            }

            conn.commit();

        } catch (SQLException e) {
            rollback(conn);

            throw new RuntimeException(
                    "회원 상태 변경에 실패했습니다.",
                    e
            );

        } catch (RuntimeException e) {
            rollback(conn);
            throw e;

        } finally {
            close(conn);
        }
    }

    /* 5. 게시글 상단 고정 / 해제 */
    public void changePostPin(
            long postId,
            boolean pinned
    ) {
        validatePostId(postId);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            int result = adminDAO.updatePostPin(
                    conn,
                    postId,
                    pinned
            );

            if (result == 0) {
                throw new NoSuchElementException(
                        "존재하지 않거나 삭제된 게시글입니다."
                );
            }

            conn.commit();

        } catch (SQLException e) {
            rollback(conn);

            throw new RuntimeException(
                    "게시글 고정 상태 변경에 실패했습니다.",
                    e
            );

        } catch (RuntimeException e) {
            rollback(conn);
            throw e;

        } finally {
            close(conn);
        }
    }

    /* 6. 관리자 댓글 삭제 */
    public void deleteCommentByAdmin(long commentId) {
        validateCommentId(commentId);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            int result = postCommentDAO.deleteComment(
                    conn,
                    commentId
            );

            if (result == 0) {
                throw new NoSuchElementException(
                        "존재하지 않거나 이미 삭제된 댓글입니다."
                );
            }

            conn.commit();

        } catch (SQLException e) {
            rollback(conn);

            throw new RuntimeException(
                    "댓글 삭제에 실패했습니다.",
                    e
            );

        } catch (RuntimeException e) {
            rollback(conn);
            throw e;

        } finally {
            close(conn);
        }
    }

    /* 7. 회원번호 검증 */
    private void validateMemberId(long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException(
                    "올바르지 않은 회원 번호입니다."
            );
        }
    }

    /* 8. 게시글번호 검증 */
    private void validatePostId(long postId) {
        if (postId <= 0) {
            throw new IllegalArgumentException(
                    "올바르지 않은 게시글 번호입니다."
            );
        }
    }

    /* 9. 댓글번호 검증 */
    private void validateCommentId(long commentId) {
        if (commentId <= 0) {
            throw new IllegalArgumentException(
                    "올바르지 않은 댓글 번호입니다."
            );
        }
    }

    /* 10. 트랜잭션 ROLLBACK */
    private void rollback(Connection conn) {
        if (conn == null) {
            return;
        }

        try {
            conn.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* 11. Connection 종료 */
    private void close(Connection conn) {
        if (conn == null) {
            return;
        }

        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}