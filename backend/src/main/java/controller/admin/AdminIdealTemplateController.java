package controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import service.IdealService;
import util.SessionUtil;

@WebServlet("/api/admin/worldcup-templates")
public class AdminIdealTemplateController extends HttpServlet {
  private final Gson gson = new Gson();
  private final IdealService service = new IdealService();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    json(response);
    if (!requireAdmin(request, response)) return;
    gson.toJson(
        Map.of(
            "success", true,
            "templates", service.findTemplates(request.getParameter("keyword"), null, null, true)),
        response.getWriter());
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    request.setCharacterEncoding("UTF-8");
    json(response);
    if (!requireAdmin(request, response)) return;
    try {
      ReviewRequest body = gson.fromJson(request.getReader(), ReviewRequest.class);
      service.reviewTemplate(
          body.templateId, SessionUtil.memberId(request), body.approved, body.reason);
      gson.toJson(
          Map.of(
              "success", true,
              "message", body.approved ? "월드컵 템플릿을 승인했습니다." : "월드컵 템플릿을 반려했습니다."),
          response.getWriter());
    } catch (IllegalArgumentException e) {
      error(response, 400, e.getMessage());
    } catch (NoSuchElementException e) {
      error(response, 404, e.getMessage());
    } catch (RuntimeException e) {
      e.printStackTrace();
      error(response, 500, e.getMessage());
    }
  }

  private void json(HttpServletResponse response) {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
  }

  private boolean requireAdmin(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (SessionUtil.memberId(request) == null) {
      error(response, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요한 기능입니다.");
      return false;
    }
    if (!"ADMIN".equals(SessionUtil.role(request))) {
      error(response, HttpServletResponse.SC_FORBIDDEN, "관리자 권한이 필요한 기능입니다.");
      return false;
    }
    return true;
  }

  private void error(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    gson.toJson(Map.of("success", false, "message", message), response.getWriter());
  }

  private static class ReviewRequest {
    long templateId;
    boolean approved;
    String reason;
  }
}
