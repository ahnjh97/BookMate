package controller.tier;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import service.TierService;

@WebServlet("/api/tier-lists")
public class TierListController extends HttpServlet {
  private final Gson gson = new Gson();
  private final TierService service = new TierService();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String tierListValue = request.getParameter("tierListId");
    if (tierListValue != null && !tierListValue.isBlank()) {
      try {
        long tierListId = Long.parseLong(tierListValue);
        Map<String, Object> tierList = service.findTierList(tierListId);
        if (tierList == null) {
          send(response, 404, "티어리스트를 찾을 수 없습니다.");
          return;
        }
        HttpSession session = request.getSession(false);
        Object loginMemberId = session == null ? null : session.getAttribute("loginMemberId");
        Object accessValue = session == null ? null : session.getAttribute("bookshelfTierListAccess");
        boolean isOwner = loginMemberId instanceof Number memberId
            && memberId.longValue() == ((Number) tierList.get("memberId")).longValue();
        boolean visitedBookshelf = accessValue instanceof Set<?> access
            && access.contains(tierListId);
        boolean publishedToCommunity = Boolean.TRUE.equals(tierList.get("publishedToCommunity"));
        if (!isOwner && !visitedBookshelf && !publishedToCommunity) {
          send(response, 403, "회원 책장을 통해 접근해 주세요.");
          return;
        }
        tierList.remove("memberId");
        tierList.remove("publishedToCommunity");
        gson.toJson(Map.of("success", true, "tierList", tierList), response.getWriter());
      } catch (NumberFormatException e) {
        send(response, 400, "올바른 티어리스트 번호가 필요합니다.");
      } catch (RuntimeException e) {
        e.printStackTrace();
        send(response, 500, e.getMessage());
      }
      return;
    }
    HttpSession session = request.getSession(false);
    Object raw = session == null ? null : session.getAttribute("loginMemberId");
    if (!(raw instanceof Number n)) {
      send(response, 401, "로그인이 필요한 기능입니다.");
      return;
    }
    try {
      long templateId = Long.parseLong(request.getParameter("templateId"));
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("success", true);
      result.put("tierList", service.findLatestTierList(n.longValue(), templateId));
      gson.toJson(result, response.getWriter());
    } catch (NumberFormatException e) {
      send(response, 400, "올바른 템플릿 번호가 필요합니다.");
    } catch (IllegalArgumentException e) {
      send(response, 400, e.getMessage());
    } catch (RuntimeException e) {
      e.printStackTrace();
      send(response, 500, e.getMessage());
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    request.setCharacterEncoding("UTF-8");
    HttpSession session = request.getSession(false);
    Object raw = session == null ? null : session.getAttribute("loginMemberId");
    if (!(raw instanceof Number n)) {
      send(response, 401, "로그인이 필요한 기능입니다.");
      return;
    }
    try {
      SaveRequest body = gson.fromJson(request.getReader(), SaveRequest.class);
      if (body == null) throw new IllegalArgumentException("저장할 내용을 입력해 주세요.");
      List<TierService.Placement> placements =
          (body.placements == null ? List.<PlacementRequest>of() : body.placements)
              .stream()
                  .map(
                      p ->
                          new TierService.Placement(
                              p.bookId, p.grade == null ? null : p.grade.toUpperCase()))
                  .toList();
      long id =
          service.saveTierList(
              n.longValue(),
              body.templateId,
              body.description,
              body.publishToCommunity,
              placements);
      response.setStatus(201);
      gson.toJson(
          Map.of(
              "success",
              true,
              "tierListId",
              id,
              "message",
              body.publishToCommunity
                  ? "티어리스트를 저장하고 커뮤니티에 게시했습니다."
                  : "티어리스트를 저장했습니다."),
          response.getWriter());
    } catch (IllegalArgumentException e) {
      send(response, 400, e.getMessage());
    } catch (RuntimeException e) {
      e.printStackTrace();
      send(response, 500, e.getMessage());
    }
  }

  private void send(HttpServletResponse r, int status, String message) throws IOException {
    r.setStatus(status);
    gson.toJson(Map.of("success", false, "message", message), r.getWriter());
  }

  private static class SaveRequest {
    long templateId;
    String description;
    boolean publishToCommunity;
    List<PlacementRequest> placements;
  }

  private static class PlacementRequest {
    long bookId;
    String grade;
  }
}
