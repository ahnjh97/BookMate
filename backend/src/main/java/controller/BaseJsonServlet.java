package controller;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public abstract class BaseJsonServlet extends HttpServlet {
    protected final Gson gson = new Gson();

    protected void prepareJson(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
    }

    protected <T> T readJson(HttpServletRequest request, Class<T> type) throws IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            return gson.fromJson(request.getReader(), type);
        } catch (JsonSyntaxException exception) {
            throw new IllegalArgumentException("올바른 JSON 형식으로 요청해 주세요.", exception);
        }
    }

    protected void writeJson(HttpServletResponse response, Object body) throws IOException {
        gson.toJson(body, response.getWriter());
    }

    protected void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        writeJson(response, Map.of(
                "success", false,
                "message", message == null ? "요청을 처리하지 못했습니다." : message
        ));
    }
}
