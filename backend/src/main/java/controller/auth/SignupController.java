package controller.auth;

//import com.google.gson.JsonSyntaxException;
//import exception.DuplicateUserException;
import com.google.gson.Gson;
import dto.MemberDTO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AuthService;
import util.ResponseWrapper;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/auth/signup")
public class SignupController extends HttpServlet {

    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();

    // 사용법: GET /api/auth/signup?loginId=xxx
    // 응답: {"success":true, "available":true|false}
    // 실패(형식 오류): 400, {"success":false, "message":"..."}  ← GlobalExceptionFilter가 처리
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean available = authService.isLoginIdAvailable(request.getParameter("loginId"));
        ResponseWrapper.success(response, Map.of("available", available));
    }

    // 사용법: POST /api/auth/signup, body: {"loginId","password","nickname","email"}
    // 응답(201): {"success":true, "message":"회원가입이 완료되었습니다.", "memberId":1}
    // 실패(400/409): GlobalExceptionFilter가 처리
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MemberDTO member = gson.fromJson(request.getReader(), MemberDTO.class);
        long memberId = authService.signup(member);
        ResponseWrapper.success(response, HttpServletResponse.SC_CREATED, Map.of(
                "message", "회원가입이 완료되었습니다.",
                "memberId", memberId
        ));
    }
}
