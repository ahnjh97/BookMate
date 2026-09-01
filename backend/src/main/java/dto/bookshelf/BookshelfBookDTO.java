package dto.bookshelf;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookshelfBookDTO {
    private long bookId;
    private int score;
    private String comment;
    private String title;
    private String imageUrl;
    private String authorName;
}
