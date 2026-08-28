package check;

import service.AdminService;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.NoSuchElementException;

public class AdminServiceCheck {

    public static void main(String[] args) {

        AdminService adminService = new AdminService();

        long notExistsMemberId = 999999L;
        long notExistsPostId = 999999L;

        System.out.println(
                "===== AdminService 검증 시작 ====="
        );

        try {

            Long testUserId = findMemberIdByRole("USER");
            Long testAdminId = findMemberIdByRole("ADMIN");
            Long testPostId = findActivePostId();

            /*
             * ============================================
             * 회원 관련 테스트
             * ============================================
             */
            if (testUserId == null) {

                System.out.println(
                        "※ USER 회원이 없어 회원 잠금 테스트를 건너뜁니다."
                );

            } else if (testAdminId == null) {

                System.out.println(
                        "※ ADMIN 회원이 없어 회원 잠금 테스트를 건너뜁니다."
                );

            } else {

                System.out.println(
                        "   테스트 USER 번호 : " + testUserId
                );

                System.out.println(
                        "   테스트 ADMIN 번호 : " + testAdminId
                );

                String originalLockStatus =
                        getMemberLockStatus(testUserId);

                /*
                 * ============================================
                 * 1. 정상 USER 회원 잠금
                 * ============================================
                 */
                try {

                    adminService.changeMemberLock(
                            testUserId,
                            true,
                            testAdminId
                    );

                    String lockStatus =
                            getMemberLockStatus(testUserId);

                    if ("Y".equals(lockStatus)) {

                        System.out.println(
                                "1. USER 회원 잠금 : PASS"
                        );

                    } else {

                        System.out.println(
                                "1. USER 회원 잠금 : FAIL"
                        );

                        System.out.println(
                                "   └ IS_LOCKED : " + lockStatus
                        );
                    }

                } catch (Exception e) {

                    System.out.println(
                            "1. USER 회원 잠금 : FAIL"
                    );

                    printException(e);
                }


                /*
                 * ============================================
                 * 2. 정상 USER 회원 잠금 해제
                 * ============================================
                 */
                try {

                    adminService.changeMemberLock(
                            testUserId,
                            false,
                            testAdminId
                    );

                    String lockStatus =
                            getMemberLockStatus(testUserId);

                    if ("N".equals(lockStatus)) {

                        System.out.println(
                                "2. USER 회원 잠금 해제 : PASS"
                        );

                    } else {

                        System.out.println(
                                "2. USER 회원 잠금 해제 : FAIL"
                        );

                        System.out.println(
                                "   └ IS_LOCKED : " + lockStatus
                        );
                    }

                } catch (Exception e) {

                    System.out.println(
                            "2. USER 회원 잠금 해제 : FAIL"
                    );

                    printException(e);
                }


                /*
                 * ============================================
                 * 3. 잘못된 회원 번호
                 *
                 * 기대 결과 : IllegalArgumentException
                 * ============================================
                 */
                try {

                    adminService.changeMemberLock(
                            0L,
                            true,
                            testAdminId
                    );

                    System.out.println(
                            "3. 잘못된 회원 번호 검증 : FAIL"
                    );

                    System.out.println(
                            "   └ 예외가 발생하지 않았습니다."
                    );

                } catch (IllegalArgumentException e) {

                    System.out.println(
                            "3. 잘못된 회원 번호 검증 : PASS"
                    );

                    printException(e);

                } catch (Exception e) {

                    System.out.println(
                            "3. 잘못된 회원 번호 검증 : FAIL"
                    );

                    printException(e);
                }


                /*
                 * ============================================
                 * 4. 존재하지 않는 회원
                 *
                 * 기대 결과 : NoSuchElementException
                 * ============================================
                 */
                try {

                    adminService.changeMemberLock(
                            notExistsMemberId,
                            true,
                            testAdminId
                    );

                    System.out.println(
                            "4. 없는 회원 처리 : FAIL"
                    );

                    System.out.println(
                            "   └ 예외가 발생하지 않았습니다."
                    );

                } catch (NoSuchElementException e) {

                    System.out.println(
                            "4. 없는 회원 처리 : PASS"
                    );

                    printException(e);

                } catch (Exception e) {

                    System.out.println(
                            "4. 없는 회원 처리 : FAIL"
                    );

                    printException(e);
                }


                /*
                 * ============================================
                 * 5. ADMIN 계정 잠금 차단
                 *
                 * 기대 결과 : IllegalArgumentException
                 * ============================================
                 */
                try {

                    adminService.changeMemberLock(
                            testAdminId,
                            true,
                            testAdminId
                    );

                    System.out.println(
                            "5. ADMIN 계정 잠금 차단 : FAIL"
                    );

                    System.out.println(
                            "   └ 관리자 계정이 잠금 처리되었습니다."
                    );

                } catch (IllegalArgumentException e) {

                    System.out.println(
                            "5. ADMIN 계정 잠금 차단 : PASS"
                    );

                    printException(e);

                } catch (Exception e) {

                    System.out.println(
                            "5. ADMIN 계정 잠금 차단 : FAIL"
                    );

                    printException(e);
                }


                /*
                 * ============================================
                 * 6. ADMIN 계정 잠금 해제 차단
                 *
                 * ADMIN은 잠금뿐 아니라 잠금 해제도
                 * 직접 변경할 수 없어야 합니다.
                 * ============================================
                 */
                try {

                    adminService.changeMemberLock(
                            testAdminId,
                            false,
                            testAdminId
                    );

                    System.out.println(
                            "6. ADMIN 계정 잠금 해제 차단 : FAIL"
                    );

                    System.out.println(
                            "   └ 관리자 계정 상태가 변경되었습니다."
                    );

                } catch (IllegalArgumentException e) {

                    System.out.println(
                            "6. ADMIN 계정 잠금 해제 차단 : PASS"
                    );

                    printException(e);

                } catch (Exception e) {

                    System.out.println(
                            "6. ADMIN 계정 잠금 해제 차단 : FAIL"
                    );

                    printException(e);
                }


                /*
                 * 테스트 전에 USER가 잠겨 있었다면
                 * 원래 상태로 복구합니다.
                 */
                boolean originalLocked =
                        "Y".equals(originalLockStatus);

                adminService.changeMemberLock(
                        testUserId,
                        originalLocked,
                        testAdminId
                );

                System.out.println(
                        "   USER 원래 잠금 상태 복구 완료 : "
                                + originalLockStatus
                );
            }


            /*
             * ============================================
             * 게시글 관련 테스트
             * ============================================
             */
            if (testPostId == null) {

                System.out.println(
                        "※ ACTIVE 게시글이 없어 게시글 고정 테스트를 건너뜁니다."
                );

            } else {

                System.out.println(
                        "   테스트 게시글 번호 : " + testPostId
                );

                String originalPinStatus =
                        getPostPinStatus(testPostId);


                /*
                 * ============================================
                 * 7. 정상 게시글 상단 고정
                 * ============================================
                 */
                try {

                    adminService.changePostPin(
                            testPostId,
                            true
                    );

                    if ("Y".equals(
                            getPostPinStatus(testPostId)
                    )) {

                        System.out.println(
                                "7. 정상 게시글 상단 고정 : PASS"
                        );

                    } else {

                        System.out.println(
                                "7. 정상 게시글 상단 고정 : FAIL"
                        );
                    }

                } catch (Exception e) {

                    System.out.println(
                            "7. 정상 게시글 상단 고정 : FAIL"
                    );

                    printException(e);
                }


                /*
                 * ============================================
                 * 8. 정상 게시글 고정 해제
                 * ============================================
                 */
                try {

                    adminService.changePostPin(
                            testPostId,
                            false
                    );

                    if ("N".equals(
                            getPostPinStatus(testPostId)
                    )) {

                        System.out.println(
                                "8. 정상 게시글 고정 해제 : PASS"
                        );

                    } else {

                        System.out.println(
                                "8. 정상 게시글 고정 해제 : FAIL"
                        );
                    }

                } catch (Exception e) {

                    System.out.println(
                            "8. 정상 게시글 고정 해제 : FAIL"
                    );

                    printException(e);
                }


                /*
                 * 원래 게시글 고정 상태로 복구
                 */
                adminService.changePostPin(
                        testPostId,
                        "Y".equals(originalPinStatus)
                );

                System.out.println(
                        "   게시글 원래 고정 상태 복구 완료 : "
                                + originalPinStatus
                );
            }


            /*
             * ============================================
             * 9. 잘못된 게시글 번호
             * ============================================
             */
            try {

                adminService.changePostPin(
                        0L,
                        true
                );

                System.out.println(
                        "9. 잘못된 게시글 번호 검증 : FAIL"
                );

            } catch (IllegalArgumentException e) {

                System.out.println(
                        "9. 잘못된 게시글 번호 검증 : PASS"
                );

                printException(e);

            } catch (Exception e) {

                System.out.println(
                        "9. 잘못된 게시글 번호 검증 : FAIL"
                );

                printException(e);
            }


            /*
             * ============================================
             * 10. 존재하지 않는 게시글
             * ============================================
             */
            try {

                adminService.changePostPin(
                        notExistsPostId,
                        true
                );

                System.out.println(
                        "10. 없는 게시글 처리 : FAIL"
                );

            } catch (NoSuchElementException e) {

                System.out.println(
                        "10. 없는 게시글 처리 : PASS"
                );

                printException(e);

            } catch (Exception e) {

                System.out.println(
                        "10. 없는 게시글 처리 : FAIL"
                );

                printException(e);
            }


            /*
             * ============================================
             * 11. 관리자 회원 목록 조회
             * ============================================
             */
            try {

                var members =
                        adminService.getMemberList();

                if (members != null) {

                    System.out.println(
                            "11. 관리자 회원 목록 조회 : PASS"
                    );

                    System.out.println(
                            "   현재 회원 수 : "
                                    + members.size()
                    );

                } else {

                    System.out.println(
                            "11. 관리자 회원 목록 조회 : FAIL"
                    );
                }

            } catch (Exception e) {

                System.out.println(
                        "11. 관리자 회원 목록 조회 : FAIL"
                );

                printException(e);
            }

        } catch (Exception e) {

            System.out.println(
                    "검증 준비 중 오류 발생"
            );

            e.printStackTrace();
        }

        System.out.println(
                "===== AdminService 검증 종료 ====="
        );
    }


