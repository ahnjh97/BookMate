package controller.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.PostPageDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.PostService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/admin/posts")
public class AdminPostListController extends HttpServlet {
    private final PostService service = new PostService();
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            PostPageDTO page = service.getAdminPostPage(
                    parsePositive(request.getParameter("page"), 1),
                    parsePositive(request.getParameter("size"), 10),
                    request.getParameter("filter"));
            gson.toJson(Map.of("success", true, "posts", page.posts(), "page", page.page(),
                    "pageSize", page.pageSize(), "totalCount", page.totalCount(),
                    "totalPages", page.totalPages()), response.getWriter());
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            gson.toJson(Map.of("success", false, "message", "관리자 게시글 목록을 불러오지 못했습니다."),
                    response.getWriter());
        }
    }

    private int parsePositive(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
