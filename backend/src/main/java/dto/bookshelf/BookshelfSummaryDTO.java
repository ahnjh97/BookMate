package dto.bookshelf;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookshelfSummaryDTO {
    private int ratings;
    private double ratingAverage;
    private int tierLists;
    private int worldcups;
}
