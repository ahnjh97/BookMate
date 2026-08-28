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
import java.util.Map;

@WebServlet("/api/auth/login")
public class LoginController extends HttpServlet {

    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        prepare(response);

        try {
            request.setCharacterEncoding("UTF-8");

            LoginRequest loginRequest =
                    gson.fromJson(
                            request.getReader(),
                            LoginRequest.class
                    );

            if (loginRequest == null) {
                throw new IllegalArgumentException(
                        "로그인 정보를 입력해 주세요."
                );
            }

            MemberDTO member =
                    authService.login(
                            loginRequest.getLoginId(),
                            loginRequest.getPassword()
                    );

            HttpSession session =
                    request.getSession(true);

            session.setAttribute(
                    "loginMemberId",
                    member.getMemberId()
            );

            session.setAttribute(
                    "loginMemberRole",
                    member.getRole()
            );

            gson.toJson(
                    Map.of(
                            "success", true,
                            "message", "로그인되었습니다.",
                            "memberId", member.getMemberId(),
                            "role", member.getRole()
                    ),
                    response.getWriter()
            );

        } catch (JsonSyntaxException exception) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "올바른 JSON 형식으로 요청해 주세요."
            );

        } catch (IllegalArgumentException exception) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    exception.getMessage()
            );

        } catch (AuthenticationException exception) {

            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    exception.getMessage()
            );

        } catch (RuntimeException exception) {

            sendError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "로그인 처리 중 오류가 발생했습니다."
            );
        }
    }

    private void prepare(
            HttpServletResponse response
    ) {

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );
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
                        "message",
                        message == null
                                ? "요청을 처리하지 못했습니다."
                                : message
                ),
                response.getWriter()
        );
    }

    private static class LoginRequest {

        private String loginId;
        private String password;

        public String getLoginId() {
            return loginId;
        }

        public void setLoginId(String loginId) {
            this.loginId = loginId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}