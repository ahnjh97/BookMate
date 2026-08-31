// ============================================
// 파일: controller/member/PasswordController.java
//
// [목적] 비밀번호 변경 — 현재비번 검증(재인증) 후 새 비밀번호로 교체
// PUT /api/members/password
// ============================================

package controller.member;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import exception.AuthenticationException;
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
        req.setCharacterEncoding("UTF-8");
        Long memberId = SessionUtil.getLoginMemberId(req);
        if (memberId == null) {
            ResponseWrapper.fail(res, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
            return;
        }

        try {
            ChangeRequest body = gson.fromJson(req.getReader(), ChangeRequest.class);
            if (body == null) throw new IllegalArgumentException("변경할 비밀번호를 입력해 주세요.");
            authService.changePassword(memberId, body.currentPassword, body.newPassword);
            ResponseWrapper.successMessage(res, "비밀번호가 변경되었습니다.");
        } catch (AuthenticationException exception) {
            ResponseWrapper.fail(res, HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
        } catch (JsonSyntaxException | IllegalArgumentException exception) {
            ResponseWrapper.fail(res, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            ResponseWrapper.fail(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "비밀번호를 변경하지 못했습니다.");
        }
    }

    private static class ChangeRequest {
        String currentPassword;
        String newPassword;
    }
}
