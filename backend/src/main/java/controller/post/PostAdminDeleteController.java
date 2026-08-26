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

@WebServlet("/api/admin/posts/delete")
public class PostAdminDeleteController
        extends HttpServlet {

    private PostService postService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        postService = new PostService();
        gson = new Gson();
    }

    /*
     * POST /api/admin/posts/delete
     *
     * 관리자 게시글 소프트 삭제
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        setJsonResponse(response);

        HttpSession session =
                request.getSession(false);

        if (session == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요한 기능입니다."
            );

            return;
        }

        /*
         * 로그인 기능에서 사용하는 세션 속성명이 다르면
         * loginMemberRole을 실제 이름으로 바꿔야 합니다.
         */
        Object roleAttribute =
                session.getAttribute("loginMemberRole");

        String loginMemberRole =
                roleAttribute == null
                        ? null
                        : roleAttribute.toString();

        final DeleteRequest deleteRequest;

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
                    postService.deletePostByAdmin(
                            deleteRequest.getPostId(),
                            loginMemberRole
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
                            "관리자 권한으로 게시글을 삭제했습니다.",
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

        } catch (IllegalArgumentException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage()
            );

        } catch (RuntimeException e) {
            sendError(
                    response,
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR,
                    "게시글 삭제 중 오류가 발생했습니다."
            );
        }
    }

    private DeleteRequest readRequestBody(
            HttpServletRequest request
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");

        try (BufferedReader reader =
                     request.getReader()) {

            return gson.fromJson(
                    reader,
                    DeleteRequest.class
            );
        }
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

    private static class DeleteRequest {

        private long postId;

        public DeleteRequest() {
        }

        public long getPostId() {
            return postId;
        }

        public void setPostId(long postId) {
            this.postId = postId;
        }
    }
}