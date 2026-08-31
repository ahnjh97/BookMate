package controller.admin;

import com.google.gson.Gson;
import dto.PostCommentDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AdminService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/* 관리자 댓글 관리 */
@WebServlet("/api/admin/comments")
public class AdminCommentController extends HttpServlet {

    private final Gson gson = new Gson();
    private final AdminService adminService = new AdminService();

    /* 1. 관리자 댓글 전체 목록 조회 */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            List<PostCommentDTO> comments =
                    adminService.getCommentList();

            response.setStatus(HttpServletResponse.SC_OK);

            gson.toJson(
                    Map.of(
                            "success", true,
                            "comments", comments
                    ),
                    response.getWriter()
            );

        } catch (RuntimeException e) {
            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            gson.toJson(
                    Map.of(
                            "success", false,
                            "message", "댓글 목록을 불러오지 못했습니다."
                    ),
                    response.getWriter()
            );
        }
    }

    /* 2. 관리자 댓글 삭제 */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            CommentDeleteRequest body =
                    gson.fromJson(
                            request.getReader(),
                            CommentDeleteRequest.class
                    );

            if (body == null || body.commentId <= 0) {
                response.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );

                gson.toJson(
                        Map.of(
                                "success", false,
                                "message", "올바른 댓글 번호가 필요합니다."
                        ),
                        response.getWriter()
                );

                return;
            }

            adminService.deleteCommentByAdmin(
                    body.commentId
            );

            response.setStatus(HttpServletResponse.SC_OK);

            gson.toJson(
                    Map.of(
                            "success", true,
                            "message", "댓글이 삭제되었습니다."
                    ),
                    response.getWriter()
            );

        } catch (NoSuchElementException e) {
            response.setStatus(
                    HttpServletResponse.SC_NOT_FOUND
            );

            gson.toJson(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ),
                    response.getWriter()
            );

        } catch (IllegalArgumentException e) {
            response.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );

            gson.toJson(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ),
                    response.getWriter()
            );

        } catch (RuntimeException e) {
            e.printStackTrace();

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            gson.toJson(
                    Map.of(
                            "success", false,
                            "message", "댓글 삭제에 실패했습니다."
                    ),
                    response.getWriter()
            );
        }
    }

    /* 3. 댓글 삭제 요청 데이터 */
    private static class CommentDeleteRequest {
        long commentId;
    }
}