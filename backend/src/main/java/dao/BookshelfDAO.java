package dao;

import dto.bookshelf.BookshelfBookDTO;
import dto.bookshelf.BookshelfMemberDTO;
import dto.bookshelf.BookshelfRatingPageDTO;
import dto.bookshelf.BookshelfSummaryDTO;
import dto.bookshelf.BookshelfTierDTO;
import dto.bookshelf.BookshelfWorldcupDTO;
import util.BookImageUrlUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookshelfDAO {
    public BookshelfMemberDTO selectMember(Connection connection, long memberId) throws SQLException {
        String sql = "SELECT member_id,nickname FROM MEMBER WHERE member_id=? AND role='USER'"
                + " AND login_id<>'bookmate_system'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                return new BookshelfMemberDTO(
                        resultSet.getLong("member_id"), resultSet.getString("nickname"));
            }
        }
    }

    public BookshelfSummaryDTO selectCounts(Connection connection, long memberId) throws SQLException {
        String sql = """
                SELECT (SELECT COUNT(*) FROM RATING WHERE member_id=?) rating_count,
                       (SELECT NVL(AVG(score),0) FROM RATING WHERE member_id=?) rating_average,
                       (SELECT COUNT(*) FROM TIER_LIST WHERE member_id=?) tier_count,
                       (SELECT COUNT(*) FROM IDEAL_RUN WHERE member_id=?) worldcup_count
                  FROM DUAL
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 1; index <= 4; index++) statement.setLong(index, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return BookshelfSummaryDTO.builder()
                        .ratings(resultSet.getInt("rating_count"))
                        .ratingAverage(resultSet.getDouble("rating_average"))
                        .tierLists(resultSet.getInt("tier_count"))
                        .worldcups(resultSet.getInt("worldcup_count")).build();
            }
        }
    }

    public BookshelfRatingPageDTO selectRatingPage(Connection connection, long memberId, Integer score,
                                                 int page, int size) throws SQLException {
        int totalCount = countRatings(connection, memberId, score);
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) size));
        int safePage = Math.min(page, totalPages);
        String sql = """
                SELECT R.book_id,R.score,R.comment_text,B.title,B.image_url,A.author_name
                  FROM RATING R
                  JOIN BOOK B ON B.book_id=R.book_id
                  JOIN AUTHOR A ON A.author_id=B.author_id
                 WHERE R.member_id=? AND B.status='APPROVED'
                   AND R.comment_text IS NOT NULL AND TRIM(R.comment_text) IS NOT NULL
                   AND (? IS NULL OR R.score=?)
                 ORDER BY R.score DESC,B.title ASC,B.book_id ASC
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        List<BookshelfBookDTO> books = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindScore(statement, memberId, score);
            statement.setInt(4, (safePage - 1) * size);
            statement.setInt(5, size);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    books.add(BookshelfBookDTO.builder().bookId(resultSet.getLong("book_id"))
                            .score(resultSet.getInt("score")).comment(resultSet.getString("comment_text"))
                            .title(resultSet.getString("title"))
                            .imageUrl(BookImageUrlUtil.thumbnail(resultSet.getString("image_url")))
                            .authorName(resultSet.getString("author_name")).build());
                }
            }
        }
        return BookshelfRatingPageDTO.builder().books(books).page(safePage).pageSize(size)
                .totalCount(totalCount).totalPages(totalPages).build();
    }

    public List<BookshelfTierDTO> selectTierLists(Connection connection, long memberId)
            throws SQLException {
        String sql = """
                SELECT L.tier_list_id,L.template_id,L.title,T.title template_title,L.created_at
                  FROM TIER_LIST L JOIN TIER_TEMPLATE T ON T.template_id=L.template_id
                 WHERE L.member_id=? ORDER BY L.created_at DESC,L.tier_list_id DESC
                """;
        List<BookshelfTierDTO> lists = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    lists.add(BookshelfTierDTO.builder().tierListId(resultSet.getLong("tier_list_id"))
                            .templateId(resultSet.getLong("template_id")).title(resultSet.getString("title"))
                            .templateTitle(resultSet.getString("template_title")).build());
                }
            }
        }
        return lists;
    }

    public List<BookshelfWorldcupDTO> selectWorldcupResults(Connection connection, long memberId)
            throws SQLException {
        String sql = """
                SELECT R.run_id,T.title template_title,B.book_id,B.title winner_title,B.image_url
                  FROM IDEAL_RUN R
                  JOIN IDEAL_TEMPLATE T ON T.template_id=R.template_id
                  JOIN BOOK B ON B.book_id=R.winner_book_id
                 WHERE R.member_id=? ORDER BY R.created_at DESC,R.run_id DESC
                """;
        List<BookshelfWorldcupDTO> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, memberId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(BookshelfWorldcupDTO.builder().runId(resultSet.getLong("run_id"))
                            .templateTitle(resultSet.getString("template_title"))
                            .winnerBookId(resultSet.getLong("book_id"))
                            .winnerTitle(resultSet.getString("winner_title"))
                            .winnerImageUrl(BookImageUrlUtil.thumbnail(resultSet.getString("image_url"))).build());
                }
            }
        }
        return results;
    }

    private int countRatings(Connection connection, long memberId, Integer score) throws SQLException {
        String sql = "SELECT COUNT(*) FROM RATING R JOIN BOOK B ON B.book_id=R.book_id WHERE R.member_id=? AND B.status='APPROVED' AND R.comment_text IS NOT NULL AND TRIM(R.comment_text) IS NOT NULL AND (? IS NULL OR R.score=?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindScore(statement, memberId, score);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void bindScore(PreparedStatement statement, long memberId, Integer score) throws SQLException {
        statement.setLong(1, memberId);
        if (score == null) {
            statement.setNull(2, Types.NUMERIC);
            statement.setNull(3, Types.NUMERIC);
        } else {
            statement.setInt(2, score);
            statement.setInt(3, score);
        }
    }
}
