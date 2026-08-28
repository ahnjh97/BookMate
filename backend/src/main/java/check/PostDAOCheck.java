package check;

import dao.PostDAO;
import dto.PostDTO;
import util.DBUtil;

import java.sql.Connection;
import java.util.List;

public class PostDAOCheck {
    public static void main(String[] args) {
        PostDAO postDAO = new PostDAO();
        // 실제로 존재하지 않을 가능성이 높은 게시글 번호
        long testPostId = 999999L;
        System.out.println("===== PostDAO DB 연동 검증 시작 =====");
        try (Connection conn = DBUtil.getConnection()) {
            // 혹시라도 DB 변경이 발생하면 마지막에 되돌리기
            conn.setAutoCommit(false);
            try {
                // ============================================
                // 1. 게시글 전체 목록 조회
                // ============================================
                try {
                    List<PostDTO> posts =
                            postDAO.selectPostList(conn);

                    System.out.println("1. 게시글 목록 조회 : PASS");
                    System.out.println("   현재 ACTIVE 게시글 수 : "+ posts.size());
                } catch (Exception e) {
                    System.out.println("1. 게시글 목록 조회 : FAIL");
                    System.out.println("   └ " + e.getMessage());
                }

                // ============================================
                // 2. 존재하지 않는 게시글 상세 조회
                // 기대 결과 : null
                // ============================================
                try {
                    PostDTO post =
                            postDAO.selectPostById(
                                    conn,
                                    testPostId
                            );

                    if (post == null) {
                        System.out.println("2. 없는 게시글 상세 조회 : PASS");
                    } else {
                        System.out.println("2. 없는 게시글 상세 조회 : FAIL");
                    }

                } catch (Exception e) {

                    System.out.println("2. 없는 게시글 상세 조회 : FAIL");

                    System.out.println("   └ " + e.getMessage());
                }


                // ============================================
                // 3. 게시글 존재 여부 확인
                // 기대 결과 : false
                // ============================================
                try {
                    boolean exists =
                            postDAO.existsPost(
                                    conn,
                                    testPostId
                            );

                    if (!exists) {
                        System.out.println("3. 게시글 존재 여부 확인 : PASS");
                    } else {
                        System.out.println("3. 게시글 존재 여부 확인 : FAIL");
                    }

                } catch (Exception e) {

                    System.out.println("3. 게시글 존재 여부 확인 : FAIL");

                    System.out.println("   └ " + e.getMessage());
                }


                // ============================================
                // 4. 게시글 작성자 번호 조회
                // 기대 결과 : null
                // ============================================
                try {
                    Long writerId =
                            postDAO.selectWriterId(
                                    conn,
                                    testPostId
                            );

                    if (writerId == null) {
                        System.out.println("4. 작성자 번호 조회 : PASS");
                    } else {
                        System.out.println("4. 작성자 번호 조회 : FAIL");
                    }

                } catch (Exception e) {
                    System.out.println("4. 작성자 번호 조회 : FAIL");
                    System.out.println("   └ " + e.getMessage());
                }

                // ============================================
                // 5. 존재하지 않는 게시글 수정
                // 기대 결과 : 0건 수정
                // ============================================
                try {
                    PostDTO post = new PostDTO();

                    post.setPostId(testPostId);
                    post.setCategory("TEST");
                    post.setTitle("테스트 제목");
                    post.setContent("테스트 내용");

                    int result =
                            postDAO.updatePost(
                                    conn,
                                    post
                            );

                    if (result == 0) {
                        System.out.println("5. 없는 게시글 수정 : PASS");
                    } else {
                        System.out.println("5. 없는 게시글 수정 : FAIL");
                    }

                } catch (Exception e) {

                    System.out.println("5. 없는 게시글 수정 : FAIL");

                    System.out.println("   └ " + e.getMessage());
                }


                // ============================================
                // 6. 작성자 삭제
                // 기대 결과 : 0건 수정
                // ============================================
                try {
                    int result =
                            postDAO.deletePostByWriter(
                                    conn,
                                    testPostId
                            );

                    if (result == 0) {
                        System.out.println("6. 작성자 게시글 삭제 : PASS");
                    } else {
                        System.out.println("6. 작성자 게시글 삭제 : FAIL");
                    }

                } catch (Exception e) {

                    System.out.println("6. 작성자 게시글 삭제 : FAIL");

                    System.out.println("   └ " + e.getMessage());
                }


                // ============================================
                // 7. 관리자 삭제
                // 기대 결과 : 0건 수정
                // ============================================
                try {
                    int result =
                            postDAO.deletePostByAdmin(
                                    conn,
                                    testPostId
                            );

                    if (result == 0) {
                        System.out.println(
                                "7. 관리자 게시글 삭제 : PASS"
                        );
                    } else {
                        System.out.println(
                                "7. 관리자 게시글 삭제 : FAIL"
                        );
                    }

                } catch (Exception e) {

                    System.out.println(
                            "7. 관리자 게시글 삭제 : FAIL"
                    );

                    System.out.println(
                            "   └ " + e.getMessage()
                    );
                }


                // ============================================
                // 8. 조회수 증가
                // 기대 결과 : 0건 수정
                // ============================================
                try {
                    int result =
                            postDAO.increaseViewCount(
                                    conn,
                                    testPostId
                            );

                    if (result == 0) {
                        System.out.println(
                                "8. 조회수 증가 : PASS"
                        );
                    } else {
                        System.out.println(
                                "8. 조회수 증가 : FAIL"
                        );
                    }

                } catch (Exception e) {

                    System.out.println(
                            "8. 조회수 증가 : FAIL"
                    );

                    System.out.println(
                            "   └ " + e.getMessage()
                    );
                }

                // ============================================
// 9. ACTIVE 게시글의 IS_PINNED 조회 확인
// 기대 결과 : Y 또는 N
// ============================================
                try {

                    List<PostDTO> posts =
                            postDAO.selectPostList(conn);

                    if (posts.isEmpty()) {

                        System.out.println(
                                "9. 게시글 IS_PINNED 조회 : SKIP"
                        );

                        System.out.println(
                                "   └ ACTIVE 게시글이 없습니다."
                        );

                    } else {

                        PostDTO post =
                                posts.get(0);

                        String isPinned =
                                post.getIsPinned();

                        if ("Y".equals(isPinned)
                                || "N".equals(isPinned)) {

                            System.out.println(
                                    "9. 게시글 IS_PINNED 조회 : PASS"
                            );

                            System.out.println(
                                    "   POST_ID : "
                                            + post.getPostId()
                            );

                            System.out.println(
                                    "   IS_PINNED : "
                                            + isPinned
                            );

                        } else {

                            System.out.println(
                                    "9. 게시글 IS_PINNED 조회 : FAIL"
                            );

                            System.out.println(
                                    "   └ IS_PINNED 값 : "
                                            + isPinned
                            );
                        }
                    }

                } catch (Exception e) {

                    System.out.println(
                            "9. 게시글 IS_PINNED 조회 : FAIL"
                    );

                    System.out.println(
                            "   └ " + e.getMessage()
                    );
                }


            } finally {

                // 혹시 발생한 모든 변경사항 되돌리기
                conn.rollback();

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
                "===== PostDAO DB 연동 검증 종료 ====="
        );
    }
}