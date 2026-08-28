package controller.auth;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dto.MemberDTO;
import exception.AuthenticationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.AuthService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/auth")
public class AuthController extends HttpServlet {
    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepare(response);
        HttpSession session = request.getSession(false);
        Object memberId = session == null ? null : session.getAttribute("loginMemberId");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("loggedIn", memberId instanceof Number);
        if (memberId instanceof Number) {
            if (session.getAttribute("loginNickname") == null) {
                MemberDTO member = authService.findMember(((Number) memberId).longValue());
                if (member != null) {
                    session.setAttribute("loginId", member.getLoginId());
                    session.setAttribute("loginNickname", member.getNickname());
                    session.setAttribute("loginRole", member.getRole());
                }
            }
            result.put("memberId", ((Number) memberId).longValue());
            result.put("loginId", session.getAttribute("loginId"));
            result.put("nickname", session.getAttribute("loginNickname"));
            result.put("role", session.getAttribute("loginRole"));
        }
        gson.toJson(result, response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepare(response);
        try {
            request.setCharacterEncoding("UTF-8");
            MemberDTO input = gson.fromJson(request.getReader(), MemberDTO.class);
            MemberDTO member = authService.login(input == null ? null : input.getLoginId(), input == null ? null : input.getPassword());
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) oldSession.invalidate();
            HttpSession session = request.getSession(true);
            session.setAttribute("loginMemberId", member.getMemberId());
            session.setAttribute("loginId", member.getLoginId());
            session.setAttribute("loginNickname", member.getNickname());
            session.setAttribute("loginRole", member.getRole());
            session.setMaxInactiveInterval(30 * 60);
            gson.toJson(Map.of("success", true, "message", "로그인했습니다."), response.getWriter());
        } catch (JsonSyntaxException | IllegalArgumentException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (AuthenticationException exception) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
        } catch (RuntimeException exception) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "로그인 중 오류가 발생했습니다.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepare(response);
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        gson.toJson(Map.of("success", true, "message", "로그아웃했습니다."), response.getWriter());
    }

    private void prepare(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        gson.toJson(Map.of("success", false, "message", message == null ? "요청을 처리하지 못했습니다." : message), response.getWriter());
    }
}
