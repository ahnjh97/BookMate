package service;

import dao.PostCommentDAO;
import dao.PostDAO;
import dao.ReportDAO;
import dto.ReportDTO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
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

    /* 2. 신고 대상 검증 */
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

    /* 3. 게시글 신고 대상 검증 */
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

    /* 4. 댓글 신고 대상 검증 */
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

    /* 5. 회원 번호 검증 */
    private void validateMemberId(long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException(
                    "로그인 회원 정보가 올바르지 않습니다."
            );
        }
    }

    /* 6. 신고 대상 종류 검증 */
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

    /* 7. 신고 대상 번호 검증 */
    private void validateTargetId(long targetId) {
        if (targetId <= 0) {
            throw new IllegalArgumentException(
                    "올바르지 않은 신고 대상 번호입니다."
            );
        }
    }

    /* 8. 신고 사유 검증 */
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

    /* 9. 신고 상세 내용 검증 */
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

    /* 10. 신고 상세 내용 정리 */
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

    /* 11. 트랜잭션 롤백 */
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

    /* 12. DB 연결 종료 */
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