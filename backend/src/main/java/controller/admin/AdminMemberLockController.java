package controller.admin;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.AdminService;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet("/api/admin/members/lock")
public class AdminMemberLockController extends HttpServlet {

    private AdminService adminService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        adminService = new AdminService();
        gson = new Gson();
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        setJsonResponse(response);

        /*
         * 현재 로그인한 관리자 확인
         *
         * AdminAuthFilter에서도 권한을 검사하지만,
         * 어떤 관리자가 작업했는지 Service에 전달하기 위해
         * 회원번호를 가져옵니다.
         */
        HttpSession session = request.getSession(false);

        if (session == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
            return;
        }

        Object loginMemberIdAttribute =
                session.getAttribute("loginMemberId");

        if (!(loginMemberIdAttribute instanceof Number)) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인 회원 정보를 확인할 수 없습니다."
            );
            return;
        }

        long loginAdminMemberId =
                ((Number) loginMemberIdAttribute).longValue();

        final LockRequest lockRequest;

        try {
            lockRequest = readRequestBody(request);

        } catch (JsonSyntaxException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "요청 데이터 형식이 올바르지 않습니다."
            );
            return;
        }

        /*
         * 요청 본문 검증
         */
        if (lockRequest == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "회원 상태 정보가 없습니다."
            );
            return;
        }

        if (lockRequest.getMemberId() <= 0) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "올바른 회원 번호가 필요합니다."
            );
            return;
        }

        /*
         * Boolean이므로 JSON에서 locked가 빠지면 null
         */
        if (lockRequest.getLocked() == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "잠금 여부가 필요합니다."
            );
            return;
        }

        try {
            /*
             * 대상 회원번호 + 변경 상태 + 작업 관리자 번호 전달
             */
            adminService.changeMemberLock(
                    lockRequest.getMemberId(),
                    lockRequest.getLocked(),
                    loginAdminMemberId
            );

            response.setStatus(HttpServletResponse.SC_OK);

            gson.toJson(
                    Map.of(
                            "success", true,
                            "message",
                            lockRequest.getLocked()
                                    ? "회원이 잠금 처리되었습니다."
                                    : "회원 잠금이 해제되었습니다.",
                            "memberId", lockRequest.getMemberId()
                    ),
                    response.getWriter()
            );

        } catch (NoSuchElementException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    e.getMessage()
            );

        } catch (IllegalArgumentException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage()
            );

        } catch (SecurityException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    e.getMessage()
            );

        } catch (RuntimeException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "회원 상태 변경 중 오류가 발생했습니다."
            );
        }
    }

    private LockRequest readRequestBody(
            HttpServletRequest request
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");

        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(reader, LockRequest.class);
        }
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

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

    /*
     * 회원 잠금 요청 내부 DTO
     */
    private static class LockRequest {

        private long memberId;
        private Boolean locked;

        public long getMemberId() {
            return memberId;
        }

        public void setMemberId(long memberId) {
            this.memberId = memberId;
        }

        public Boolean getLocked() {
            return locked;
        }

        public void setLocked(Boolean locked) {
            this.locked = locked;
        }
    }
}