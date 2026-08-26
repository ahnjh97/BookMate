package service;

import dao.PostDAO;
import dto.PostDTO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class PostService {

    private final PostDAO postDAO;

    public PostService() {
        this.postDAO = new PostDAO();
    }

    /*
     * 게시글 목록 조회
     */
    public List<PostDTO> getPostList() {
        try (Connection conn = DBUtil.getConnection()) {
            return postDAO.selectPostList(conn);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "게시글 목록을 불러오지 못했습니다.",
                    e
            );
        }
    }

    /*
     * 게시글 상세 조회
     * 게시글이 존재하면 조회수를 1 증가시킨 뒤 조회합니다.
     */
    public PostDTO getPostDetail(long postId) {
        validatePostId(postId);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            int result =
                    postDAO.increaseViewCount(conn, postId);

            if (result == 0) {
                rollback(conn);
                return null;
            }

            PostDTO post =
                    postDAO.selectPostById(conn, postId);

            conn.commit();

            return post;

        } catch (SQLException e) {
            rollback(conn);

            throw new RuntimeException(
                    "게시글을 불러오지 못했습니다.",
                    e
            );

        } finally {
            close(conn);
        }
    }

    /*
     * 게시글 등록
     *
     * 등록된 게시글 번호를 반환합니다.
     */
    public long createPost(PostDTO post) {
        validateCreatePost(post);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            long postId =
                    postDAO.insertPost(conn, post);

            conn.commit();

            return postId;

        } catch (SQLException e) {
            rollback(conn);

            throw new RuntimeException(
                    "게시글 등록에 실패했습니다.",
                    e
            );

        } finally {
            close(conn);
        }
    }

    /*
     * 게시글 수정
     *
     * loginMemberId는 현재 로그인한 회원 번호입니다.
     */
    public boolean updatePost(
            PostDTO post,
            long loginMemberId
    ) {
        validateUpdatePost(post);
        validateMemberId(loginMemberId);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            checkWriterPermission(
                    conn,
                    post.getPostId(),
                    loginMemberId
            );

            int result =
                    postDAO.updatePost(conn, post);

            if (result == 0) {
                rollback(conn);
                return false;
            }

            conn.commit();

            return true;

        } catch (SQLException e) {
            rollback(conn);

            throw new RuntimeException(
                    "게시글 수정에 실패했습니다.",
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
     * 작성자가 자신의 게시글 삭제
     */
    public boolean deletePostByWriter(
            long postId,
            long loginMemberId
    ) {
        validatePostId(postId);
        validateMemberId(loginMemberId);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            checkWriterPermission(
                    conn,
                    postId,
                    loginMemberId
            );

            int result =
                    postDAO.deletePostByWriter(
                            conn,
                            postId
                    );

            if (result == 0) {
                rollback(conn);
                return false;
            }

            conn.commit();

            return true;

        } catch (SQLException e) {
            rollback(conn);

            throw new RuntimeException(
                    "게시글 삭제에 실패했습니다.",
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
     * 관리자 게시글 삭제
     */
    public boolean deletePostByAdmin(
            long postId,
            String loginMemberRole
    ) {
        validatePostId(postId);
        validateAdminRole(loginMemberRole);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            int result =
                    postDAO.deletePostByAdmin(
                            conn,
                            postId
                    );

            if (result == 0) {
                rollback(conn);
                return false;
            }

            conn.commit();

            return true;

        } catch (SQLException e) {
            rollback(conn);

            throw new RuntimeException(
                    "관리자 게시글 삭제에 실패했습니다.",
                    e
            );

        } finally {
            close(conn);
        }
    }

    /*
     * 관리자 권한 확인
     */
    private void validateAdminRole(String role) {
        if (role == null || !"ADMIN".equals(role)) {
            throw new SecurityException(
                    "관리자만 이용할 수 있는 기능입니다."
            );
        }
    }
    /*
     * 작성자 권한 확인
     */
    private void checkWriterPermission(
            Connection conn,
            long postId,
            long loginMemberId
    ) throws SQLException {

        Long writerId =
                postDAO.selectWriterId(conn, postId);

        if (writerId == null) {
            throw new NoSuchElementException(
                    "존재하지 않거나 삭제된 게시글입니다."
            );
        }

        if (writerId != loginMemberId) {
            throw new SecurityException(
                    "게시글을 수정하거나 삭제할 권한이 없습니다."
            );
        }
    }

    /*
     * 게시글 등록값 검증
     */
    private void validateCreatePost(PostDTO post) {
        if (post == null) {
            throw new IllegalArgumentException(
                    "게시글 정보가 없습니다."
            );
        }

        validateMemberId(post.getMemberId());
        validatePostContent(post);
    }

    /*
     * 게시글 수정값 검증
     */
    private void validateUpdatePost(PostDTO post) {
        if (post == null) {
            throw new IllegalArgumentException(
                    "게시글 정보가 없습니다."
            );
        }

        validatePostId(post.getPostId());
        validatePostContent(post);
    }

    /*
     * 제목·내용·카테고리 검증
     */
    private void validatePostContent(PostDTO post) {
        String title = post.getTitle();
        String content = post.getContent();
        String category = post.getCategory();

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "제목을 입력해주세요."
            );
        }

        if (title.trim().length() > 200) {
            throw new IllegalArgumentException(
                    "제목은 200자 이하로 입력해주세요."
            );
        }

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "내용을 입력해주세요."
            );
        }

        if (content.trim().length() > 4000) {
            throw new IllegalArgumentException(
                    "내용은 4000자 이하로 입력해주세요."
            );
        }

        if (!isValidCategory(category)) {
            throw new IllegalArgumentException(
                    "올바르지 않은 게시글 카테고리입니다."
            );
        }

        post.setTitle(title.trim());
        post.setContent(content.trim());
    }

    /*
     * 카테고리 검증
     */
    private boolean isValidCategory(String category) {
        if (category == null) {
            return false;
        }

        return switch (category) {
            case "NOTICE",
                 "FREE",
                 "RECOMMEND",
                 "REVIEW",
                 "TIER",
                 "AUTHOR" -> true;

            default -> false;
        };
    }

    private void validatePostId(long postId) {
        if (postId <= 0) {
            throw new IllegalArgumentException(
                    "올바르지 않은 게시글 번호입니다."
            );
        }
    }

    private void validateMemberId(long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException(
                    "로그인 회원 정보가 올바르지 않습니다."
            );
        }
    }

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