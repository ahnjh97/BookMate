package dto.bookshelf;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BookshelfRatingPageDTO {
    private List<BookshelfBookDTO> books;
    private int page;
    private int pageSize;
    private int totalCount;
    private int totalPages;
}
