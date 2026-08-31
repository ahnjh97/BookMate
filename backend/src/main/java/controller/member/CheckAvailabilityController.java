// ============================================
// 파일: controller/member/CheckAvailabilityController.java
// GET /api/members/check-nickname?nickname=xxx
// GET /api/members/check-email?email=xxx
// (수정 화면 전용, 본인 제외 검사)
// ============================================

package controller.member;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AuthService;
import util.ResponseWrapper;
import util.SessionUtil;

import java.io.IOException;
import java.util.Map;

@WebServlet({"/api/members/check-nickname", "/api/members/check-email"})
public class CheckAvailabilityController extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Long memberId = SessionUtil.getLoginMemberId(req);
        if (memberId == null) {
            ResponseWrapper.fail(res, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
            return;
        }

        String path = req.getServletPath();
        boolean available;

        if (path.endsWith("check-nickname")) {
            available = authService.isNicknameAvailable(req.getParameter("nickname"), memberId);
        } else {
            available = authService.isEmailAvailable(req.getParameter("email"), memberId);
        }

        ResponseWrapper.success(res, Map.of("available", available));
    }
}
