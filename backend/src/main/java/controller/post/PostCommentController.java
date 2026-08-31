package controller.post;

import com.google.gson.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.PostCommentService;

import java.io.IOException;
import java.util.*;

@WebServlet("/api/posts/comments")
public class PostCommentController extends HttpServlet {
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    private final PostCommentService service = new PostCommentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        json(response);
        try {
            long postId = Long.parseLong(request.getParameter("postId"));
            List<Map<String, Object>> comments = service.findComments(postId);
            long activeCount = comments.stream().filter(comment -> "ACTIVE".equals(comment.get("status"))).count();
            gson.toJson(Map.of("success", true, "comments", comments, "count", activeCount), response.getWriter());
        } catch (IllegalArgumentException exception) {
            send(response, 400, exception.getMessage() == null ? "게시글 번호가 올바르지 않습니다." : exception.getMessage());
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            send(response, 500, exception.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        json(response);
        try {
            CommentRequest body = read(request);
            long memberId = memberId(request);
            long commentId = service.createComment(body.postId, memberId, body.parentCommentId, body.content);
            response.setStatus(201);
            gson.toJson(Map.of("success", true, "commentId", commentId, "message", "댓글을 등록했습니다."), response.getWriter());
        } catch (SecurityException exception) { send(response, 401, exception.getMessage()); }
        catch (IllegalArgumentException exception) { send(response, 400, exception.getMessage()); }
        catch (NoSuchElementException exception) { send(response, 404, exception.getMessage()); }
        catch (RuntimeException exception) { exception.printStackTrace(); send(response, 500, exception.getMessage()); }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        json(response);
        try {
            CommentRequest body = read(request);
            service.updateComment(body.commentId, memberId(request), body.content);
            gson.toJson(Map.of("success", true, "message", "댓글을 수정했습니다."), response.getWriter());
        } catch (SecurityException exception) { send(response, 401, exception.getMessage()); }
        catch (IllegalArgumentException exception) { send(response, 400, exception.getMessage()); }
        catch (NoSuchElementException exception) { send(response, 404, exception.getMessage()); }
        catch (RuntimeException exception) { exception.printStackTrace(); send(response, 500, exception.getMessage()); }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        json(response);
        try {
            CommentRequest body = read(request);
            HttpSession session = request.getSession(false);
            long memberId = memberId(request);
            String role = String.valueOf(session.getAttribute("loginRole"));
            service.deleteComment(body.commentId, memberId, role);
            gson.toJson(Map.of("success", true, "message", "댓글을 삭제했습니다."), response.getWriter());
        } catch (SecurityException exception) { send(response, 401, exception.getMessage()); }
        catch (IllegalArgumentException exception) { send(response, 400, exception.getMessage()); }
        catch (NoSuchElementException exception) { send(response, 404, exception.getMessage()); }
        catch (RuntimeException exception) { exception.printStackTrace(); send(response, 500, exception.getMessage()); }
    }

    private CommentRequest read(HttpServletRequest request) throws IOException {
        CommentRequest body = gson.fromJson(request.getReader(), CommentRequest.class);
        if (body == null) throw new IllegalArgumentException("댓글 정보가 필요합니다.");
        return body;
    }
    private long memberId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object raw = session == null ? null : session.getAttribute("loginMemberId");
        if (!(raw instanceof Number number)) throw new SecurityException("로그인이 필요한 기능입니다.");
        return number.longValue();
    }
    private void json(HttpServletResponse response) { response.setContentType("application/json;charset=UTF-8"); }
    private void send(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        gson.toJson(Map.of("success", false, "message", message), response.getWriter());
    }
    private static class CommentRequest { long postId; long commentId; Long parentCommentId; String content; }
}
