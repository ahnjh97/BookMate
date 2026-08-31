package controller.post;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.PostCommentService;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet("/api/posts/comments/update")
public class PostCommentUpdateController extends HttpServlet {

    private PostCommentService postCommentService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        postCommentService = new PostCommentService();
        gson = new Gson();
    }

    /* 1. 댓글 수정 */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        setJsonResponse(response);

        Long loginMemberId = getLoginMemberId(request);

        if (loginMemberId == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요한 기능입니다."
            );
            return;
        }

        final UpdateCommentRequest updateRequest;

        try {
            updateRequest = readRequestBody(request);
        } catch (JsonSyntaxException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "요청 데이터 형식이 올바르지 않습니다."
            );
            return;
        }

        if (updateRequest == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "댓글 정보가 필요합니다."
            );
            return;
        }

        try {
            postCommentService.updateComment(
                    updateRequest.getCommentId(),
                    loginMemberId,
                    updateRequest.getContent()
            );

            gson.toJson(
                    Map.of(
                            "success", true,
                            "message", "댓글이 수정되었습니다."
                    ),
                    response.getWriter()
            );
        } catch (NoSuchElementException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    e.getMessage()
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
            e.printStackTrace();

            sendError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "댓글 수정 중 오류가 발생했습니다."
            );
        }
    }

    /* 2. 요청 JSON 읽기 */
    private UpdateCommentRequest readRequestBody(
            HttpServletRequest request
    ) throws IOException {
        request.setCharacterEncoding("UTF-8");

        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(
                    reader,
                    UpdateCommentRequest.class
            );
        }
    }

    /* 3. 로그인 회원 번호 조회 */
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

    /* 4. JSON 응답 설정 */
    private void setJsonResponse(
            HttpServletResponse response
    ) {
        response.setContentType(
                "application/json;charset=UTF-8"
        );
        response.setCharacterEncoding("UTF-8");
    }

    /* 5. 오류 응답 */
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

    /* 6. 댓글 수정 요청 데이터 */
    private static class UpdateCommentRequest {

        private long commentId;
        private String content;

        public long getCommentId() {
            return commentId;
        }

        public String getContent() {
            return content;
        }
    }
}