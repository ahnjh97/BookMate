package controller.auth;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dto.MemberDTO;
import exception.DuplicateUserException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AuthService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/auth/signup")
public class SignupController extends HttpServlet {
    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepare(response);
        try {
            boolean available = authService.isLoginIdAvailable(request.getParameter("loginId"));
            gson.toJson(Map.of("success", true, "available", available), response.getWriter());
        } catch (IllegalArgumentException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (RuntimeException exception) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "아이디 확인 중 오류가 발생했습니다.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepare(response);
        try {
            request.setCharacterEncoding("UTF-8");
            MemberDTO member = gson.fromJson(request.getReader(), MemberDTO.class);
            long memberId = authService.signup(member);
            response.setStatus(HttpServletResponse.SC_CREATED);
            gson.toJson(Map.of("success", true, "message", "회원가입이 완료되었습니다.", "memberId", memberId), response.getWriter());
        } catch (JsonSyntaxException | IllegalArgumentException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (DuplicateUserException exception) {
            sendError(response, HttpServletResponse.SC_CONFLICT, exception.getMessage());
        } catch (RuntimeException exception) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "회원가입 중 오류가 발생했습니다.");
        }
    }

    private void prepare(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        gson.toJson(Map.of("success", false, "message", message == null ? "요청을 처리하지 못했습니다." : message), response.getWriter());
    }
}
