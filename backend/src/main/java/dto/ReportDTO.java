package dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
public class ReportDTO {

    /* 신고 데이터를 담는 저장소 */
    private long reportId;
    private long reporterId;
    private String targetType;
    private long targetId;
    private String reasonType;
    private String reasonDetail;
    private String status;
    private Long adminId;
    private String adminMemo;
    private Timestamp createdAt;
    private Timestamp processedAt;
}