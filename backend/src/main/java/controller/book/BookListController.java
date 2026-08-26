package controller.book;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.BookService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/books")
public class BookListController extends HttpServlet {
    private final BookService bookService = new BookService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        setJsonResponse(response);

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("data", bookService.findBooks(
                    request.getParameter("keyword"),
                    request.getParameter("genre"),
                    parsePositiveInt(request.getParameter("page"), 1),
                    parsePositiveInt(request.getParameter("size"), 100)
            ));
            gson.toJson(body, response.getWriter());
        } catch (RuntimeException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            gson.toJson(Map.of("success", false, "message", "책 목록을 불러오지 못했습니다."), response.getWriter());
        }
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
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
