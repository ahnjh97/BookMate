package service;

import dao.BookRequestDAO;
import dto.bookrequest.BookRequestDTO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class BookRequestService {
    private final BookRequestDAO bookRequestDAO = new BookRequestDAO();

    public boolean isbnExists(String rawIsbn) {
        String isbn = normalizeIsbn(rawIsbn);
        try (Connection connection = DBUtil.getConnection()) {
            return bookRequestDAO.existsByIsbn(connection, isbn);
        } catch (SQLException exception) {
            throw new RuntimeException("ISBN 중복 여부를 확인하지 못했습니다.", exception);
        }
    }

    public long request(long memberId, String rawIsbn, String title, String authorName, String genre,
                        String publisher, String publishedDate, String description, String imageUrl,
                        String sourceUrl) {
        if (memberId <= 0) throw new IllegalArgumentException("로그인이 필요한 기능입니다.");
        String isbn = normalizeIsbn(rawIsbn);
        String safeTitle = required(title, "책 제목", 200);
        String safeAuthor = required(authorName, "작가명", 100);
        String safeGenre = required(genre, "장르", 50);
        String safePublisher = required(publisher, "출판사", 100);
        String safeDescription = required(description, "책 설명", 1000);
        String safeImageUrl = required(imageUrl, "표지 이미지", 500);
        String safeSourceUrl = required(sourceUrl, "도서 정보 출처", 500);
        LocalDate safeDate;
        try {
            safeDate = LocalDate.parse(required(publishedDate, "출간일", 10));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("올바른 출간일을 입력해 주세요.");
        }
        if (isbnExists(isbn)) throw new IllegalArgumentException("이미 등록되었거나 검토 중인 ISBN입니다.");

        try (Connection connection = DBUtil.getConnection()) {
            return bookRequestDAO.insert(connection, memberId, isbn, safeTitle, safeAuthor,
                    safeGenre, safePublisher, safeDate, safeDescription, safeImageUrl, safeSourceUrl);
        } catch (SQLIntegrityConstraintViolationException exception) {
            throw new IllegalArgumentException("이미 등록되었거나 검토 중인 ISBN입니다.");
        } catch (SQLException exception) {
            throw new RuntimeException("책 등록 신청을 저장하지 못했습니다.", exception);
        }
    }

    public List<BookRequestDTO> findRequests() {
        try (Connection connection = DBUtil.getConnection()) {
            return bookRequestDAO.selectAll(connection);
        } catch (SQLException exception) {
            throw new RuntimeException("책 등록 신청을 불러오지 못했습니다.", exception);
        }
    }

    public void review(long requestId, long adminId, boolean approved, String reason) {
        if (requestId <= 0 || adminId <= 0) throw new IllegalArgumentException("올바른 신청 정보가 필요합니다.");
        if (!approved && (reason == null || reason.isBlank())) throw new IllegalArgumentException("반려 사유를 입력해 주세요.");
        String rejectReason = approved ? null : reason.trim();
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Map<String, String> request = bookRequestDAO.selectPendingForUpdate(connection, requestId);
                if (approved) bookRequestDAO.insertApprovedBook(connection, request);
                if (bookRequestDAO.updateReview(connection, requestId, adminId, approved, rejectReason) != 1) {
                    throw new IllegalStateException("이미 처리된 신청입니다.");
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof RuntimeException runtime) throw runtime;
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("책 등록 신청 검토 결과를 저장하지 못했습니다.", exception);
        }
    }

    public static String normalizeIsbn(String value) {
        String isbn = value == null ? "" : value.replaceAll("[^0-9]", "");
        if (isbn.length() != 13) throw new IllegalArgumentException("ISBN-13 숫자 13자리를 입력해 주세요.");
        int weightedSum = 0;
        for (int index = 0; index < 12; index++) {
            weightedSum += (isbn.charAt(index) - '0') * (index % 2 == 0 ? 1 : 3);
        }
        int checkDigit = (10 - weightedSum % 10) % 10;
        if (checkDigit != isbn.charAt(12) - '0') throw new IllegalArgumentException("ISBN-13 검증 숫자가 올바르지 않습니다.");
        return isbn;
    }

    private String required(String value, String label, int max) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + "을(를) 입력해 주세요.");
        if (normalized.length() > max) throw new IllegalArgumentException(label + "은(는) " + max + "자 이하여야 합니다.");
        return normalized;
    }
}
