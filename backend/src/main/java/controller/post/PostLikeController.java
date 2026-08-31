package controller.post;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.PostLikeService;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet("/api/posts/like")
public class PostLikeController extends HttpServlet {
    private PostLikeService postLikeService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        postLikeService = new PostLikeService();
        gson = new Gson();
    }

    /* POST /api/posts/like - 게시글 좋아요 등록 또는 취소 */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        setJsonResponse(response);

        Long loginMemberId = getLoginMemberId(request);
        if (loginMemberId == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요한 기능입니다.");
            return;
        }

        final LikePostRequest likeRequest;

        try {
            likeRequest = readRequestBody(request);
        } catch (JsonSyntaxException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "요청 데이터 형식이 올바르지 않습니다.");
            return;
        }

        if (likeRequest == null || likeRequest.getPostId() <= 0) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "올바른 게시글 번호가 필요합니다.");
            return;
        }

        try {
            PostLikeService.LikeResult result =
                    postLikeService.togglePostLike(likeRequest.getPostId(), loginMemberId);

            response.setStatus(HttpServletResponse.SC_OK);

            gson.toJson(
                    Map.of(
                            "success", true,
                            "liked", result.liked(),
                            "likeCount", result.likeCount(),
                            "postId", likeRequest.getPostId()
                    ),
                    response.getWriter()
            );
        } catch (NoSuchElementException e) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "게시글 좋아요 처리 중 오류가 발생했습니다.");
        }
    }

    /* 요청 본문의 JSON을 좋아요 요청 객체로 변환 */
    private LikePostRequest readRequestBody(HttpServletRequest request) throws IOException {
        request.setCharacterEncoding("UTF-8");

        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(reader, LikePostRequest.class);
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
        response.setContentType("application/json");
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

    /* 좋아요 요청 JSON을 받는 내부 DTO */
    private static class LikePostRequest {
        private long postId;

        public long getPostId() {
            return postId;
        }
    }
}