package controller.member;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import dto.bookshelf.BookshelfDTO;
import dto.bookshelf.BookshelfTierDTO;
import service.BookshelfService;

@WebServlet("/api/members/bookshelf")
public class BookshelfController extends HttpServlet {
    private final Gson gson = new Gson();
    private final BookshelfService service = new BookshelfService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            long memberId = Long.parseLong(request.getParameter("memberId"));
            BookshelfDTO bookshelf = service.findBookshelf(memberId);
            if (bookshelf == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "존재하지 않는 회원입니다.");
                return;
            }
            Set<Long> accessibleTierListIds = new LinkedHashSet<>();
            for (BookshelfTierDTO tierList : bookshelf.getTierLists()) {
                accessibleTierListIds.add(tierList.getTierListId());
            }
            request.getSession(true).setAttribute(
                    "bookshelfTierListAccess", accessibleTierListIds);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("bookshelf", bookshelf);
            gson.toJson(result, response.getWriter());
        } catch (NumberFormatException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "회원 번호가 올바르지 않습니다.");
        } catch (IllegalArgumentException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, exception.getMessage());
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        gson.toJson(Map.of("success", false, "message", message), response.getWriter());
    }
}
