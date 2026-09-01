package dto.bookrequest;

import lombok.Builder;
import lombok.Getter;

import java.sql.Date;
import java.sql.Timestamp;

@Getter
@Builder
public class BookRequestDTO {
    private long requestId;
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
    private Timestamp requestedAt;
    private String requesterNickname;
}
