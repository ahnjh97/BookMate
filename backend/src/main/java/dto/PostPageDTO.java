package dto;

import java.util.List;

public record PostPageDTO(List<PostDTO> posts, int page, int pageSize, int totalCount, int totalPages) { }
