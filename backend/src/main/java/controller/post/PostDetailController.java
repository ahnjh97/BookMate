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
import java.util.Map;

@WebServlet("/api/posts/detail")
public class PostDetailController extends HttpServlet {

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
     * GET /api/posts/detail?postId=1
     *
     * 게시글 상세 조회
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        setJsonResponse(response);

        String postIdParameter =
                request.getParameter("postId");

        if (postIdParameter == null
                || postIdParameter.isBlank()) {

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
            PostDTO post =
                    postService.getPostDetail(postId);

            if (post == null) {
                sendError(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        "존재하지 않거나 삭제된 게시글입니다."
                );

                return;
            }

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            gson.toJson(
                    Map.of(
                            "success", true,
                            "post", post
                    ),
                    response.getWriter()
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
                    "게시글을 불러오지 못했습니다."
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
}