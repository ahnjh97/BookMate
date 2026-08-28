package controller.book;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.BookService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/books/rankings")
public class BookRankingController extends HttpServlet {
    private final BookService bookService = new BookService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            gson.toJson(Map.of(
                    "success", true,
                    "data", bookService.findBookRankings(
                            request.getParameter("genre"),
                            request.getParameter("sort"),
                            parsePositiveInt(request.getParameter("minimumRatings"), 1),
                            parsePositiveInt(request.getParameter("limit"), 20)
                    )
            ), response.getWriter());
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            gson.toJson(Map.of("success", false, "message", exception.getMessage()), response.getWriter());
        } catch (RuntimeException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            gson.toJson(Map.of("success", false, "message", "책 랭킹을 불러오지 못했습니다."), response.getWriter());
        }
    }

    private int parsePositiveInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
