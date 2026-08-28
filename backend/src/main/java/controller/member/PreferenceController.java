package controller.member;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import service.PreferenceService;

@WebServlet("/api/preferences/similar")
public class PreferenceController extends HttpServlet {
    private final Gson gson = new Gson();
    private final PreferenceService service = new PreferenceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("loginMemberId");
        if (!(value instanceof Number memberId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            gson.toJson(Map.of("success", false, "message", "로그인이 필요합니다."), response.getWriter());
            return;
        }

        try {
            int limit = parseLimit(request.getParameter("limit"));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("users", service.findSimilarMembers(memberId.longValue(), limit));
            gson.toJson(result, response.getWriter());
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            gson.toJson(Map.of("success", false, "message", exception.getMessage()), response.getWriter());
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            gson.toJson(Map.of("success", false, "message", exception.getMessage()), response.getWriter());
        }
    }

    private int parseLimit(String value) {
        if (value == null || value.isBlank()) return 3;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("조회 개수가 올바르지 않습니다.");
        }
    }
}
