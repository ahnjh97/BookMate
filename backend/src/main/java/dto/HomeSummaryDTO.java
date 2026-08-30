package dto;

public record HomeSummaryDTO(
        long bookCount,
        long ratingCount,
        long tierTemplateCount,
        long tierParticipationCount,
        long worldcupTemplateCount,
        long worldcupParticipationCount) {
}
