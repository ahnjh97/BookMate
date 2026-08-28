package controller.auth;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/auth/session")
public class SessionInfoController extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        prepare(response);

        HttpSession session =
                request.getSession(false);

        if (session == null) {
            gson.toJson(
                    Map.of(
                            "success", true,
                            "loggedIn", false
                    ),
                    response.getWriter()
            );
            return;
        }

        Object memberId =
                session.getAttribute("loginMemberId");

        Object role =
                session.getAttribute("loginMemberRole");

        if (memberId == null || role == null) {
            gson.toJson(
                    Map.of(
                            "success", true,
                            "loggedIn", false
                    ),
                    response.getWriter()
            );
            return;
        }

        gson.toJson(
                Map.of(
                        "success", true,
                        "loggedIn", true,
                        "memberId", memberId,
                        "role", role
                ),
                response.getWriter()
        );

    }

    @Override
    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        prepare(response);

        HttpSession session =
                request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        gson.toJson(
                Map.of(
                        "success", true,
                        "message", "로그아웃되었습니다."
                ),
                response.getWriter()
        );
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
}