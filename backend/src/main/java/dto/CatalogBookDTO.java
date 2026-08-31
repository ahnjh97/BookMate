package dto;

public record CatalogBookDTO(
        String isbn,
        String title,
        String authorName,
        String genre,
        String publisher,
        String publishedDate,
        String description,
        String imageUrl,
        String sourceUrl
) {
}
