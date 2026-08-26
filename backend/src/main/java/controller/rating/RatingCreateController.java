package controller.rating;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dto.RatingDTO;
import exception.DuplicateRatingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.RatingService;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@WebServlet("/api/ratings")
public class RatingCreateController extends HttpServlet {
    private RatingService ratingService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        ratingService = new RatingService();
        gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        setJsonResponse(response);

        Long loginMemberId = getLoginMemberId(request);
        if (loginMemberId == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요한 기능입니다.");
            return;
        }

        final RatingDTO rating;
        try {
            request.setCharacterEncoding("UTF-8");
            rating = gson.fromJson(request.getReader(), RatingDTO.class);
        } catch (JsonSyntaxException exception) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "요청 데이터 형식이 올바르지 않습니다.");
            return;
        }

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

    private Long getLoginMemberId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object memberIdAttribute = session.getAttribute("loginMemberId");
        if (!(memberIdAttribute instanceof Number)) {
            return null;
        }
        return ((Number) memberIdAttribute).longValue();
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
