package controller.book;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.BookService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/search/suggestions")
public class SearchSuggestionController extends HttpServlet {
    private final BookService bookService = new BookService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            gson.toJson(
                    Map.of(
                            "success", true,
                            "data", bookService.findSearchSuggestions(request.getParameter("q"))
                    ),
                    response.getWriter()
            );
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            gson.toJson(Map.of("success", false, "message", exception.getMessage()), response.getWriter());
        } catch (RuntimeException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            gson.toJson(Map.of("success", false, "message", "자동완성 검색에 실패했습니다."), response.getWriter());
        }
    }
}