    /*
     * ROLE에 맞는 테스트 회원 조회
     */
    private static Long findMemberIdByRole(
            String role
    ) throws Exception {

        String sql = """
                SELECT member_id
                FROM MEMBER
                WHERE role = ?
                  AND ROWNUM = 1
                """;

        try (
                Connection conn =
                        DBUtil.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, role);

            try (ResultSet rs =
                         pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong(
                            "member_id"
                    );
                }
            }
        }

        return null;
    }


    /*
     * ACTIVE 게시글 조회
     */
    private static Long findActivePostId()
            throws Exception {

        String sql = """
                SELECT post_id
                FROM POST
                WHERE status = 'ACTIVE'
                  AND ROWNUM = 1
                """;

        try (
                Connection conn =
                        DBUtil.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql);

                ResultSet rs =
                        pstmt.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getLong(
                        "post_id"
                );
            }
        }

        return null;
    }


    /*
     * 회원 잠금 상태 조회
     */
    private static String getMemberLockStatus(
            long memberId
    ) throws Exception {

        String sql = """
                SELECT is_locked
                FROM MEMBER
                WHERE member_id = ?
                """;

        try (
                Connection conn =
                        DBUtil.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql)
        ) {

            pstmt.setLong(1, memberId);

            try (ResultSet rs =
                         pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getString(
                            "is_locked"
                    );
                }
            }
        }

        return null;
    }


    /*
     * 게시글 고정 상태 조회
     */
    private static String getPostPinStatus(
            long postId
    ) throws Exception {

        String sql = """
                SELECT is_pinned
                FROM POST
                WHERE post_id = ?
                """;

        try (
                Connection conn =
                        DBUtil.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql)
        ) {

            pstmt.setLong(1, postId);

            try (ResultSet rs =
                         pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getString(
                            "is_pinned"
                    );
                }
            }
        }

        return null;
    }


    private static void printException(
            Exception e
    ) {

        System.out.println(
                "   └ "
                        + e.getClass().getSimpleName()
                        + " : "
                        + e.getMessage()
        );
    }
}