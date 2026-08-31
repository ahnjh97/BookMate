// ============================================
// 파일: controller/member/PasswordController.java
//
// [목적] 비밀번호 변경 — 현재비번 검증(재인증) 후 새 비밀번호로 교체
// PUT /api/members/password
// ============================================

package controller.member;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AuthService;
import util.ResponseWrapper;
import util.SessionUtil;

import java.io.IOException;

@WebServlet("/api/members/password")
public class PasswordController extends HttpServlet {

    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Long memberId = SessionUtil.getLoginMemberId(req);
        if (memberId == null) {
            ResponseWrapper.fail(res, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
            return;
        }

        ChangeRequest body = gson.fromJson(req.getReader(), ChangeRequest.class);
        authService.changePassword(memberId, body.currentPassword, body.newPassword);

        ResponseWrapper.successMessage(res, "비밀번호가 변경되었습니다.");
    }

    private static class ChangeRequest {
        String currentPassword;
        String newPassword;
    }
}
