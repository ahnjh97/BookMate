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

    /*
     * POST /api/posts/create
     *
     * 게시글 등록
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        setJsonResponse(response);

        /*
         * 기존 세션을 가져옵니다.
         * 세션이 없다고 새로운 세션을 만들지는 않습니다.
         */
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
         * 로그인 성공 시 세션에 저장했다고 가정한 값입니다.
         *
         * 로그인 기능에서 사용하는 속성명이 다르면
         * "loginMemberId"를 실제 이름으로 바꿔야 합니다.
         */
        Object memberIdAttribute =
                session.getAttribute("loginMemberId");

        if (!(memberIdAttribute instanceof Number)) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인 회원 정보를 확인할 수 없습니다."
            );

            return;
        }

        long loginMemberId =
                ((Number) memberIdAttribute).longValue();

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

        /*
         * 작성자 번호는 브라우저에서 받은 값을 사용하지 않고
         * 로그인 세션의 회원 번호를 사용합니다.
         */
        post.setMemberId(loginMemberId);

        try {
            long postId =
                    postService.createPost(post);

            response.setStatus(
                    HttpServletResponse.SC_CREATED
            );

            gson.toJson(
                    Map.of(
                            "success", true,
                            "message",
                            "게시글이 등록되었습니다.",
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
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR,
                    "게시글 등록 중 오류가 발생했습니다."
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