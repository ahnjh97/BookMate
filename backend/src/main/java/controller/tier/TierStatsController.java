package controller.tier;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import service.TierService;

@WebServlet("/api/tier-stats")
public class TierStatsController extends HttpServlet {
  private final Gson gson = new Gson();
  private final TierService service = new TierService();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    HttpSession session = request.getSession(false);
    Object memberId = session == null ? null : session.getAttribute("loginMemberId");
    if (!(memberId instanceof Number)) {
      send(response, 401, "로그인이 필요한 기능입니다.");
      return;
    }
    try {
      long id = Long.parseLong(request.getParameter("templateId"));
      gson.toJson(
          Map.of("success", true, "stats", service.findTemplateStats(id)), response.getWriter());
    } catch (IllegalArgumentException e) {
      send(response, 400, e.getMessage() == null ? "템플릿 번호가 필요합니다." : e.getMessage());
    } catch (NoSuchElementException e) {
      send(response, 404, e.getMessage());
    } catch (RuntimeException e) {
      e.printStackTrace();
      send(response, 500, e.getMessage());
    }
  }

  private void send(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    gson.toJson(Map.of("success", false, "message", message), response.getWriter());
  }
}
