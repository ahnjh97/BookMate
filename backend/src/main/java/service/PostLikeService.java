package service;

import dao.PostDAO;
import dao.PostLikeDAO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.NoSuchElementException;

public class PostLikeService {
    private final PostLikeDAO postLikeDAO;
    private final PostDAO postDAO;

    public PostLikeService() {
        postLikeDAO = new PostLikeDAO();
        postDAO = new PostDAO();
    }

    /* 게시글 좋아요 등록 또는 취소 */
    public LikeResult togglePostLike(long postId, long loginMemberId) {
        validatePostId(postId);
        validateMemberId(loginMemberId);
        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (!postDAO.existsPost(conn, postId)) {
                throw new NoSuchElementException("존재하지 않거나 조회할 수 없는 게시글입니다.");
            }

            boolean liked = postLikeDAO.existsPostLike(conn, postId, loginMemberId);

            if (liked) {
                postLikeDAO.deletePostLike(conn, postId, loginMemberId);
            } else {
                postLikeDAO.insertPostLike(conn, postId, loginMemberId);
            }

            int likeCount = postLikeDAO.countPostLikes(conn, postId);
            conn.commit();

            return new LikeResult(!liked, likeCount);
        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("게시글 좋아요 처리에 실패했습니다.", e);
        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } finally {
            close(conn);
        }
    }

    /* 게시글 좋아요 개수 조회 */
    public int getPostLikeCount(long postId) {
        validatePostId(postId);

        try (Connection conn = DBUtil.getConnection()) {
            return postLikeDAO.countPostLikes(conn, postId);
        } catch (SQLException e) {
            throw new RuntimeException("게시글 좋아요 수를 불러오지 못했습니다.", e);
        }
    }

    /* 회원의 게시글 좋아요 여부 조회 */
    public boolean isPostLiked(long postId, long loginMemberId) {
        validatePostId(postId);
        validateMemberId(loginMemberId);

        try (Connection conn = DBUtil.getConnection()) {
            return postLikeDAO.existsPostLike(conn, postId, loginMemberId);
        } catch (SQLException e) {
            throw new RuntimeException("게시글 좋아요 상태를 불러오지 못했습니다.", e);
        }
    }

    /* 게시글 번호 검증 */
    private void validatePostId(long postId) {
        if (postId <= 0) {
            throw new IllegalArgumentException("올바르지 않은 게시글 번호입니다.");
        }
    }

    /* 회원 번호 검증 */
    private void validateMemberId(long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("로그인 회원 정보가 올바르지 않습니다.");
        }
    }

    /* 트랜잭션 롤백 */
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

    /* DB 연결 종료 */
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

    /* 좋아요 처리 결과 */
    public record LikeResult(boolean liked, int likeCount) {
    }
}