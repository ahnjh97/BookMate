package controller.dev;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.DevAuthService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/dev/auth")
public class DevAuthController extends HttpServlet {
    // TODO: 실제 로그인 기능이 병합되면 controller/dev와 DevAuthService, DevAuthDAO를 함께 삭제합니다.
    private final DevAuthService devAuthService = new DevAuthService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!prepareDevResponse(response)) return;

        HttpSession session = request.getSession(false);
        Object memberId = session == null ? null : session.getAttribute("loginMemberId");

        gson.toJson(
                Map.of(
                        "success", true,
                        "loggedIn", memberId instanceof Number,
                        "nickname", memberId instanceof Number ? "개발회원" : ""
                ),
                response.getWriter()
        );
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!prepareDevResponse(response)) return;

        try {
            long memberId = devAuthService.findOrCreateDevMember();
            HttpSession session = request.getSession(true);
            session.setAttribute("loginMemberId", memberId);
            session.setAttribute("loginNickname", "개발회원");
            session.setAttribute("loginRole", "USER");

            gson.toJson(
                    Map.of(
                            "success", true,
                            "message", "개발용 회원으로 로그인했습니다.",
                            "memberId", memberId,
                            "nickname", "개발회원"
                    ),
                    response.getWriter()
            );
        } catch (RuntimeException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            gson.toJson(
                    Map.of("success", false, "message", "개발용 로그인에 실패했습니다."),
                    response.getWriter()
            );
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!prepareDevResponse(response)) return;

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        gson.toJson(
                Map.of("success", true, "message", "로그아웃했습니다."),
                response.getWriter()
        );
    }

    private boolean prepareDevResponse(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (devAuthService.isDevMode()) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        gson.toJson(Map.of("success", false, "message", "요청한 기능을 찾을 수 없습니다."), response.getWriter());
        return false;
    }
}
