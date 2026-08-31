package controller.admin;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import service.IdealService;

@WebServlet("/api/admin/worldcup-templates")
public class AdminIdealTemplateController extends HttpServlet {
  private final Gson gson = new Gson();
  private final IdealService service = new IdealService();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    json(response);
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
    try {
      ReviewRequest body = gson.fromJson(request.getReader(), ReviewRequest.class);
      HttpSession session = request.getSession(false);
      Object rawMemberId = session == null ? null : session.getAttribute("loginMemberId");
      if (!(rawMemberId instanceof Number memberId))
        throw new IllegalArgumentException("관리자 정보를 확인할 수 없습니다.");
      service.reviewTemplate(body.templateId, memberId.longValue(), body.approved, body.reason);
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
