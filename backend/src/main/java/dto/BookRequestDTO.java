package dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class BookRequestDTO {

    private Long requestId;
    private Long memberId;
    private String memberNickname;

    private String isbn;
    private String title;
    private String authorName;
    private String genre;
    private String publisher;
    private Date publishedDate;
    private String description;
    private String imageUrl;
    private String sourceUrl;

    private String status;
    private String rejectReason;

    private Long reviewedBy;
    private Date requestedAt;
    private Date reviewedAt;

    public record Page(
            List<BookRequestDTO> requests,
            int page,
            int pageSize,
            int totalCount,
            int totalPages
    ) {
    }
}