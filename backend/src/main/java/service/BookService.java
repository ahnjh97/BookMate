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

    public BookPageDTO findBooks(String keyword, String genre, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(12, Math.min(pageSize, 100));
        int offset = (safePage - 1) * safePageSize;

        try (Connection connection = DBUtil.getConnection()) {
            List<BookDTO> fetched = bookDAO.selectBooks(
                    connection,
                    keyword,
                    genre,
                    offset,
                    safePageSize + 1
            );
            boolean hasMore = fetched.size() > safePageSize;
            List<BookDTO> books = hasMore
                    ? List.copyOf(fetched.subList(0, safePageSize))
                    : List.copyOf(fetched);
            return new BookPageDTO(books, hasMore, hasMore ? safePage + 1 : safePage);
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
}
