package controller.rating;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dto.RatingDTO;
import exception.DuplicateRatingException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.RatingService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet("/api/ratings")
public class RatingController extends HttpServlet {
    private final RatingService ratingService = new RatingService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        setJsonResponse(response);
        Long loginMemberId = requireLoginMemberId(request, response);
        if (loginMemberId == null) return;

        try {
            long bookId = parseBookId(request.getParameter("bookId"));
            RatingDTO rating = ratingService.findMyRating(bookId, loginMemberId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("data", rating);
            gson.toJson(result, response.getWriter());
        } catch (IllegalArgumentException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (RuntimeException exception) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "내 평점을 불러오지 못했습니다.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        setJsonResponse(response);
        Long loginMemberId = requireLoginMemberId(request, response);
        if (loginMemberId == null) return;

        RatingDTO rating = readRating(request, response);
        if (rating == null) return;

        try {
            long ratingId = ratingService.createRating(rating, loginMemberId);
            response.setStatus(HttpServletResponse.SC_CREATED);
            gson.toJson(
                    Map.of(
                            "success", true,
                            "message", "평점이 등록되었습니다.",
                            "ratingId", ratingId
                    ),
                    response.getWriter()
            );
        } catch (IllegalArgumentException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (NoSuchElementException exception) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, exception.getMessage());
        } catch (DuplicateRatingException exception) {
            sendError(response, HttpServletResponse.SC_CONFLICT, exception.getMessage());
        } catch (RuntimeException exception) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "평점 등록 중 오류가 발생했습니다.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        setJsonResponse(response);
        Long loginMemberId = requireLoginMemberId(request, response);
        if (loginMemberId == null) return;

        RatingDTO rating = readRating(request, response);
        if (rating == null) return;

        try {
            ratingService.updateRating(rating, loginMemberId);
            gson.toJson(
                    Map.of("success", true, "message", "평점이 수정되었습니다."),
                    response.getWriter()
            );
        } catch (IllegalArgumentException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (NoSuchElementException exception) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, exception.getMessage());
        } catch (RuntimeException exception) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "평점 수정 중 오류가 발생했습니다.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        setJsonResponse(response);
        Long loginMemberId = requireLoginMemberId(request, response);
        if (loginMemberId == null) return;

        try {
            long ratingId = parsePositiveId(request.getParameter("ratingId"), "올바른 평점 번호가 필요합니다.");
            long bookId = parseBookId(request.getParameter("bookId"));
            ratingService.deleteRating(ratingId, bookId, loginMemberId);
            gson.toJson(
                    Map.of("success", true, "message", "평점이 삭제되었습니다."),
                    response.getWriter()
            );
        } catch (IllegalArgumentException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (NoSuchElementException exception) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, exception.getMessage());
        } catch (RuntimeException exception) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "평점 삭제 중 오류가 발생했습니다.");
        }
    }

    private RatingDTO readRating(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            request.setCharacterEncoding("UTF-8");
            return gson.fromJson(request.getReader(), RatingDTO.class);
        } catch (JsonSyntaxException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "요청 데이터 형식이 올바르지 않습니다.");
            return null;
        }
    }

    private long parseBookId(String value) {
        return parsePositiveId(value, "올바른 책 번호가 필요합니다.");
    }

    private long parsePositiveId(String value, String message) {
        if (value == null || !value.matches("\\d+")) {
            throw new IllegalArgumentException(message);
        }
        long id = Long.parseLong(value);
        if (id <= 0) throw new IllegalArgumentException(message);
        return id;
    }

    private Long requireLoginMemberId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object memberId = session == null ? null : session.getAttribute("loginMemberId");
        if (!(memberId instanceof Number)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요한 기능입니다.");
            return null;
        }
        return ((Number) memberId).longValue();
    }

    private void setJsonResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        gson.toJson(Map.of("success", false, "message", message), response.getWriter());
    }
}
