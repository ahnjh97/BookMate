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
import exception.AuthenticationException;
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
        if (memberId == null) throw new AuthenticationException("로그인이 필요합니다.");

        var member = authService.findMember(memberId);
        boolean isAuthor = authService.isAuthorAccount(memberId);

        Map<String, Object> data = new HashMap<>();
        data.put("loginId", member.getLoginId());
        data.put("nickname", member.getNickname());
        data.put("email", member.getEmail());
        data.put("isAuthor", isAuthor);
        if (isAuthor) {
            data.put("selfIntro", authService.getSelfIntro(memberId));
        }

        ResponseWrapper.success(res, data);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Long memberId = SessionUtil.getLoginMemberId(req);
        if (memberId == null) throw new AuthenticationException("로그인이 필요합니다.");

        UpdateRequest body = gson.fromJson(req.getReader(), UpdateRequest.class);
        authService.updateProfile(memberId, body.nickname, body.email);

        if (authService.isAuthorAccount(memberId) && body.selfIntro != null) {
            authService.updateSelfIntro(memberId, body.selfIntro);
        }

        ResponseWrapper.successMessage(res, "변경사항이 저장되었습니다.");
    }

    private static class UpdateRequest {
        String nickname;
        String email;
        String selfIntro;
    }
}