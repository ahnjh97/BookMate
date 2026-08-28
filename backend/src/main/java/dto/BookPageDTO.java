package dto;

import java.util.List;

public record BookPageDTO(
        List<BookDTO> books,
        boolean hasMore,
        int nextPage,
        int totalCount,
        int totalPages
) {
}
