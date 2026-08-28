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

@WebServlet("/api/posts/create")
public class PostCreateController extends HttpServlet {
    private PostService postService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        postService = new PostService();
        gson = new Gson();
    }

    /* 게시글 등록 */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        setJsonResponse(response);

        /* 기존 세션을 가져오며 세션이 없으면 새로 생성하지 않음 */
        HttpSession session = request.getSession(false);

        if (session == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요한 기능입니다."
            );
            return;
        }

        /* 로그인 세션의 회원 번호 확인 */
        Object memberIdAttribute = session.getAttribute("loginMemberId");

        if (!(memberIdAttribute instanceof Number)) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인 회원 정보를 확인할 수 없습니다."
            );
            return;
        }

        long loginMemberId = ((Number) memberIdAttribute).longValue();

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
                    "게시글 정보가 없습니다."
            );
            return;
        }

        /* 작성자 번호는 요청값이 아니라 로그인 세션의 회원 번호 사용 */
        post.setMemberId(loginMemberId);

        try {
            long postId = postService.createPost(post);

            response.setStatus(HttpServletResponse.SC_CREATED);

            gson.toJson(
                    Map.of(
                            "success", true,
                            "message", "게시글이 등록되었습니다.",
                            "postId", postId
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
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "게시글 등록 중 오류가 발생했습니다."
            );
        }
    }

    /* 요청 JSON을 PostDTO로 변환 */
    private PostDTO readRequestBody(HttpServletRequest request) throws IOException {
        request.setCharacterEncoding("UTF-8");

        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(reader, PostDTO.class);
        }
    }

    /* JSON 응답 기본 설정 */
    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    /* 오류 응답 전송 */
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