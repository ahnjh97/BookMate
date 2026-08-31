package controller.report;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.ReportService;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet("/api/reports/create")
public class ReportCreateController extends HttpServlet {

    private ReportService reportService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        reportService = new ReportService();
        gson = new Gson();
    }

    /* 1. 신고 등록 */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        setJsonResponse(response);

        Long loginMemberId = getLoginMemberId(request);

        if (loginMemberId == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요한 기능입니다."
            );
            return;
        }

        final CreateReportRequest reportRequest;

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
                    "신고 정보가 필요합니다."
            );
            return;
        }

        try {
            long reportId = reportService.createReport(
                    loginMemberId,
                    reportRequest.getTargetType(),
                    reportRequest.getTargetId(),
                    reportRequest.getReasonType(),
                    reportRequest.getReasonDetail()
            );

            response.setStatus(HttpServletResponse.SC_CREATED);

            gson.toJson(
                    Map.of(
                            "success", true,
                            "reportId", reportId,
                            "message", "신고가 접수되었습니다."
                    ),
                    response.getWriter()
            );
        } catch (NoSuchElementException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    e.getMessage()
            );
        } catch (SecurityException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
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

    /* 2. 요청 JSON 읽기 */
    private CreateReportRequest readRequestBody(
            HttpServletRequest request
    ) throws IOException {
        request.setCharacterEncoding("UTF-8");

        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(
                    reader,
                    CreateReportRequest.class
            );
        }
    }

    /* 3. 로그인 회원 번호 조회 */
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

    /* 4. JSON 응답 설정 */
    private void setJsonResponse(
            HttpServletResponse response
    ) {
        response.setContentType(
                "application/json;charset=UTF-8"
        );
        response.setCharacterEncoding("UTF-8");
    }

    /* 5. 오류 응답 */
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

    /* 6. 신고 등록 요청 데이터 */
    private static class CreateReportRequest {

        private String targetType;
        private long targetId;
        private String reasonType;
        private String reasonDetail;

        public String getTargetType() {
            return targetType;
        }

        public long getTargetId() {
            return targetId;
        }

        public String getReasonType() {
            return reasonType;
        }

        public String getReasonDetail() {
            return reasonDetail;
        }
    }
}