package controller.book;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.BookRequestService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/books/isbn")
public class BookIsbnController extends HttpServlet {
    private final Gson gson = new Gson();
    private final BookRequestService service = new BookRequestService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            String isbn = BookRequestService.normalizeIsbn(request.getParameter("value"));
            gson.toJson(Map.of("success", true, "isbn", isbn, "available", !service.isbnExists(isbn)), response.getWriter());
        } catch (IllegalArgumentException exception) {
            response.setStatus(400);
            gson.toJson(Map.of("success", false, "message", exception.getMessage()), response.getWriter());
        }
    }
}
