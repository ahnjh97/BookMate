package controller.auth;

import controller.BaseJsonServlet;
import dto.MemberDTO;
import exception.AuthenticationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.AuthService;
import util.SessionUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/auth", "/api/auth/login", "/api/auth/session"})
public class AuthController extends BaseJsonServlet {
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepareJson(response);
        HttpSession session = request.getSession(false);
        Long memberId = SessionUtil.memberId(request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("loggedIn", memberId != null);
        if (memberId != null) {
            if (SessionUtil.nickname(session) == null) {
                MemberDTO member = authService.findMember(memberId);
                if (member != null) {
                    SessionUtil.store(session, member);
                }
            }
            result.put("memberId", memberId);
            result.put("loginId", SessionUtil.loginId(session));
            result.put("nickname", SessionUtil.nickname(session));
            result.put("role", SessionUtil.role(session));
        }
        writeJson(response, result);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepareJson(response);
        try {
            MemberDTO input = readJson(request, MemberDTO.class);
            MemberDTO member = authService.login(input == null ? null : input.getLoginId(), input == null ? null : input.getPassword());
            SessionUtil.start(request, member);
            writeJson(response, Map.of(
                    "success", true,
                    "message", "로그인했습니다.",
                    "memberId", member.getMemberId(),
                    "role", member.getRole()
            ));
        } catch (IllegalArgumentException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (AuthenticationException exception) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
        } catch (RuntimeException exception) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "로그인 중 오류가 발생했습니다.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepareJson(response);
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        writeJson(response, Map.of("success", true, "message", "로그아웃했습니다."));
    }
}
