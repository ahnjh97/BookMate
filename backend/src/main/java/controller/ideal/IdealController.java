package controller.ideal;

import com.google.gson.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import service.IdealService;

@WebServlet(urlPatterns = {"/api/worldcup/templates", "/api/worldcup/runs", "/api/worldcup/stats"})
public class IdealController extends HttpServlet {
  private final Gson gson = new Gson();
  private final IdealService service = new IdealService();

  protected void doGet(HttpServletRequest q, HttpServletResponse r) throws IOException {
    json(r);
    try {
      String path = q.getServletPath();
      if (path.endsWith("templates")) {
        String id = q.getParameter("id");
        ok(
            r,
            id == null ? "templates" : "template",
            id == null
                ? service.findTemplates(q.getParameter("keyword"), member(q))
                : service.findTemplate(Long.parseLong(id), member(q)));
      } else if (path.endsWith("runs"))
        ok(r, "result", service.result(Long.parseLong(q.getParameter("id"))));
      else ok(r, "stats", service.stats(Long.parseLong(q.getParameter("templateId"))));
    } catch (NumberFormatException e) {
      err(r, 400, "올바른 번호가 필요합니다.");
    } catch (IllegalArgumentException e) {
      err(r, 400, e.getMessage());
    } catch (NoSuchElementException e) {
      err(r, 404, e.getMessage());
    } catch (RuntimeException e) {
      e.printStackTrace();
      err(r, 500, e.getMessage());
    }
  }

  protected void doPost(HttpServletRequest q, HttpServletResponse r) throws IOException {
    q.setCharacterEncoding("UTF-8");
    json(r);
    Long member = member(q);
    if (member == null) {
      err(r, 401, "로그인이 필요한 기능입니다.");
      return;
    }
    try {
      Body b = gson.fromJson(q.getReader(), Body.class);
      if (q.getServletPath().endsWith("templates")) {
        long id = service.createTemplate(member, b.title, b.description, b.category, b.bookIds);
        r.setStatus(201);
        ok(r, "templateId", id);
      } else {
        List<IdealService.Match> ms =
            (b.matches == null ? List.<MatchBody>of() : b.matches)
                .stream()
                    .map(
                        m ->
                            new IdealService.Match(
                                m.roundSize,
                                m.matchOrder,
                                m.leftBookId,
                                m.rightBookId,
                                m.winnerBookId))
                    .toList();
        long id = service.saveRun(member, b.templateId, b.bracketSize, ms);
        r.setStatus(201);
        ok(r, "runId", id);
      }
    } catch (JsonSyntaxException | IllegalArgumentException e) {
      err(r, 400, e.getMessage());
    } catch (RuntimeException e) {
      e.printStackTrace();
      err(r, 500, e.getMessage());
    }
  }

  private Long member(HttpServletRequest q) {
    HttpSession s = q.getSession(false);
    Object o = s == null ? null : s.getAttribute("loginMemberId");
    return o instanceof Number n ? n.longValue() : null;
  }

  private void json(HttpServletResponse r) {
    r.setContentType("application/json");
    r.setCharacterEncoding("UTF-8");
  }

  private void ok(HttpServletResponse r, String k, Object v) throws IOException {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("success", true);
    m.put(k, v);
    gson.toJson(m, r.getWriter());
  }

  private void err(HttpServletResponse r, int s, String m) throws IOException {
    r.setStatus(s);
    gson.toJson(
        Map.of("success", false, "message", m == null ? "요청을 처리하지 못했습니다." : m), r.getWriter());
  }

  static class Body {
    String title, description, category;
    List<Long> bookIds;
    long templateId;
    int bracketSize;
    List<MatchBody> matches;
  }

  static class MatchBody {
    int roundSize, matchOrder;
    long leftBookId, rightBookId, winnerBookId;
  }
}
