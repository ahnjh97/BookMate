package controller.admin;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dto.ReportDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.ReportService;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet("/api/admin/reports")
public class AdminReportController extends HttpServlet {

    private ReportService reportService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        reportService = new ReportService();
        gson = new Gson();
    }

    /* 1. 관리자 신고 목록 조회 */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        setJsonResponse(response);

        Long adminId = getLoginMemberId(request);

        if (adminId == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요한 기능입니다."
            );
            return;
        }

        String targetType =
                request.getParameter("targetType");

        String keyword =
                request.getParameter("keyword");

        try {
            List<ReportDTO> reports =
                    reportService.getReports(
                            targetType,
                            keyword
                    );

            gson.toJson(
                    Map.of(
                            "success", true,
                            "reports", reports
                    ),
                    response.getWriter()
            );
        } catch (IllegalArgumentException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage()
            );
        } catch (RuntimeException e) {
            e.printStackTrace();

            sendError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "신고 목록을 불러오는 중 오류가 발생했습니다."
            );
        }
    }

    /* 2. 관리자 신고 처리 */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        setJsonResponse(response);

        Long adminId = getLoginMemberId(request);

        if (adminId == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요한 기능입니다."
            );
            return;
        }

        final ProcessReportRequest reportRequest;

        try {
            reportRequest = readRequestBody(request);
        } catch (JsonSyntaxException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "요청 데이터 형식이 올바르지 않습니다."
            );
            return;
        }

        if (reportRequest == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "신고 처리 정보가 필요합니다."
            );
            return;
        }

        try {
            reportService.processReport(
                    reportRequest.getReportId(),
                    reportRequest.getStatus(),
                    adminId,
                    reportRequest.getAdminMemo()
            );

            gson.toJson(
                    Map.of(
                            "success", true,
                            "message", "신고 처리가 완료되었습니다."
                    ),
                    response.getWriter()
            );
        } catch (NoSuchElementException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    e.getMessage()
            );
        } catch (IllegalStateException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_CONFLICT,
                    e.getMessage()
            );
        } catch (IllegalArgumentException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage()
            );
        } catch (RuntimeException e) {
            e.printStackTrace();

            sendError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "신고 처리 중 오류가 발생했습니다."
            );
        }
    }

    /* 3. 요청 JSON 읽기 */
    private ProcessReportRequest readRequestBody(
            HttpServletRequest request
    ) throws IOException {
        request.setCharacterEncoding("UTF-8");

        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(
                    reader,
                    ProcessReportRequest.class
            );
        }
    }

    /* 4. 로그인 회원 번호 조회 */
    private Long getLoginMemberId(
            HttpServletRequest request
    ) {
        HttpSession session =
                request.getSession(false);

        if (session == null) {
            return null;
        }

        Object memberIdAttribute =
                session.getAttribute("loginMemberId");

        if (!(memberIdAttribute instanceof Number)) {
            return null;
        }

        return ((Number) memberIdAttribute)
                .longValue();
    }

    /* 5. JSON 응답 설정 */
    private void setJsonResponse(
            HttpServletResponse response
    ) {
        response.setContentType(
                "application/json;charset=UTF-8"
        );
        response.setCharacterEncoding("UTF-8");
    }

    /* 6. 오류 응답 */
    private void sendError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {
        response.setStatus(status);

        gson.toJson(
                Map.of(
                        "success", false,
                        "message", message
                ),
                response.getWriter()
        );
    }

    /* 7. 신고 처리 요청 데이터 */
    private static class ProcessReportRequest {

        private long reportId;
        private String status;
        private String adminMemo;

        public long getReportId() {
            return reportId;
        }

        public String getStatus() {
            return status;
        }

        public String getAdminMemo() {
            return adminMemo;
        }
    }
}