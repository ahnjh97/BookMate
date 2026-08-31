package controller.book;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.AladinCatalogService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/books/catalog")
public class BookCatalogController extends HttpServlet {
    private final Gson gson = new Gson();
    private final AladinCatalogService catalogService = new AladinCatalogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        json(response);
        if (!isLoggedIn(request)) {
            send(response, 401, "로그인이 필요한 기능입니다.");
            return;
        }
        try {
            var books = catalogService.search(request.getParameter("query"));
            gson.toJson(Map.of("success", true, "books", books), response.getWriter());
        } catch (IllegalArgumentException exception) {
            send(response, 400, exception.getMessage());
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            send(response, 502, exception.getMessage());
        }
    }

    private boolean isLoggedIn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("loginMemberId") instanceof Number;
    }

    private void json(HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
    }

    private void send(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        gson.toJson(Map.of("success", false, "message", message), response.getWriter());
    }
}
