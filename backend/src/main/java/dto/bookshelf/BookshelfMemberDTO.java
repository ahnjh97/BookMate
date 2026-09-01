package dto.bookshelf;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookshelfMemberDTO {
    private long memberId;
    private String nickname;
}
