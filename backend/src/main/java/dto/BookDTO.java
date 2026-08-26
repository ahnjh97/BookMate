package dto;

import java.sql.Date;

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

    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }
    public long getAuthorId() { return authorId; }
    public void setAuthorId(long authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public Date getPublishedDate() { return publishedDate; }
    public void setPublishedDate(Date publishedDate) { this.publishedDate = publishedDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
}
