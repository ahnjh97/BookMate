package controller.member;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.BookshelfService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/members/bookshelf/ratings")
public class BookshelfRatingController extends HttpServlet {
    private final Gson gson = new Gson();
    private final BookshelfService service = new BookshelfService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            long memberId = Long.parseLong(request.getParameter("memberId"));
            String rawScore = request.getParameter("score");
            Integer score = rawScore == null || rawScore.isBlank() ? null : Integer.valueOf(rawScore);
            int page = positive(request.getParameter("page"), 1);
            int size = positive(request.getParameter("size"), 10);
            gson.toJson(Map.of("success", true, "data", service.findRatingPage(memberId, score, page, size)), response.getWriter());
        } catch (NumberFormatException exception) {
            response.setStatus(400);
            gson.toJson(Map.of("success", false, "message", "조회 조건이 올바르지 않습니다."), response.getWriter());
        } catch (IllegalArgumentException exception) {
            response.setStatus(400);
            gson.toJson(Map.of("success", false, "message", exception.getMessage()), response.getWriter());
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            response.setStatus(500);
            gson.toJson(Map.of("success", false, "message", exception.getMessage()), response.getWriter());
        }
    }

    private int positive(String value, int fallback) {
        try { int parsed = Integer.parseInt(value); return parsed > 0 ? parsed : fallback; }
        catch (RuntimeException exception) { return fallback; }
    }
}
