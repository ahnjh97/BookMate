package controller.rating;

import com.google.gson.Gson;
import dto.RatingPageDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.RatingService;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet("/api/ratings/public")
public class PublicRatingController extends HttpServlet {
    private static final int PAGE_SIZE = 4;
    private final RatingService ratingService = new RatingService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            long bookId = parsePositiveNumber(request.getParameter("bookId"), "올바른 책 번호가 필요합니다.");
            int page = (int) parsePositiveNumber(request.getParameter("page"), "올바른 페이지 번호가 필요합니다.");
            Integer score = parseOptionalScore(request.getParameter("score"));
            RatingPageDTO result = ratingService.findPublicRatings(bookId, page, PAGE_SIZE, score);
            gson.toJson(Map.of("success", true, "data", result), response.getWriter());
        } catch (IllegalArgumentException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (NoSuchElementException exception) {
            sendError(response, HttpServletResponse.SC_NOT_FOUND, exception.getMessage());
        } catch (RuntimeException exception) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "독자 평가를 불러오지 못했습니다.");
        }
    }

    private long parsePositiveNumber(String value, String message) {
        if (value == null || !value.matches("\\d+")) throw new IllegalArgumentException(message);
        long number = Long.parseLong(value);
        if (number <= 0) throw new IllegalArgumentException(message);
        return number;
    }

    private Integer parseOptionalScore(String value) {
        if (value == null || value.isBlank()) return null;
        if (!value.matches("[1-5]")) throw new IllegalArgumentException("별점은 1~5점이어야 합니다.");
        return Integer.valueOf(value);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        gson.toJson(Map.of("success", false, "message", message), response.getWriter());
    }
}
