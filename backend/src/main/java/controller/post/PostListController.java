package controller.post;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.PostDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.PostService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/posts")
public class PostListController extends HttpServlet {

    private PostService postService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        postService = new PostService();

        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }

    /*
     * GET /api/posts
     *
     * 게시글 목록 조회
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        setJsonResponse(response);

        try {
            String rawPage = request.getParameter("page");
            if (rawPage != null) {
                int requestedPageSize = parsePositive(request.getParameter("size"), 10);
                var pinnedPosts = postService.getPinnedPosts(request.getParameter("category"),
                        request.getParameter("genre"), request.getParameter("keyword"), request.getParameter("sort"));
                int regularPageSize = Math.max(1, requestedPageSize - pinnedPosts.size());
                var page = postService.getPostPage(request.getParameter("category"), request.getParameter("genre"),
                        request.getParameter("keyword"), request.getParameter("sort"), parsePositive(rawPage, 1),
                        regularPageSize);
                response.setStatus(HttpServletResponse.SC_OK);
                gson.toJson(Map.of("success", true, "posts", page.posts(), "pinnedPosts", pinnedPosts,
                        "page", page.page(), "pageSize", requestedPageSize,
                        "totalCount", page.totalCount() + pinnedPosts.size(),
                        "paginationTotalCount", page.totalCount() + pinnedPosts.size(),
                        "totalPages", page.totalPages()),
                        response.getWriter());
                return;
            }
            List<PostDTO> postList =
                    postService.getPostList();

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            gson.toJson(
                    Map.of(
                            "success", true,
                            "posts", postList
                    ),
                    response.getWriter()
            );

        } catch (RuntimeException e) {
            e.printStackTrace();
            response.setStatus(
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR
            );

            gson.toJson(
                    Map.of(
                            "success", false,
                            "message",
                            "게시글 목록을 불러오지 못했습니다."
                    ),
                    response.getWriter()
            );
        }
    }

    private int parsePositive(String value, int fallback) {
        try { int parsed = Integer.parseInt(value); return parsed > 0 ? parsed : fallback; }
        catch (RuntimeException exception) { return fallback; }
    }

    private void setJsonResponse(
            HttpServletResponse response
    ) {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
    }
}
