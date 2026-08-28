package controller.home;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.HomeService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/home/summary")
public class HomeSummaryController extends HttpServlet {
    private final HomeService homeService = new HomeService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            gson.toJson(Map.of("success", true, "data", homeService.findSummary()), response.getWriter());
        } catch (RuntimeException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            gson.toJson(Map.of("success", false, "message", "홈 통계를 불러오지 못했습니다."), response.getWriter());
        }
    }
}
