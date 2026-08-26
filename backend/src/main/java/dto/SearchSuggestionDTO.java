package dto;

public record SearchSuggestionDTO(
        String type,
        long id,
        String name,
        String detail,
        String imageUrl
) {
}
