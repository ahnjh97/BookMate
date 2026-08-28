package controller.tier;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.TierService;

import java.io.IOException;
import java.util.*;

@WebServlet("/api/tier-templates")
public class TierTemplateController extends HttpServlet {
    private final Gson gson = new Gson();
    private final TierService service = new TierService();

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        json(response);
        try {
            String id = request.getParameter("id");
            if (id != null && memberId(request) == null) {
                error(response, 401, "로그인이 필요한 기능입니다.");
                return;
            }
            Long bookId = optionalPositiveLong(request.getParameter("bookId"), "올바른 책 번호가 필요합니다.");
            Object data = id == null ? service.findTemplates(request.getParameter("keyword"), false, memberId(request), bookId)
                    : service.findTemplate(Long.parseLong(id));
            gson.toJson(Map.of("success", true, id == null ? "templates" : "template", data), response.getWriter());
        } catch (IllegalArgumentException e) { error(response, 400, e.getMessage());
        } catch (NoSuchElementException e) { error(response, 404, e.getMessage());
        } catch (RuntimeException e) { e.printStackTrace(); error(response, 500, e.getMessage()); }
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        json(response); Long memberId = memberId(request);
        if (memberId == null) { error(response, 401, "로그인이 필요한 기능입니다."); return; }
        try {
            TemplateRequest body = gson.fromJson(request.getReader(), TemplateRequest.class);
            if (body == null) throw new IllegalArgumentException("신청 내용을 입력해 주세요.");
            long id = service.requestTemplate(memberId, body.title, body.description, body.category, body.bookIds);
            response.setStatus(201);
            gson.toJson(Map.of("success", true, "templateId", id, "message", "템플릿이 관리자 검토 대기열에 등록되었습니다."), response.getWriter());
        } catch (JsonSyntaxException | IllegalArgumentException e) { error(response, 400, e.getMessage());
        } catch (RuntimeException e) { e.printStackTrace(); error(response, 500, e.getMessage()); }
    }
    private Long memberId(HttpServletRequest request) { HttpSession s=request.getSession(false); Object id=s==null?null:s.getAttribute("loginMemberId"); return id instanceof Number n?n.longValue():null; }
    private Long optionalPositiveLong(String value, String message) {
        if (value == null || value.isBlank()) return null;
        try { long parsed = Long.parseLong(value); if (parsed > 0) return parsed; } catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException(message);
    }
    private void json(HttpServletResponse r){r.setContentType("application/json");r.setCharacterEncoding("UTF-8");}
    private void error(HttpServletResponse r,int status,String message)throws IOException{r.setStatus(status);gson.toJson(Map.of("success",false,"message",message==null?"요청을 처리하지 못했습니다.":message),r.getWriter());}
    private static class TemplateRequest { String title; String description; String category; List<Long> bookIds; }
}
