package controller.admin;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.AdminService;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet("/api/admin/posts/pin")
public class AdminPostPinController
        extends HttpServlet {

    private AdminService adminService;
    private Gson gson;

    @Override
    public void init() throws ServletException {

        adminService = new AdminService();
        gson = new Gson();
    }


    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        setJsonResponse(response);

        final PinRequest pinRequest;

        try {

            pinRequest =
                    readRequestBody(request);

        } catch (JsonSyntaxException e) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "요청 데이터 형식이 올바르지 않습니다."
            );

            return;
        }

        if (pinRequest == null) {

            sendError(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "게시글 정보가 없습니다."
            );

            return;
        }

        try {

            adminService.changePostPin(
                    pinRequest.getPostId(),
                    pinRequest.isPinned()
            );

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            gson.toJson(
                    Map.of(
                            "success", true,
                            "message",
                            pinRequest.isPinned()
                                    ? "게시글을 상단에 고정했습니다."
                                    : "게시글 고정을 해제했습니다.",
                            "postId",
                            pinRequest.getPostId()
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
                    HttpServletResponse
                            .SC_INTERNAL_SERVER_ERROR,
                    "게시글 상태 변경 중 오류가 발생했습니다."
            );
        }
    }


    private PinRequest readRequestBody(
            HttpServletRequest request
    ) throws IOException {

        request.setCharacterEncoding("UTF-8");

        try (BufferedReader reader =
                     request.getReader()) {

            return gson.fromJson(
                    reader,
                    PinRequest.class
            );
        }
    }


    private void setJsonResponse(
            HttpServletResponse response
    ) {

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );
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


    private static class PinRequest {

        private long postId;
        private boolean pinned;

        public long getPostId() {
            return postId;
        }

        public void setPostId(long postId) {
            this.postId = postId;
        }

        public boolean isPinned() {
            return pinned;
        }

        public void setPinned(boolean pinned) {
            this.pinned = pinned;
        }
    }
}