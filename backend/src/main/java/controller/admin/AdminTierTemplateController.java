package controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import service.TierService;
import util.SessionUtil;

@WebServlet("/api/admin/tier-templates")
public class AdminTierTemplateController extends HttpServlet {
  private final Gson gson = new Gson();
  private final TierService service = new TierService();

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    json(res);
    if (!requireAdmin(req, res)) return;
    gson.toJson(
        Map.of(
            "success", true, "templates", service.findTemplates(req.getParameter("keyword"), true)),
        res.getWriter());
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    json(res);
    if (!requireAdmin(req, res)) return;
    try {
      ReviewRequest body = gson.fromJson(req.getReader(), ReviewRequest.class);
      service.reviewTemplate(body.templateId, SessionUtil.memberId(req), body.approved, body.reason);
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

  private boolean requireAdmin(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (SessionUtil.memberId(request) == null) {
      send(response, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요한 기능입니다.");
      return false;
    }
    if (!"ADMIN".equals(SessionUtil.role(request))) {
      send(response, HttpServletResponse.SC_FORBIDDEN, "관리자 권한이 필요한 기능입니다.");
      return false;
    }
    return true;
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
