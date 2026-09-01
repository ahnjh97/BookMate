package dto.bookshelf;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookshelfWorldcupDTO {
    private long runId;
    private String templateTitle;
    private long winnerBookId;
    private String winnerTitle;
    private String winnerImageUrl;
}
