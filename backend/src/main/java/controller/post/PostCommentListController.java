package controller.post;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.PostCommentDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.PostCommentService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet("/api/posts/comments")
public class PostCommentListController extends HttpServlet {

    private PostCommentService postCommentService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        postCommentService = new PostCommentService();

        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }

    /* 1. 게시글 댓글 목록 조회 */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        setJsonResponse(response);

        String postIdParameter = request.getParameter("postId");

        if (postIdParameter == null || postIdParameter.isBlank()) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시글 번호가 필요합니다."
            );
            return;
        }

        final long postId;

        try {
            postId = Long.parseLong(postIdParameter);
        } catch (NumberFormatException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시글 번호 형식이 올바르지 않습니다."
            );
            return;
        }

        try {
            List<PostCommentDTO> commentList =
                    postCommentService.getCommentList(postId);

            response.setStatus(HttpServletResponse.SC_OK);

            gson.toJson(
                    Map.of(
                            "success", true,
                            "comments", commentList
                    ),
                    response.getWriter()
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
                    "댓글 목록을 불러오지 못했습니다."
            );
        }
    }

    /* 2. JSON 응답 설정 */
    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
    }

    /* 3. 오류 응답 */
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
}