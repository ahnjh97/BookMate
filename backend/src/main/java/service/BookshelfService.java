package service;

import dao.BookshelfDAO;
import dto.bookshelf.BookshelfDTO;
import dto.bookshelf.BookshelfMemberDTO;
import dto.bookshelf.BookshelfRatingPageDTO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public class BookshelfService {
    private final BookshelfDAO bookshelfDAO = new BookshelfDAO();

    public BookshelfDTO findBookshelf(long memberId) {
        if (memberId <= 0) throw new IllegalArgumentException("회원 번호가 올바르지 않습니다.");
        try (Connection connection = DBUtil.getConnection()) {
            BookshelfMemberDTO member = bookshelfDAO.selectMember(connection, memberId);
            if (member == null) return null;
            BookshelfRatingPageDTO ratingPage = bookshelfDAO.selectRatingPage(connection, memberId, null, 1, 10);
            return BookshelfDTO.builder().memberId(member.getMemberId()).nickname(member.getNickname())
                    .counts(bookshelfDAO.selectCounts(connection, memberId))
                    .favoriteBooks(ratingPage.getBooks()).favoriteBooksTotal(ratingPage.getTotalCount())
                    .tierLists(bookshelfDAO.selectTierLists(connection, memberId))
                    .worldcupResults(bookshelfDAO.selectWorldcupResults(connection, memberId)).build();
        } catch (SQLException exception) {
            throw new RuntimeException("회원 책장을 불러오지 못했습니다.", exception);
        }
    }

    public BookshelfRatingPageDTO findRatingPage(long memberId, Integer score, int page, int size) {
        if (memberId <= 0) throw new IllegalArgumentException("회원 번호가 올바르지 않습니다.");
        if (score != null && (score < 1 || score > 5)) {
            throw new IllegalArgumentException("별점은 1점부터 5점까지 선택할 수 있습니다.");
        }
        int safeSize = Math.min(Math.max(size, 1), 50);
        try (Connection connection = DBUtil.getConnection()) {
            return bookshelfDAO.selectRatingPage(connection, memberId, score, Math.max(page, 1), safeSize);
        } catch (SQLException exception) {
            throw new RuntimeException("후기를 남긴 책을 불러오지 못했습니다.", exception);
        }
    }
}
