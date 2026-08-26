package controller.book;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.BookDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.BookService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/books/*")
public class BookDetailController extends HttpServlet {
    private final BookService bookService = new BookService();
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            long bookId = parseBookId(request.getPathInfo());
            BookDTO book = bookService.findBook(bookId);

            if (book == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                gson.toJson(Map.of("success", false, "message", "책을 찾을 수 없습니다."), response.getWriter());
                return;
            }

            gson.toJson(Map.of("success", true, "data", book), response.getWriter());
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            gson.toJson(Map.of("success", false, "message", exception.getMessage()), response.getWriter());
        } catch (RuntimeException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            gson.toJson(Map.of("success", false, "message", "책 정보를 불러오지 못했습니다."), response.getWriter());
        }
    }

    private long parseBookId(String pathInfo) {
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            throw new IllegalArgumentException("올바른 책 번호가 필요합니다.");
        }
        return Long.parseLong(pathInfo.substring(1));
    }
}