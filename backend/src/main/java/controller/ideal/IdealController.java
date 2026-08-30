package controller.ideal;

import com.google.gson.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import service.IdealService;

@WebServlet(urlPatterns = {"/api/worldcup/templates", "/api/worldcup/runs", "/api/worldcup/stats", "/api/worldcup/share"})
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
                ? service.findTemplates(
                    q.getParameter("keyword"), member(q), positiveLong(q.getParameter("bookId")))
                : service.findTemplate(Long.parseLong(id), member(q)));
      } else if (path.endsWith("runs"))
        ok(r, "result", service.result(Long.parseLong(q.getParameter("id")), member(q)));
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
        gson.toJson(
            Map.of(
                "success", true,
                "templateId", id,
                "message", "월드컵 템플릿이 관리자 검토 대기열에 등록되었습니다."),
            r.getWriter());
      } else if (q.getServletPath().endsWith("share")) {
        long postId = service.publishResult(b.runId, member, b.content);
        ok(r, "postId", postId);
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

  private Long positiveLong(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      long parsed = Long.parseLong(value);
      if (parsed > 0) return parsed;
    } catch (NumberFormatException ignored) {
    }
    throw new IllegalArgumentException("올바른 책 번호가 필요합니다.");
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
    String title, description, category, content;
    List<Long> bookIds;
    long templateId;
    long runId;
    int bracketSize;
    List<MatchBody> matches;
  }

  static class MatchBody {
    int roundSize, matchOrder;
    long leftBookId, rightBookId, winnerBookId;
  }
}
