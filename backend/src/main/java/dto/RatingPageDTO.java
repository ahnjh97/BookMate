package dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RatingPageDTO {
    private List<RatingDTO> ratings;
    private int page;
    private int pageSize;
    private int totalCount;
    private int totalPages;
}
