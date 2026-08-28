package service;

import dao.AdminDAO;
import dto.AdminMemberDTO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class AdminService {

    private final AdminDAO adminDAO;

    public AdminService() {
        this.adminDAO = new AdminDAO();
    }


    /*
     * =========================================
     * 관리자 회원 목록 조회
     * =========================================
     */
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


    /*
     * =========================================
     * 회원 잠금 / 잠금 해제
     *
     * ADMIN 계정은 잠금/해제 불가
     * =========================================
     */
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

            /*
             * 변경 대상 회원 ROLE 조회
             */
            String targetRole =
                    adminDAO.selectMemberRole(
                            conn,
                            memberId
                    );

            /*
             * 존재하지 않는 회원
             */
            if (targetRole == null) {
                throw new NoSuchElementException(
                        "존재하지 않는 회원입니다."
                );
            }

            /*
             * 관리자 계정 보호
             */
            if ("ADMIN".equals(targetRole)) {
                throw new IllegalArgumentException(
                        "관리자 계정은 잠금 또는 잠금 해제할 수 없습니다."
                );
            }

            /*
             * 일반회원 잠금 상태 변경
             */
            int result =
                    adminDAO.updateMemberLock(
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


    /*
     * =========================================
     * 게시글 상단 고정 / 해제
     * =========================================
     */
    public void changePostPin(
            long postId,
            boolean pinned
    ) {

        validatePostId(postId);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            int result =
                    adminDAO.updatePostPin(
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


    /*
     * =========================================
     * 회원번호 검증
     * =========================================
     */
    private void validateMemberId(long memberId) {

        if (memberId <= 0) {
            throw new IllegalArgumentException(
                    "올바르지 않은 회원 번호입니다."
            );
        }
    }


    /*
     * =========================================
     * 게시글번호 검증
     * =========================================
     */
    private void validatePostId(long postId) {

        if (postId <= 0) {
            throw new IllegalArgumentException(
                    "올바르지 않은 게시글 번호입니다."
            );
        }
    }


    /*
     * =========================================
     * 트랜잭션 ROLLBACK
     * =========================================
     */
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


    /*
     * =========================================
     * Connection 종료
     * =========================================
     */
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