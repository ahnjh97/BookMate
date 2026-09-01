package dao;

import dto.bookrequest.BookRequestDTO;
import util.BookImageUrlUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class BookRequestDAO {
    public boolean existsByIsbn(Connection connection, String isbn) throws SQLException {
        String sql = "SELECT 1 FROM BOOK WHERE isbn=? UNION ALL SELECT 1 FROM BOOK_REQUEST WHERE isbn=? FETCH FIRST 1 ROW ONLY";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, isbn);
            statement.setString(2, isbn);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public long insert(Connection connection, long memberId, String isbn, String title,
                       String authorName, String genre, String publisher, LocalDate publishedDate,
                       String description, String imageUrl, String sourceUrl) throws SQLException {
        String sql = """
                INSERT INTO BOOK_REQUEST(request_id,member_id,isbn,title,author_name,genre,publisher,
                                         published_date,description,image_url,source_url)
                VALUES(SEQ_BOOK_REQUEST.NEXTVAL,?,?,?,?,?,?,?, ?,?,?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, new String[]{"request_id"})) {
            statement.setLong(1, memberId);
            statement.setString(2, isbn);
            statement.setString(3, title);
            statement.setString(4, authorName);
            statement.setString(5, genre);
            statement.setString(6, publisher);
            statement.setDate(7, Date.valueOf(publishedDate));
            statement.setString(8, description);
            statement.setString(9, imageUrl);
            statement.setString(10, sourceUrl);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new SQLException("신청 번호를 가져오지 못했습니다.");
        }
    }

    public List<BookRequestDTO> selectAll(Connection connection) throws SQLException {
        String sql = """
                SELECT R.request_id,R.isbn,R.title,R.author_name,R.genre,R.publisher,R.published_date,
                       R.description,R.image_url,R.source_url,R.status,R.reject_reason,R.requested_at,M.nickname
                  FROM BOOK_REQUEST R JOIN MEMBER M ON M.member_id=R.member_id
                 ORDER BY CASE R.status WHEN 'PENDING' THEN 0 ELSE 1 END,R.requested_at DESC
                """;
        List<BookRequestDTO> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(BookRequestDTO.builder()
                        .requestId(rs.getLong("request_id")).isbn(rs.getString("isbn"))
                        .title(rs.getString("title")).authorName(rs.getString("author_name"))
                        .genre(rs.getString("genre")).publisher(rs.getString("publisher"))
                        .publishedDate(rs.getDate("published_date")).description(rs.getString("description"))
                        .imageUrl(BookImageUrlUtil.thumbnail(rs.getString("image_url")))
                        .sourceUrl(rs.getString("source_url")).status(rs.getString("status"))
                        .rejectReason(rs.getString("reject_reason")).requestedAt(rs.getTimestamp("requested_at"))
                        .requesterNickname(rs.getString("nickname")).build());
            }
        }
        return result;
    }

    public Map<String, String> selectPendingForUpdate(Connection connection, long requestId)
            throws SQLException {
        String sql = "SELECT isbn,title,author_name,genre,publisher,TO_CHAR(published_date,'YYYY-MM-DD') published_date,description,image_url,source_url FROM BOOK_REQUEST WHERE request_id=? AND status='PENDING' FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requestId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NoSuchElementException("대기 중인 책 등록 신청을 찾을 수 없습니다.");
                Map<String, String> row = new HashMap<>();
                for (String key : List.of("isbn", "title", "author_name", "genre", "publisher",
                        "published_date", "description", "image_url", "source_url")) {
                    row.put(key, rs.getString(key));
                }
                return row;
            }
        }
    }

    public void insertApprovedBook(Connection connection, Map<String, String> request) throws SQLException {
        long authorId = findOrInsertAuthor(connection, request.get("author_name"));
        String sql = "INSERT INTO BOOK(book_id,author_id,isbn,title,genre,publisher,published_date,description,image_url,source_url,status) VALUES(SEQ_BOOK.NEXTVAL,?,?,?,?,?,?,?,?,?, 'APPROVED')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, authorId);
            statement.setString(2, request.get("isbn"));
            statement.setString(3, request.get("title"));
            statement.setString(4, request.get("genre"));
            statement.setString(5, request.get("publisher"));
            statement.setDate(6, Date.valueOf(request.get("published_date")));
            statement.setString(7, request.get("description"));
            statement.setString(8, request.get("image_url"));
            statement.setString(9, request.get("source_url"));
            statement.executeUpdate();
        }
    }

    public int updateReview(Connection connection, long requestId, long adminId,
                            boolean approved, String reason) throws SQLException {
        String sql = "UPDATE BOOK_REQUEST SET status=?,reject_reason=?,reviewed_by=?,reviewed_at=SYSDATE WHERE request_id=? AND status='PENDING'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, approved ? "APPROVED" : "REJECTED");
            statement.setString(2, approved ? null : reason);
            statement.setLong(3, adminId);
            statement.setLong(4, requestId);
            return statement.executeUpdate();
        }
    }

    private long findOrInsertAuthor(Connection connection, String authorName) throws SQLException {
        try (PreparedStatement find = connection.prepareStatement(
                "SELECT author_id FROM AUTHOR WHERE LOWER(author_name)=LOWER(?) FETCH FIRST 1 ROW ONLY")) {
            find.setString(1, authorName);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO AUTHOR(author_id,author_name) VALUES(SEQ_AUTHOR.NEXTVAL,?)",
                new String[]{"author_id"})) {
            insert.setString(1, authorName);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("작가 번호를 가져오지 못했습니다.");
    }
}
