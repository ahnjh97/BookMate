package service;

import dao.RatingDAO;
import dto.RatingDTO;
import exception.DuplicateRatingException;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.NoSuchElementException;

public class RatingService {
    private final RatingDAO ratingDAO = new RatingDAO();

    public RatingDTO findMyRating(long bookId, long loginMemberId) {
        validateBookAndMember(bookId, loginMemberId);

        try (Connection connection = DBUtil.getConnection()) {
            return ratingDAO.selectRatingByBookAndMember(connection, bookId, loginMemberId);
        } catch (SQLException exception) {
            throw new RuntimeException("내 평점을 조회하는 중 오류가 발생했습니다.", exception);
        }
    }

    public long createRating(RatingDTO rating, long loginMemberId) {
        validateRating(rating, loginMemberId);
        rating.setMemberId(loginMemberId);
        rating.setCommentText(normalizeComment(rating.getCommentText()));

        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (!ratingDAO.existsApprovedBook(connection, rating.getBookId())) {
                    throw new NoSuchElementException("평가할 책을 찾을 수 없습니다.");
                }
                if (ratingDAO.existsRating(connection, rating.getBookId(), loginMemberId)) {
                    throw new DuplicateRatingException("이미 평가한 책입니다. 기존 평점을 수정해 주세요.");
                }

                long ratingId = ratingDAO.insertRating(connection, rating);
                connection.commit();
                return ratingId;
            } catch (RuntimeException exception) {
                rollback(connection);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection);
                if (exception.getErrorCode() == 1) {
                    throw new DuplicateRatingException("이미 평가한 책입니다. 기존 평점을 수정해 주세요.");
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("평점 등록 중 오류가 발생했습니다.", exception);
        }
    }

    public void updateRating(RatingDTO rating, long loginMemberId) {
        validateRating(rating, loginMemberId);
        if (rating.getRatingId() <= 0) {
            throw new IllegalArgumentException("올바른 평점 번호가 필요합니다.");
        }

        rating.setMemberId(loginMemberId);
        rating.setCommentText(normalizeComment(rating.getCommentText()));

        try (Connection connection = DBUtil.getConnection()) {
            if (ratingDAO.updateRating(connection, rating) == 0) {
                throw new NoSuchElementException("수정할 평점을 찾을 수 없습니다.");
            }
        } catch (SQLException exception) {
            throw new RuntimeException("평점 수정 중 오류가 발생했습니다.", exception);
        }
    }

    private void validateRating(RatingDTO rating, long loginMemberId) {
        if (loginMemberId <= 0) {
            throw new IllegalArgumentException("로그인 회원 정보가 올바르지 않습니다.");
        }
        if (rating == null) {
            throw new IllegalArgumentException("평점 정보가 없습니다.");
        }
        if (rating.getBookId() <= 0) {
            throw new IllegalArgumentException("올바른 책 번호가 필요합니다.");
        }
        if (rating.getScore() < 1 || rating.getScore() > 5) {
            throw new IllegalArgumentException("평점은 1점부터 5점까지 입력해 주세요.");
        }
        if (rating.getCommentText() != null && rating.getCommentText().trim().length() > 500) {
            throw new IllegalArgumentException("한줄평은 500자 이하로 입력해 주세요.");
        }
    }

    private void validateBookAndMember(long bookId, long loginMemberId) {
        if (bookId <= 0) {
            throw new IllegalArgumentException("올바른 책 번호가 필요합니다.");
        }
        if (loginMemberId <= 0) {
            throw new IllegalArgumentException("로그인 회원 정보가 올바르지 않습니다.");
        }
    }

    private String normalizeComment(String commentText) {
        if (commentText == null || commentText.isBlank()) {
            return null;
        }
        return commentText.trim();
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 원래 발생한 예외를 유지합니다.
        }
    }
}
