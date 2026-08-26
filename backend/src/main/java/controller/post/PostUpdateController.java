package controller.post;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dto.PostDTO;
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

@WebServlet("/api/posts/update")
public class PostUpdateController extends HttpServlet {

    private PostService postService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        postService = new PostService();
        gson = new Gson();
    }

    /*
     * POST /api/posts/update
     *
     * 게시글 수정
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

        final PostDTO post;

        try {
            post = readRequestBody(request);

        } catch (JsonSyntaxException e) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "요청 데이터 형식이 올바르지 않습니다."
            );

            return;
        }

        if (post == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "수정할 게시글 정보가 없습니다."
            );

            return;
        }

        try {
            boolean updated =
                    postService.updatePost(
                            post,
                            loginMemberId
                    );

            if (!updated) {
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
                            "message",
                            "게시글이 수정되었습니다.",
                            "postId",
                            post.getPostId()
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
                    "게시글 수정 중 오류가 발생했습니다."
            );
        }
    }

    /*
     * 요청 본문의 JSON을 PostDTO로 변환합니다.
     */
    private PostDTO readRequestBody(
            HttpServletRequest request
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");

        try (BufferedReader reader =
                     request.getReader()) {

            return gson.fromJson(
                    reader,
                    PostDTO.class
            );
        }
    }

    /*
     * 로그인 세션에서 회원 번호를 가져옵니다.
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
}