package dto;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Setter
@Getter
public class BookDTO {
    private long bookId;
    private long authorId;
    private String authorName;
    private String title;
    private String genre;
    private String publisher;
    private Date publishedDate;
    private String description;
    private String imageUrl;
    private String status;
    private double averageRating;
    private int ratingCount;

    public BookDTO() {
    }

}
