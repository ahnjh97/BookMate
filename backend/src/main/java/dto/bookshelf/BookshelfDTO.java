package dto.bookshelf;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BookshelfDTO {
    private long memberId;
    private String nickname;
    private BookshelfSummaryDTO counts;
    private List<BookshelfBookDTO> favoriteBooks;
    private int favoriteBooksTotal;
    private List<BookshelfTierDTO> tierLists;
    private List<BookshelfWorldcupDTO> worldcupResults;
}
