package controller.post;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.PostDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.PostLikeService;
import service.PostService;
import service.TierService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/posts/detail")
public class PostDetailController extends HttpServlet {
    private PostService postService;
    private PostLikeService postLikeService;
    private TierService tierService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        postService = new PostService();
        postLikeService = new PostLikeService();
        tierService = new TierService();
        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }

    /* GET /api/posts/detail?postId=1 - 게시글 상세 조회 */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        setJsonResponse(response);

        String postIdParameter = request.getParameter("postId");
        if (postIdParameter == null || postIdParameter.isBlank()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "게시글 번호가 필요합니다.");
            return;
        }

        final long postId;
        try {
            postId = Long.parseLong(postIdParameter);
        } catch (NumberFormatException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "게시글 번호 형식이 올바르지 않습니다.");
            return;
        }

        try {
            PostDTO post = postService.getPostDetail(postId);
            if (post == null) {
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "존재하지 않거나 삭제된 게시글입니다.");
                return;
            }

            int likeCount = postLikeService.getPostLikeCount(postId);
            boolean liked = false;

            Long loginMemberId = getLoginMemberId(request);
            if (loginMemberId != null) {
                liked = postLikeService.isPostLiked(postId, loginMemberId);
            }

            response.setStatus(HttpServletResponse.SC_OK);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("post", post);
            result.put("likeCount", likeCount);
            result.put("liked", liked);

            if (post.getTierListId() != null) {
                Map<String, Object> tierList = tierService.findTierList(post.getTierListId());
                if (tierList != null) {
                    tierList.remove("memberId");
                    tierList.remove("publishedToCommunity");
                }
                result.put("tierList", tierList);
            }

            gson.toJson(result, response.getWriter());
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "게시글을 불러오지 못했습니다.");
        }
    }

    /* 로그인 세션에서 회원 번호 조회 */
    private Long getLoginMemberId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object memberIdAttribute = session.getAttribute("loginMemberId");
        if (!(memberIdAttribute instanceof Number)) {
            return null;
        }

        return ((Number) memberIdAttribute).longValue();
    }

    /* JSON 응답 형식 설정 */
    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
    }

    /* 오류 응답 전송 */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        gson.toJson(
                Map.of(
                        "success", false,
                        "message", message
                ),
                response.getWriter()
        );
    }
}
