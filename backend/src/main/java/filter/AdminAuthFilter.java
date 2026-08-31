package filter;

import com.google.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import util.SessionUtil;

import java.io.IOException;
import java.util.Map;

@WebFilter("/api/admin/*")
public class AdminAuthFilter implements Filter {

    private final Gson gson = new Gson();

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest request =
                (HttpServletRequest) servletRequest;

        HttpServletResponse response =
                (HttpServletResponse) servletResponse;

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null) {
            sendError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "로그인이 필요한 기능입니다."
            );
            return;
        }

        String role = SessionUtil.role(request);

        if (!"ADMIN".equals(role)) {
            sendError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "관리자만 이용할 수 있는 기능입니다."
            );
            return;
        }

        chain.doFilter(request, response);
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
