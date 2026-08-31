package controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.BookRequestService;

import java.io.IOException;
import java.util.*;

@WebServlet("/api/admin/book-requests")
public class AdminBookRequestController extends HttpServlet {
    private final Gson gson = new Gson();
    private final BookRequestService service = new BookRequestService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        json(response);
        gson.toJson(Map.of("success", true, "requests", service.findRequests()), response.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        json(response);
        try {
            Review body = gson.fromJson(request.getReader(), Review.class);
            Object raw = request.getSession(false).getAttribute("loginMemberId");
            if (!(raw instanceof Number adminId)) throw new IllegalArgumentException("관리자 정보를 확인할 수 없습니다.");
            service.review(body.requestId, adminId.longValue(), body.approved, body.reason);
            gson.toJson(Map.of("success", true, "message", body.approved ? "책을 등록했습니다." : "책 등록 신청을 반려했습니다."), response.getWriter());
        } catch (IllegalArgumentException exception) {
            send(response, 400, exception.getMessage());
        } catch (NoSuchElementException exception) {
            send(response, 404, exception.getMessage());
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            send(response, 500, exception.getMessage());
        }
    }

    private void json(HttpServletResponse response) { response.setContentType("application/json;charset=UTF-8"); }
    private void send(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        gson.toJson(Map.of("success", false, "message", message), response.getWriter());
    }
    private static class Review { long requestId; boolean approved; String reason; }
}
