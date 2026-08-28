package check;

import dao.AdminDAO;
import dto.AdminMemberDTO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class AdminDAOCheck {

    public static void main(String[] args) {

        AdminDAO adminDAO = new AdminDAO();

        long notExistsMemberId = 999999L;
        long notExistsPostId = 999999L;

        System.out.println(
                "===== AdminDAO DB 연동 검증 시작 ====="
        );

        try (Connection conn = DBUtil.getConnection()) {

            /*
             * 테스트 중 변경사항을 실제 DB에 남기지 않도록
             * 트랜잭션을 수동으로 관리합니다.
             */
            conn.setAutoCommit(false);

            try {

                /*
                 * ============================================
                 * 1. 관리자용 회원 전체 목록 조회
                 * ============================================
                 */
                try {

                    List<AdminMemberDTO> members =
                            adminDAO.selectMemberList(conn);

                    System.out.println(
                            "1. 회원 목록 조회 : PASS"
                    );

                    System.out.println(
                            "   현재 회원 수 : "
                                    + members.size()
                    );

                } catch (Exception e) {

                    System.out.println(
                            "1. 회원 목록 조회 : FAIL"
                    );

                    System.out.println(
                            "   └ " + e.getMessage()
                    );
                }


                /*
                 * 테스트에 사용할 실제 회원 번호를 조회합니다.
                 */
                Long testMemberId =
                        findTestMemberId(conn);

                if (testMemberId == null) {

                    System.out.println(
                            "※ MEMBER 테이블에 회원이 없어 "
                                    + "회원 잠금 테스트를 건너뜁니다."
                    );

                } else {

                    System.out.println(
                            "   테스트 회원 번호 : "
                                    + testMemberId
                    );
                    /*
                     * ============================================
                     * 2. 회원 ROLE 조회
                     *
                     * 기대 결과
                     * USER 테스트 회원의 ROLE = USER
                     * ============================================
                     */
                    try {

                        String role =
                                adminDAO.selectMemberRole(
                                        conn,
                                        testMemberId
                                );

                        if ("USER".equals(role)) {

                            System.out.println(
                                    "2. 회원 ROLE 조회 : PASS"
                            );

                            System.out.println(
                                    "   ROLE : " + role
                            );

                        } else {

                            System.out.println(
                                    "2. 회원 ROLE 조회 : FAIL"
                            );

                            System.out.println(
                                    "   └ ROLE : " + role
                            );
                        }

                    } catch (Exception e) {

                        System.out.println(
                                "2. 회원 ROLE 조회 : FAIL"
                        );

                        System.out.println(
                                "   └ " + e.getMessage()
                        );
                    }

                    /*
                     * ============================================
                     * 3. 회원 잠금
                     *
                     * 기대 결과
                     * UPDATE 1건
                     * IS_LOCKED = 'Y'
                     * ============================================
                     */
                    try {

                        int result =
                                adminDAO.updateMemberLock(
                                        conn,
                                        testMemberId,
                                        true
                                );

                        String isLocked =
                                getMemberLockStatus(
                                        conn,
                                        testMemberId
                                );

                        if (result == 1
                                && "Y".equals(isLocked)) {

                            System.out.println(
                                    "2. 회원 잠금 : PASS"
                            );

                        } else {

                            System.out.println(
                                    "2. 회원 잠금 : FAIL"
                            );

                            System.out.println(
                                    "   └ UPDATE 결과 : "
                                            + result
                            );

                            System.out.println(
                                    "   └ IS_LOCKED : "
                                            + isLocked
                            );
                        }

                    } catch (Exception e) {

                        System.out.println(
                                "2. 회원 잠금 : FAIL"
                        );

                        System.out.println(
                                "   └ " + e.getMessage()
                        );
                    }


                    /*
                     * ============================================
                     * 4. 회원 잠금 해제
                     *
                     * 기대 결과
                     * UPDATE 1건
                     * IS_LOCKED = 'N'
                     * FAIL_COUNT = 0
                     * ============================================
                     */
                    try {

                        int result =
                                adminDAO.updateMemberLock(
                                        conn,
                                        testMemberId,
                                        false
                                );

                        String isLocked =
                                getMemberLockStatus(
                                        conn,
                                        testMemberId
                                );

                        int failCount =
                                getMemberFailCount(
                                        conn,
                                        testMemberId
                                );

                        if (result == 1
                                && "N".equals(isLocked)
                                && failCount == 0) {

                            System.out.println(
                                    "3. 회원 잠금 해제 : PASS"
                            );

                        } else {

                            System.out.println(
                                    "3. 회원 잠금 해제 : FAIL"
                            );

                            System.out.println(
                                    "   └ UPDATE 결과 : "
                                            + result
                            );

                            System.out.println(
                                    "   └ IS_LOCKED : "
                                            + isLocked
                            );

                            System.out.println(
                                    "   └ FAIL_COUNT : "
                                            + failCount
                            );
                        }

                    } catch (Exception e) {

                        System.out.println(
                                "3. 회원 잠금 해제 : FAIL"
                        );

                        System.out.println(
                                "   └ " + e.getMessage()
                        );
                    }
                }
                /*
                 * ============================================
                 * 존재하지 않는 회원 ROLE 조회
                 *
                 * 기대 결과 : null
                 * ============================================
                 */
                try {

                    String role =
                            adminDAO.selectMemberRole(
                                    conn,
                                    notExistsMemberId
                            );

                    if (role == null) {

                        System.out.println(
                                "5. 없는 회원 ROLE 조회 : PASS"
                        );

                    } else {

                        System.out.println(
                                "5. 없는 회원 ROLE 조회 : FAIL"
                        );

                        System.out.println(
                                "   └ ROLE : " + role
                        );
                    }

                } catch (Exception e) {

                    System.out.println(
                            "5. 없는 회원 ROLE 조회 : FAIL"
                    );

                    System.out.println(
                            "   └ " + e.getMessage()
                    );
                }

                /*
                 * ============================================
                 * 6. 존재하지 않는 회원 잠금
                 *
                 * 기대 결과 : 0건 수정
                 * ============================================
                 */
                try {

                    int result =
                            adminDAO.updateMemberLock(
                                    conn,
                                    notExistsMemberId,
                                    true
                            );

                    if (result == 0) {

                        System.out.println(
                                "4. 없는 회원 잠금 : PASS"
                        );

                    } else {

                        System.out.println(
                                "4. 없는 회원 잠금 : FAIL"
                        );

                        System.out.println(
                                "   └ UPDATE 결과 : "
                                        + result
                        );
                    }

                } catch (Exception e) {

                    System.out.println(
                            "4. 없는 회원 잠금 : FAIL"
                    );

                    System.out.println(
                            "   └ " + e.getMessage()
                    );
                }


                /*
                 * 게시글 상단 고정 테스트에 사용할
                 * ACTIVE 게시글을 하나 조회합니다.
                 */
                Long testPostId =
                        findActivePostId(conn);

                if (testPostId == null) {

                    System.out.println(
                            "※ ACTIVE 게시글이 없어 "
                                    + "게시글 고정 테스트를 건너뜁니다."
                    );

                } else {

                    System.out.println(
                            "   테스트 게시글 번호 : "
                                    + testPostId
                    );


                    /*
                     * ============================================
                     * 7. 게시글 상단 고정
                     *
                     * 기대 결과
                     * UPDATE 1건
                     * IS_PINNED = 'Y'
                     * ============================================
                     */
                    try {

                        int result =
                                adminDAO.updatePostPin(
                                        conn,
                                        testPostId,
                                        true
                                );

                        String isPinned =
                                getPostPinStatus(
                                        conn,
                                        testPostId
                                );

                        if (result == 1
                                && "Y".equals(isPinned)) {

                            System.out.println(
                                    "5. 게시글 상단 고정 : PASS"
                            );

                        } else {

                            System.out.println(
                                    "5. 게시글 상단 고정 : FAIL"
                            );

                            System.out.println(
                                    "   └ UPDATE 결과 : "
                                            + result
                            );

                            System.out.println(
                                    "   └ IS_PINNED : "
                                            + isPinned
                            );
                        }

                    } catch (Exception e) {

                        System.out.println(
                                "5. 게시글 상단 고정 : FAIL"
                        );

                        System.out.println(
                                "   └ " + e.getMessage()
                        );
                    }


                    /*
                     * ============================================
                     * 8. 게시글 상단 고정 해제
                     *
                     * 기대 결과
                     * UPDATE 1건
                     * IS_PINNED = 'N'
                     * ============================================
                     */
                    try {

                        int result =
                                adminDAO.updatePostPin(
                                        conn,
                                        testPostId,
                                        false
                                );

                        String isPinned =
                                getPostPinStatus(
                                        conn,
                                        testPostId
                                );

                        if (result == 1
                                && "N".equals(isPinned)) {

                            System.out.println(
                                    "6. 게시글 고정 해제 : PASS"
                            );

                        } else {

                            System.out.println(
                                    "6. 게시글 고정 해제 : FAIL"
                            );

                            System.out.println(
                                    "   └ UPDATE 결과 : "
                                            + result
                            );

                            System.out.println(
                                    "   └ IS_PINNED : "
                                            + isPinned
                            );
                        }

                    } catch (Exception e) {

                        System.out.println(
                                "6. 게시글 고정 해제 : FAIL"
                        );

                        System.out.println(
                                "   └ " + e.getMessage()
                        );
                    }
                }


                /*
                 * ============================================
                 * 9. 존재하지 않는 게시글 고정
                 *
                 * 기대 결과 : 0건 수정
                 * ============================================
                 */
                try {

                    int result =
                            adminDAO.updatePostPin(
                                    conn,
                                    notExistsPostId,
                                    true
                            );

                    if (result == 0) {

                        System.out.println(
                                "7. 없는 게시글 고정 : PASS"
                        );

                    } else {

                        System.out.println(
                                "7. 없는 게시글 고정 : FAIL"
                        );

                        System.out.println(
                                "   └ UPDATE 결과 : "
                                        + result
                        );
                    }

                } catch (Exception e) {

                    System.out.println(
                            "7. 없는 게시글 고정 : FAIL"
                    );

                    System.out.println(
                            "   └ " + e.getMessage()
                    );
                }


            } finally {

                /*
                 * 테스트에서 회원 잠금이나 게시글 고정을
                 * 실제로 수행했더라도 DB에 남기지 않습니다.
                 */
                conn.rollback();

                System.out.println();
                System.out.println(
                        "테스트 DB 변경사항 ROLLBACK 완료"
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "DB 연결 또는 전체 검증 중 오류 발생"
            );

            e.printStackTrace();
        }

        System.out.println(
                "===== AdminDAO DB 연동 검증 종료 ====="
        );
    }


    /*
     * ============================================
     * 테스트에 사용할 실제 회원 조회
     * ============================================
     */
    private static Long findTestMemberId(
            Connection conn
    ) throws Exception {

        String sql = """
            SELECT member_id
            FROM MEMBER
            WHERE role = 'USER'
              AND ROWNUM = 1
            """;

        try (
                PreparedStatement pstmt =
                        conn.prepareStatement(sql);

                ResultSet rs =
                        pstmt.executeQuery()
        ) {

            if (rs.next()) {
                return rs.getLong("member_id");
            }
        }

        return null;
    }


    /*
     * ============================================
     * 회원 잠금 상태 조회
     * ============================================
     */
    private static String getMemberLockStatus(
            Connection conn,
            long memberId
    ) throws Exception {

        String sql = """
                SELECT is_locked
                FROM MEMBER
                WHERE member_id = ?
                """;

        try (PreparedStatement pstmt =
                     conn.prepareStatement(sql)) {

            pstmt.setLong(
                    1,
                    memberId
            );

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
     * ============================================
     * 회원 로그인 실패 횟수 조회
     * ============================================
     */
    private static int getMemberFailCount(
            Connection conn,
            long memberId
    ) throws Exception {

        String sql = """
                SELECT fail_count
                FROM MEMBER
                WHERE member_id = ?
                """;

        try (PreparedStatement pstmt =
                     conn.prepareStatement(sql)) {

            pstmt.setLong(
                    1,
                    memberId
            );

            try (ResultSet rs =
                         pstmt.executeQuery()) {

                if (rs.next()) {

                    return rs.getInt(
                            "fail_count"
                    );
                }
            }
        }

        return -1;
    }


    /*
     * ============================================
     * 테스트용 ACTIVE 게시글 조회
     * ============================================
     */
    private static Long findActivePostId(
            Connection conn
    ) throws Exception {

        String sql = """
                SELECT post_id
                FROM POST
                WHERE status = 'ACTIVE'
                  AND ROWNUM = 1
                """;

        try (
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
     * ============================================
     * 게시글 고정 상태 조회
     * ============================================
     */
    private static String getPostPinStatus(
            Connection conn,
            long postId
    ) throws Exception {

        String sql = """
                SELECT is_pinned
                FROM POST
                WHERE post_id = ?
                """;

        try (PreparedStatement pstmt =
                     conn.prepareStatement(sql)) {

            pstmt.setLong(
                    1,
                    postId
            );

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
}