package service;

import dao.BookDAO;
import dto.BookDTO;
import dto.BookPageDTO;
import dto.SearchSuggestionDTO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class BookService {
    private final BookDAO bookDAO = new BookDAO();

    public List<SearchSuggestionDTO> findSearchSuggestions(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String normalizedKeyword = keyword.trim();
        if (normalizedKeyword.length() > 50) {
            throw new IllegalArgumentException("검색어는 50자 이하로 입력해 주세요.");
        }

        try (Connection connection = DBUtil.getConnection()) {
            return bookDAO.selectSearchSuggestions(connection, normalizedKeyword);
        } catch (SQLException exception) {
            throw new RuntimeException("자동완성 검색 중 오류가 발생했습니다.", exception);
        }
    }

    public BookPageDTO findBooks(String keyword, String genre, Long authorId, String sort, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(12, Math.min(pageSize, 100));
        int offset = (safePage - 1) * safePageSize;
        String normalizedSort = switch (sort == null ? "rating" : sort.trim().toLowerCase()) {
            case "title" -> "title";
            case "newest" -> "newest";
            default -> "rating";
        };

        try (Connection connection = DBUtil.getConnection()) {
            List<BookDTO> fetched = bookDAO.selectBooks(
                    connection,
                    keyword,
                    genre,
                    authorId,
                    normalizedSort,
                    offset,
                    safePageSize + 1
            );
            int totalCount = bookDAO.countBooks(connection, keyword, genre, authorId);
            int totalPages = (int) Math.ceil((double) totalCount / safePageSize);
            boolean hasMore = fetched.size() > safePageSize;
            List<BookDTO> books = hasMore
                    ? List.copyOf(fetched.subList(0, safePageSize))
                    : List.copyOf(fetched);
            return new BookPageDTO(
                    books,
                    hasMore,
                    hasMore ? safePage + 1 : safePage,
                    totalCount,
                    totalPages
            );
        } catch (SQLException exception) {
            throw new RuntimeException("책 목록을 조회하는 중 오류가 발생했습니다.", exception);
        }
    }

    public BookDTO findBook(long bookId) {
        if (bookId <= 0) {
            throw new IllegalArgumentException("올바른 책 번호가 필요합니다.");
        }

        try (Connection connection = DBUtil.getConnection()) {
            return bookDAO.selectBookById(connection, bookId);
        } catch (SQLException exception) {
            throw new RuntimeException("책 정보를 조회하는 중 오류가 발생했습니다.", exception);
        }
    }

    public List<BookDTO> findBookRankings(
            String genre,
            String sort,
            int minimumRatings,
            int limit
    ) {
        String normalizedGenre = genre == null ? "" : genre.trim();
        if (normalizedGenre.isEmpty() || normalizedGenre.length() > 50) {
            throw new IllegalArgumentException("장르를 선택해 주세요.");
        }
        String normalizedSort = sort == null || sort.isBlank() ? "average" : sort.trim().toLowerCase();
        if (!"average".equals(normalizedSort) && !"count".equals(normalizedSort)) {
            throw new IllegalArgumentException("올바른 정렬 기준이 필요합니다.");
        }
        if (minimumRatings < 1 || minimumRatings > 1000) {
            throw new IllegalArgumentException("최소 평가 인원은 1명 이상이어야 합니다.");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("랭킹은 최대 100권까지 조회할 수 있습니다.");
        }

        try (Connection connection = DBUtil.getConnection()) {
            return bookDAO.selectBookRankings(connection, normalizedGenre, normalizedSort, minimumRatings, limit);
        } catch (SQLException exception) {
            throw new RuntimeException("책 랭킹을 조회하는 중 오류가 발생했습니다.", exception);
        }
    }
}
