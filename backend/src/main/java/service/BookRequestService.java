package service;

import util.DBUtil;
import util.BookImageUrlUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class BookRequestService {
    public boolean isbnExists(String rawIsbn) {
        String isbn = normalizeIsbn(rawIsbn);
        String sql = "SELECT 1 FROM BOOK WHERE isbn=? UNION ALL SELECT 1 FROM BOOK_REQUEST WHERE isbn=? FETCH FIRST 1 ROW ONLY";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, isbn);
            statement.setString(2, isbn);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
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

        String sql = """
                INSERT INTO BOOK_REQUEST(request_id,member_id,isbn,title,author_name,genre,publisher,
                                         published_date,description,image_url,source_url)
                VALUES(SEQ_BOOK_REQUEST.NEXTVAL,?,?,?,?,?,?,?, ?,?,?)
                """;
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, new String[]{"request_id"})) {
            statement.setLong(1, memberId);
            statement.setString(2, isbn);
            statement.setString(3, safeTitle);
            statement.setString(4, safeAuthor);
            statement.setString(5, safeGenre);
            statement.setString(6, safePublisher);
            statement.setDate(7, java.sql.Date.valueOf(safeDate));
            statement.setString(8, safeDescription);
            statement.setString(9, safeImageUrl);
            statement.setString(10, safeSourceUrl);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new SQLException("신청 번호를 가져오지 못했습니다.");
        } catch (SQLIntegrityConstraintViolationException exception) {
            throw new IllegalArgumentException("이미 등록되었거나 검토 중인 ISBN입니다.");
        } catch (SQLException exception) {
            throw new RuntimeException("책 등록 신청을 저장하지 못했습니다.", exception);
        }
    }

    public List<Map<String, Object>> findRequests() {
        String sql = """
                SELECT R.request_id,R.isbn,R.title,R.author_name,R.genre,R.publisher,R.published_date,
                       R.description,R.image_url,R.source_url,R.status,R.reject_reason,R.requested_at,M.nickname
                  FROM BOOK_REQUEST R JOIN MEMBER M ON M.member_id=R.member_id
                 ORDER BY CASE R.status WHEN 'PENDING' THEN 0 ELSE 1 END,R.requested_at DESC
                """;
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("requestId", rs.getLong("request_id"));
                row.put("isbn", rs.getString("isbn"));
                row.put("title", rs.getString("title"));
                row.put("authorName", rs.getString("author_name"));
                row.put("genre", rs.getString("genre"));
                row.put("publisher", rs.getString("publisher"));
                row.put("publishedDate", rs.getDate("published_date"));
                row.put("description", rs.getString("description"));
                row.put("imageUrl", BookImageUrlUtil.thumbnail(rs.getString("image_url")));
                row.put("sourceUrl", rs.getString("source_url"));
                row.put("status", rs.getString("status"));
                row.put("rejectReason", rs.getString("reject_reason"));
                row.put("requestedAt", rs.getTimestamp("requested_at"));
                row.put("requesterNickname", rs.getString("nickname"));
                result.add(row);
            }
            return result;
        } catch (SQLException exception) {
            throw new RuntimeException("책 등록 신청을 불러오지 못했습니다.", exception);
        }
    }

    public void review(long requestId, long adminId, boolean approved, String reason) {
        if (requestId <= 0 || adminId <= 0) throw new IllegalArgumentException("올바른 신청 정보가 필요합니다.");
        if (!approved && (reason == null || reason.isBlank())) throw new IllegalArgumentException("반려 사유를 입력해 주세요.");
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Map<String, String> request = lockPendingRequest(connection, requestId);
                if (approved) approve(connection, request);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE BOOK_REQUEST SET status=?,reject_reason=?,reviewed_by=?,reviewed_at=SYSDATE WHERE request_id=? AND status='PENDING'")) {
                    statement.setString(1, approved ? "APPROVED" : "REJECTED");
                    statement.setString(2, approved ? null : reason.trim());
                    statement.setLong(3, adminId);
                    statement.setLong(4, requestId);
                    if (statement.executeUpdate() != 1) throw new IllegalStateException("이미 처리된 신청입니다.");
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

    private Map<String, String> lockPendingRequest(Connection connection, long requestId) throws SQLException {
        String sql = "SELECT isbn,title,author_name,genre,publisher,TO_CHAR(published_date,'YYYY-MM-DD') published_date,description,image_url,source_url FROM BOOK_REQUEST WHERE request_id=? AND status='PENDING' FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requestId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NoSuchElementException("대기 중인 책 등록 신청을 찾을 수 없습니다.");
                Map<String, String> row = new HashMap<>();
                for (String key : List.of("isbn","title","author_name","genre","publisher","published_date","description","image_url","source_url")) row.put(key, rs.getString(key));
                return row;
            }
        }
    }

    private void approve(Connection connection, Map<String, String> request) throws SQLException {
        long authorId;
        try (PreparedStatement find = connection.prepareStatement("SELECT author_id FROM AUTHOR WHERE LOWER(author_name)=LOWER(?) FETCH FIRST 1 ROW ONLY")) {
            find.setString(1, request.get("author_name"));
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) authorId = rs.getLong(1);
                else {
                    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO AUTHOR(author_id,author_name) VALUES(SEQ_AUTHOR.NEXTVAL,?)", new String[]{"author_id"})) {
                        insert.setString(1, request.get("author_name"));
                        insert.executeUpdate();
                        try (ResultSet keys = insert.getGeneratedKeys()) { keys.next(); authorId = keys.getLong(1); }
                    }
                }
            }
        }
        String sql = "INSERT INTO BOOK(book_id,author_id,isbn,title,genre,publisher,published_date,description,image_url,source_url,status) VALUES(SEQ_BOOK.NEXTVAL,?,?,?,?,?,?,?,?,?, 'APPROVED')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, authorId);
            statement.setString(2, request.get("isbn"));
            statement.setString(3, request.get("title"));
            statement.setString(4, request.get("genre"));
            statement.setString(5, request.get("publisher"));
            statement.setDate(6, java.sql.Date.valueOf(request.get("published_date")));
            statement.setString(7, request.get("description"));
            statement.setString(8, request.get("image_url"));
            statement.setString(9, request.get("source_url"));
            statement.executeUpdate();
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
        if (checkDigit != isbn.charAt(12) - '0') {
            throw new IllegalArgumentException("ISBN-13 검증 숫자가 올바르지 않습니다.");
        }
        return isbn;
    }

    private String required(String value, String label, int max) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + "을(를) 입력해 주세요.");
        if (normalized.length() > max) throw new IllegalArgumentException(label + "은(는) " + max + "자 이하여야 합니다.");
        return normalized;
    }
}
