package dto.bookshelf;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookshelfTierDTO {
    private long tierListId;
    private long templateId;
    private String title;
    private String templateTitle;
}
