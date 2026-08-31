package dto;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.util.Map;

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
    private String detailImageUrl;
    private String sourceUrl;
    private String status;
    private double averageRating;
    private int ratingCount;
    private Map<Integer, Integer> ratingDistribution;

    public BookDTO() {
    }

}
