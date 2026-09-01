package service;

import dao.PostCommentDAO;
import dao.PostDAO;
import dao.ReportDAO;
import dto.ReportDTO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class ReportService {

    private final ReportDAO reportDAO;
    private final PostDAO postDAO;
    private final PostCommentDAO postCommentDAO;

    public ReportService() {
        reportDAO = new ReportDAO();
        postDAO = new PostDAO();
        postCommentDAO = new PostCommentDAO();
    }

    /* 1. 신고 등록 */
    public long createReport(
            long reporterId,
            String targetType,
            long targetId,
            String reasonType,
            String reasonDetail
    ) {
        validateMemberId(reporterId);
        validateTargetType(targetType);
        validateTargetId(targetId);
        validateReasonType(reasonType);
        validateReasonDetail(reasonDetail);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            /* 신고 대상 존재 여부 및 본인 콘텐츠 여부 확인 */
            validateTarget(
                    conn,
                    reporterId,
                    targetType,
                    targetId
            );

            /* 동일 대상 중복 신고 방지 */
            boolean alreadyReported =
                    reportDAO.existsReport(
                            conn,
                            reporterId,
                            targetType,
                            targetId
                    );

            if (alreadyReported) {
                throw new IllegalStateException(
                        "이미 신고한 대상입니다."
                );
            }

            ReportDTO report = new ReportDTO();

            report.setReporterId(reporterId);
            report.setTargetType(targetType);
            report.setTargetId(targetId);
            report.setReasonType(reasonType);
            report.setReasonDetail(
                    normalizeReasonDetail(reasonDetail)
            );

            long reportId =
                    reportDAO.insertReport(
                            conn,
                            report
                    );

            conn.commit();

            return reportId;
        } catch (SQLException e) {
            rollback(conn);

            throw new RuntimeException(
                    "신고 등록에 실패했습니다.",
                    e
            );
        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } finally {
            close(conn);
        }
    }

    /* 2. 관리자 신고 목록 조회 */
    public List<ReportDTO> getReports(
            String targetType,
            String keyword
    ) {
        validateReportFilterTargetType(targetType);

        try (Connection conn = DBUtil.getConnection()) {
            return reportDAO.selectReports(
                    conn,
                    normalizeTargetType(targetType),
                    normalizeKeyword(keyword)
            );
        } catch (SQLException e) {
            throw new RuntimeException(
                    "신고 목록을 불러오지 못했습니다.",
                    e
            );
        }
    }

    /* 3. 관리자 신고 처리 */
    public void processReport(
            long reportId,
            String status,
            long adminId,
            String adminMemo
    ) {
        validateTargetId(reportId);
        validateMemberId(adminId);
        validateReportStatus(status);
        validateAdminMemo(adminMemo);

        Connection conn = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            ReportDTO report =
                    reportDAO.selectReportById(
                            conn,
                            reportId
                    );

            if (report == null) {
                throw new NoSuchElementException(
                        "신고 정보를 찾을 수 없습니다."
                );
            }

            if (!"PENDING".equals(report.getStatus())) {
                throw new IllegalStateException(
                        "이미 처리된 신고입니다."
                );
            }

            boolean updated =
                    reportDAO.updateReportStatus(
                            conn,
                            reportId,
                            status,
                            adminId,
                            normalizeAdminMemo(adminMemo)
                    );

            if (!updated) {
                throw new IllegalStateException(
                        "신고 상태를 변경하지 못했습니다."
                );
            }

            conn.commit();
        } catch (SQLException e) {
            rollback(conn);

            throw new RuntimeException(
                    "신고 처리에 실패했습니다.",
                    e
            );
        } catch (RuntimeException e) {
            rollback(conn);
            throw e;
        } finally {
            close(conn);
        }
    }

    /* 4. 신고 대상 검증 */
    private void validateTarget(
            Connection conn,
            long reporterId,
            String targetType,
            long targetId
    ) throws SQLException {

        switch (targetType) {
            case "POST":
                validatePostTarget(
                        conn,
                        reporterId,
                        targetId
                );
                break;

            case "COMMENT":
                validateCommentTarget(
                        conn,
                        reporterId,
                        targetId
                );
                break;

            default:
                throw new IllegalArgumentException(
                        "신고할 수 없는 대상입니다."
                );
        }
    }

    /* 5. 게시글 신고 대상 검증 */
    private void validatePostTarget(
            Connection conn,
            long reporterId,
            long postId
    ) throws SQLException {

        if (!postDAO.existsPost(conn, postId)) {
            throw new NoSuchElementException(
                    "존재하지 않거나 신고할 수 없는 게시글입니다."
            );
        }

        Long writerId =
                postDAO.selectWriterId(
                        conn,
                        postId
                );

        if (writerId == null) {
            throw new NoSuchElementException(
                    "게시글 작성자 정보를 찾을 수 없습니다."
            );
        }

        if (writerId == reporterId) {
            throw new SecurityException(
                    "본인이 작성한 게시글은 신고할 수 없습니다."
            );
        }
    }

    /* 6. 댓글 신고 대상 검증 */
    private void validateCommentTarget(
            Connection conn,
            long reporterId,
            long commentId
    ) throws SQLException {

        if (!postCommentDAO.existsComment(
                conn,
                commentId
        )) {
            throw new NoSuchElementException(
                    "존재하지 않거나 신고할 수 없는 댓글입니다."
            );
        }

        Long writerId =
                postCommentDAO.selectWriterId(
                        conn,
                        commentId
                );

        if (writerId == null) {
            throw new NoSuchElementException(
                    "댓글 작성자 정보를 찾을 수 없습니다."
            );
        }

        if (writerId == reporterId) {
            throw new SecurityException(
                    "본인이 작성한 댓글은 신고할 수 없습니다."
            );
        }
    }

    /* 7. 회원 번호 검증 */
    private void validateMemberId(long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException(
                    "로그인 회원 정보가 올바르지 않습니다."
            );
        }
    }

    /* 8. 신고 대상 종류 검증 */
    private void validateTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            throw new IllegalArgumentException(
                    "신고 대상 종류가 필요합니다."
            );
        }

        if (
                !"POST".equals(targetType) &&
                        !"COMMENT".equals(targetType)
        ) {
            throw new IllegalArgumentException(
                    "신고할 수 없는 대상입니다."
            );
        }
    }

    /* 9. 관리자 신고 목록 대상 종류 검증 */
    private void validateReportFilterTargetType(
            String targetType
    ) {
        if (
                targetType == null ||
                        targetType.isBlank()
        ) {
            return;
        }

        String normalized =
                targetType.trim().toUpperCase();

        if (
                !"POST".equals(normalized) &&
                        !"COMMENT".equals(normalized) &&
                        !"AUTHOR_REVIEW".equals(normalized)
        ) {
            throw new IllegalArgumentException(
                    "올바르지 않은 신고 대상 종류입니다."
            );
        }
    }

    /* 10. 신고 대상 번호 검증 */
    private void validateTargetId(long targetId) {
        if (targetId <= 0) {
            throw new IllegalArgumentException(
                    "올바르지 않은 신고 대상 번호입니다."
            );
        }
    }

    /* 11. 신고 사유 검증 */
    private void validateReasonType(String reasonType) {
        if (
                reasonType == null ||
                        reasonType.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "신고 사유를 선택해주세요."
            );
        }

        switch (reasonType) {
            case "SPAM":
            case "ABUSE":
            case "INAPPROPRIATE":
            case "OTHER":
                break;

            default:
                throw new IllegalArgumentException(
                        "올바르지 않은 신고 사유입니다."
                );
        }
    }

    /* 12. 신고 상세 내용 검증 */
    private void validateReasonDetail(
            String reasonDetail
    ) {
        if (
                reasonDetail != null &&
                        reasonDetail.trim().length() > 1000
        ) {
            throw new IllegalArgumentException(
                    "신고 상세 내용은 1000자 이하로 입력해주세요."
            );
        }
    }

    /* 13. 관리자 신고 처리 상태 검증 */
    private void validateReportStatus(
            String status
    ) {
        if (
                status == null ||
                        status.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "신고 처리 상태가 필요합니다."
            );
        }

        if (
                !"APPROVED".equals(status) &&
                        !"REJECTED".equals(status)
        ) {
            throw new IllegalArgumentException(
                    "올바르지 않은 신고 처리 상태입니다."
            );
        }
    }

    /* 14. 관리자 처리 메모 검증 */
    private void validateAdminMemo(
            String adminMemo
    ) {
        if (
                adminMemo != null &&
                        adminMemo.trim().length() > 1000
        ) {
            throw new IllegalArgumentException(
                    "관리자 메모는 1000자 이하로 입력해주세요."
            );
        }
    }

    /* 15. 신고 상세 내용 정리 */
    private String normalizeReasonDetail(
            String reasonDetail
    ) {
        if (
                reasonDetail == null ||
                        reasonDetail.isBlank()
        ) {
            return null;
        }

        return reasonDetail.trim();
    }

    /* 16. 신고 대상 종류 정리 */
    private String normalizeTargetType(
            String targetType
    ) {
        if (
                targetType == null ||
                        targetType.isBlank()
        ) {
            return null;
        }

        return targetType.trim().toUpperCase();
    }

    /* 17. 신고 검색어 정리 */
    private String normalizeKeyword(
            String keyword
    ) {
        if (
                keyword == null ||
                        keyword.isBlank()
        ) {
            return null;
        }

        return keyword.trim();
    }

    /* 18. 관리자 처리 메모 정리 */
    private String normalizeAdminMemo(
            String adminMemo
    ) {
        if (
                adminMemo == null ||
                        adminMemo.isBlank()
        ) {
            return null;
        }

        return adminMemo.trim();
    }

    /* 19. 트랜잭션 롤백 */
    private void rollback(Connection conn) {
        if (conn == null) {
            return;
        }

        try {
            conn.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* 20. DB 연결 종료 */
    private void close(Connection conn) {
        if (conn == null) {
            return;
        }

        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}