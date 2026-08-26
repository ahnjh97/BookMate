package controller.post;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.PostService;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet("/api/posts/delete")
public class PostDeleteController extends HttpServlet {

    private PostService postService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        postService = new PostService();
        gson = new Gson();
    }

    /*
     * POST /api/posts/delete
     *
     * 작성자 게시글 소프트 삭제
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        setJsonResponse(response);

        Long loginMemberId =
                getLoginMemberId(request);

        if (loginMemberId == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요한 기능입니다."
            );

            return;
        }

        final DeletePostRequest deleteRequest;

        try {
            deleteRequest =
                    readRequestBody(request);

        } catch (JsonSyntaxException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "요청 데이터 형식이 올바르지 않습니다."
            );

            return;
        }

        if (deleteRequest == null
                || deleteRequest.getPostId() <= 0) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "올바른 게시글 번호가 필요합니다."
            );

            return;
        }

        try {
            boolean deleted =
                    postService.deletePostByWriter(
                            deleteRequest.getPostId(),
                            loginMemberId
                    );

            if (!deleted) {
                sendError(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        "존재하지 않거나 이미 삭제된 게시글입니다."
                );

                return;
            }

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            gson.toJson(
                    Map.of(
                            "success", true,
                            "message",
                            "게시글이 삭제되었습니다.",
                            "postId",
                            deleteRequest.getPostId()
                    ),
                    response.getWriter()
            );

            } catch (SecurityException e) {
                sendError(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        e.getMessage()
                );

            } catch (NoSuchElementException e) {
                sendError(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        e.getMessage()
                );

            } catch (IllegalArgumentException e) {
                sendError(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        e.getMessage()
                );

            } catch (RuntimeException e) {
                sendError(
                        response,
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "게시글 삭제 중 오류가 발생했습니다."
                );
        }
    }

    private DeletePostRequest readRequestBody(
            HttpServletRequest request
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");

        try (BufferedReader reader =
                     request.getReader()) {

            return gson.fromJson(
                    reader,
                    DeletePostRequest.class
            );
        }
    }

    /*
     * 로그인 세션에서 회원 번호 조회
     */
    private Long getLoginMemberId(
            HttpServletRequest request
    ) {
        HttpSession session =
                request.getSession(false);

        if (session == null) {
            return null;
        }

        Object memberIdAttribute =
                session.getAttribute("loginMemberId");

        if (!(memberIdAttribute instanceof Number)) {
            return null;
        }

        return ((Number) memberIdAttribute)
                .longValue();
    }

    private void setJsonResponse(
            HttpServletResponse response
    ) {
        response.setContentType(
                "application/json"
        );
        response.setCharacterEncoding("UTF-8");
    }

    private void sendError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setStatus(status);

        gson.toJson(
                Map.of(
                        "success", false,
                        "message", message
                ),
                response.getWriter()
        );
    }

    /*
     * 삭제 요청 JSON을 받는 내부 DTO
     */
    private static class DeletePostRequest {

        private long postId;

        public DeletePostRequest() {
        }

        public long getPostId() {
            return postId;
        }

        public void setPostId(long postId) {
            this.postId = postId;
        }
    }
}