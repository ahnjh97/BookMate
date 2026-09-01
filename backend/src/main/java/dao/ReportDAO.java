package dao;

import dto.ReportDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    /* 1. 신고 등록 */
    public long insertReport(Connection conn, ReportDTO report) throws SQLException {
        String sql = """
                INSERT INTO REPORT (
                    report_id,
                    reporter_id,
                    target_type,
                    target_id,
                    reason_type,
                    reason_detail,
                    status,
                    created_at
                ) VALUES (
                    SEQ_REPORT.NEXTVAL,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    'PENDING',
                    SYSDATE
                )
                """;

        String[] generatedColumns = {"REPORT_ID"};

        try (PreparedStatement pstmt = conn.prepareStatement(sql, generatedColumns)) {
            pstmt.setLong(1, report.getReporterId());
            pstmt.setString(2, report.getTargetType());
            pstmt.setLong(3, report.getTargetId());
            pstmt.setString(4, report.getReasonType());

            if (report.getReasonDetail() == null || report.getReasonDetail().isBlank()) {
                pstmt.setNull(5, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(5, report.getReasonDetail().trim());
            }

            int result = pstmt.executeUpdate();

            if (result == 0) {
                throw new SQLException("신고 등록에 실패했습니다.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }

        throw new SQLException("생성된 신고 번호를 가져오지 못했습니다.");
    }

    /* 2. 동일 대상 중복 신고 여부 확인 */
    public boolean existsReport(
            Connection conn,
            long reporterId,
            String targetType,
            long targetId
    ) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM REPORT
                WHERE reporter_id = ?
                  AND target_type = ?
                  AND target_id = ?
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, reporterId);
            pstmt.setString(2, targetType);
            pstmt.setLong(3, targetId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /* 3. 신고 번호 조회 */
    public ReportDTO selectReportById(Connection conn, long reportId) throws SQLException {
        String sql = """
                SELECT
                    report_id,
                    reporter_id,
                    target_type,
                    target_id,
                    reason_type,
                    reason_detail,
                    status,
                    admin_id,
                    admin_memo,
                    created_at,
                    processed_at
                FROM REPORT
                WHERE report_id = ?
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, reportId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapReportRow(rs);
                }
            }
        }

        return null;
    }

    /* 4. 관리자 신고 목록 조회 */
    public List<ReportDTO> selectReports(
            Connection conn,
            String targetType,
            String keyword
    ) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    R.report_id,
                    R.reporter_id,
                    M.nickname AS reporter_nickname,
                    R.target_type,
                    R.target_id,
                    R.reason_type,
                    R.reason_detail,
                    R.status,
                    R.admin_id,
                    R.admin_memo,
                    R.created_at,
                    R.processed_at
                FROM REPORT R
                JOIN MEMBER M
                  ON R.reporter_id = M.member_id
                WHERE 1 = 1
                """);

        List<Object> params = new ArrayList<>();

        if (targetType != null && !targetType.isBlank()) {
            sql.append("""
                     AND R.target_type = ?
                    """);
            params.add(targetType.trim().toUpperCase());
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND (
                         LOWER(M.nickname) LIKE ?
                         OR LOWER(R.reason_type) LIKE ?
                         OR LOWER(NVL(R.reason_detail, '')) LIKE ?
                         OR TO_CHAR(R.target_id) LIKE ?
                     )
                    """);

            String searchKeyword = "%" + keyword.trim().toLowerCase() + "%";

            params.add(searchKeyword);
            params.add(searchKeyword);
            params.add(searchKeyword);
            params.add(searchKeyword);
        }

        sql.append("""
                 ORDER BY R.created_at DESC, R.report_id DESC
                """);

        List<ReportDTO> reports = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ReportDTO report = mapReportRow(rs);

                    report.setReporterNickname(
                            rs.getString("reporter_nickname")
                    );

                    report.setTargetContent(
                            getTargetTypeLabel(report.getTargetType())
                                    + " #" + report.getTargetId()
                    );

                    reports.add(report);
                }
            }
        }

        return reports;
    }

    /* 5. 관리자 신고 처리 */
    public boolean updateReportStatus(
            Connection conn,
            long reportId,
            String status,
            long adminId,
            String adminMemo
    ) throws SQLException {
        String sql = """
                UPDATE REPORT
                SET status = ?,
                    admin_id = ?,
                    admin_memo = ?,
                    processed_at = SYSDATE
                WHERE report_id = ?
                  AND status = 'PENDING'
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setLong(2, adminId);

            if (adminMemo == null || adminMemo.isBlank()) {
                pstmt.setNull(3, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(3, adminMemo.trim());
            }

            pstmt.setLong(4, reportId);

            return pstmt.executeUpdate() > 0;
        }
    }

    /* 6. 신고 조회 결과를 DTO로 변환 */
    private ReportDTO mapReportRow(ResultSet rs) throws SQLException {
        ReportDTO report = new ReportDTO();

        report.setReportId(rs.getLong("report_id"));
        report.setReporterId(rs.getLong("reporter_id"));
        report.setTargetType(rs.getString("target_type"));
        report.setTargetId(rs.getLong("target_id"));
        report.setReasonType(rs.getString("reason_type"));
        report.setReasonDetail(rs.getString("reason_detail"));
        report.setStatus(rs.getString("status"));

        long adminId = rs.getLong("admin_id");
        report.setAdminId(
                rs.wasNull() ? null : adminId
        );

        report.setAdminMemo(rs.getString("admin_memo"));
        report.setCreatedAt(rs.getTimestamp("created_at"));
        report.setProcessedAt(rs.getTimestamp("processed_at"));

        return report;
    }

    /* 7. 신고 대상 유형 이름 */
    private String getTargetTypeLabel(String targetType) {
        if (targetType == null) {
            return "대상";
        }

        return switch (targetType) {
            case "POST" -> "게시글";
            case "COMMENT" -> "댓글";
            case "AUTHOR_REVIEW" -> "작가 후기";
            default -> "대상";
        };
    }
}