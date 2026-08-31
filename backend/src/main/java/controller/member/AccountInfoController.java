// ============================================
// 파일: controller/member/AccountInfoController.java
//
// [목적] 내정보(닉네임/이메일/작가소개) 조회·수정
// GET  /api/members/me : 현재 값 조회(수정 화면 진입 시)
// PUT  /api/members/me : 수정사항 저장
//
// [세션 인증] SessionUtil.getLoginMemberId()로 로그인 여부 확인 —
//            없으면 401(AuthenticationException을 직접 던져 GlobalExceptionFilter가 처리)
// ============================================

package controller.member;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import exception.DuplicateUserException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AuthService;
import util.ResponseWrapper;
import util.SessionUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/members/me")
public class AccountInfoController extends HttpServlet {

    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Long memberId = SessionUtil.getLoginMemberId(req);
        if (memberId == null) {
            ResponseWrapper.fail(res, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
            return;
        }

        try {
            var member = authService.findMember(memberId);
            boolean isAuthor = authService.isAuthorAccount(memberId);
            Map<String, Object> data = new HashMap<>();
            data.put("loginId", member.getLoginId());
            data.put("nickname", member.getNickname());
            data.put("email", member.getEmail());
            data.put("isAuthor", isAuthor);
            if (isAuthor) data.put("selfIntro", authService.getSelfIntro(memberId));
            ResponseWrapper.success(res, data);
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            ResponseWrapper.fail(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "회원정보를 불러오지 못했습니다.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        req.setCharacterEncoding("UTF-8");
        Long memberId = SessionUtil.getLoginMemberId(req);
        if (memberId == null) {
            ResponseWrapper.fail(res, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
            return;
        }

        try {
            UpdateRequest body = gson.fromJson(req.getReader(), UpdateRequest.class);
            if (body == null) throw new IllegalArgumentException("수정할 회원정보를 입력해 주세요.");
            authService.updateProfile(memberId, body.nickname, body.email);
            if (authService.isAuthorAccount(memberId) && body.selfIntro != null) {
                authService.updateSelfIntro(memberId, body.selfIntro);
            }
            SessionUtil.store(req.getSession(false), authService.findMember(memberId));
            ResponseWrapper.successMessage(res, "변경사항이 저장되었습니다.");
        } catch (DuplicateUserException exception) {
            ResponseWrapper.fail(res, HttpServletResponse.SC_CONFLICT, exception.getMessage());
        } catch (JsonSyntaxException | IllegalArgumentException exception) {
            ResponseWrapper.fail(res, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            ResponseWrapper.fail(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "회원정보를 수정하지 못했습니다.");
        }
    }

    private static class UpdateRequest {
        String nickname;
        String email;
        String selfIntro;
    }
}
