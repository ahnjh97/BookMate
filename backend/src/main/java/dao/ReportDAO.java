package dao;

import dto.ReportDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

    /* 4. 신고 조회 결과를 DTO로 변환 */
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
}