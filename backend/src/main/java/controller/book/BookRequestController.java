package controller.book;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import dto.CatalogBookDTO;
import service.AladinCatalogService;
import service.BookRequestService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/books/requests")
public class BookRequestController extends HttpServlet {
    private final Gson gson = new Gson();
    private final BookRequestService service = new BookRequestService();
    private final AladinCatalogService catalogService = new AladinCatalogService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        try {
            HttpSession session = request.getSession(false);
            Object rawMemberId = session == null ? null : session.getAttribute("loginMemberId");
            if (!(rawMemberId instanceof Number memberId)) {
                response.setStatus(401);
                gson.toJson(Map.of("success", false, "message", "로그인이 필요한 기능입니다."), response.getWriter());
                return;
            }
            RequestBody body = gson.fromJson(request.getReader(), RequestBody.class);
            if (body == null) throw new IllegalArgumentException("신청할 책을 선택해 주세요.");
            CatalogBookDTO book = catalogService.findCompleteBook(body.isbn);
            long requestId = service.request(memberId.longValue(), book.isbn(), book.title(), book.authorName(),
                    book.genre(), book.publisher(), book.publishedDate(), book.description(), book.imageUrl(),
                    book.sourceUrl());
            gson.toJson(Map.of("success", true, "requestId", requestId,
                    "message", "책 등록 신청이 완료되었습니다. 관리자 승인 후 등록됩니다."), response.getWriter());
        } catch (IllegalArgumentException exception) {
            response.setStatus(400);
            gson.toJson(Map.of("success", false, "message", exception.getMessage()), response.getWriter());
        } catch (Exception exception) {
            exception.printStackTrace();
            response.setStatus(500);
            gson.toJson(Map.of("success", false, "message", "책 등록 신청을 처리하지 못했습니다."), response.getWriter());
        }
    }

    private static class RequestBody { String isbn; }
}
