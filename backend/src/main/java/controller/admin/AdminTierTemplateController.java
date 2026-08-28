package controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import service.TierService;

@WebServlet("/api/admin/tier-templates")
public class AdminTierTemplateController extends HttpServlet {
  private final Gson gson = new Gson();
  private final TierService service = new TierService();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    json(res);
    gson.toJson(
        Map.of(
            "success", true, "templates", service.findTemplates(req.getParameter("keyword"), true)),
        res.getWriter());
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    json(res);
    try {
      ReviewRequest body = gson.fromJson(req.getReader(), ReviewRequest.class);
      HttpSession s = req.getSession(false);
      Object raw = s.getAttribute("loginMemberId");
      if (!(raw instanceof Number n)) throw new IllegalArgumentException("관리자 정보를 확인할 수 없습니다.");
      service.reviewTemplate(body.templateId, n.longValue(), body.approved, body.reason);
      gson.toJson(
          Map.of("success", true, "message", body.approved ? "템플릿을 승인했습니다." : "템플릿을 반려했습니다."),
          res.getWriter());
    } catch (IllegalArgumentException e) {
      send(res, 400, e.getMessage());
    } catch (NoSuchElementException e) {
      send(res, 404, e.getMessage());
    } catch (RuntimeException e) {
      e.printStackTrace();
      send(res, 500, e.getMessage());
    }
  }

  private void json(HttpServletResponse r) {
    r.setContentType("application/json");
    r.setCharacterEncoding("UTF-8");
  }

  private void send(HttpServletResponse r, int s, String m) throws IOException {
    r.setStatus(s);
    gson.toJson(Map.of("success", false, "message", m), r.getWriter());
  }

  private static class ReviewRequest {
    long templateId;
    boolean approved;
    String reason;
  }
}
