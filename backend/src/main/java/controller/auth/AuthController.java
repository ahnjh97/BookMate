// ============================================
// 파일: controller/auth/AuthController.java
//
// [목적] 로그인 상태(세션) 자체를 다루는 컨트롤러. 하나의 URL(/api/auth)에
//        HTTP 메서드로 동작을 구분함(REST의 "세션을 자원으로 보는" 표준 패턴).
//
// [엔드포인트]
//   GET    /api/auth  세션 확인 — 로그인 여부와 기본 정보(닉네임 등) 조회
//   POST   /api/auth  로그인 — 성공 시 세션 발급
//   DELETE /api/auth  로그아웃 — 세션 무효화
//
// [응답 형식] {success, ...필드} — data로 안 감싸고 최상위에 바로 담음
//   성공(로그인/로그아웃): {"success":true, "message":"..."}
//   성공(세션확인, 로그인됨): {"success":true, "loggedIn":true, "memberId":1, "loginId":"...", "nickname":"...", "role":"USER"}
//   성공(세션확인, 비로그인): {"success":true, "loggedIn":false}
//   실패: {"success":false, "message":"..."}
//
// [세션에 저장되는 값 — 다른 컨트롤러(PreferenceController 등)도 이 키를 그대로 참조함,
//  키 이름을 바꾸려면 그 컨트롤러들도 같이 확인해야 함]
//   loginMemberId  Long    로그인한 회원 번호
//   loginId        String  로그인 아이디
//   loginNickname  String  닉네임
//   loginRole      String  USER 또는 ADMIN
//
// [주의사항 — 현재 제공 범위 밖]
//   - email, 작가 인증 여부(isAuthor)는 세션에 없음
//     → 필요하면 GET /api/members/me(AccountInfoController)로 별도 조회
//   - 로그인 실패 횟수 제한(fail_count/is_locked 갱신)은 이 컨트롤러가 안 함
//     → AuthService.login() 내부에서 처리하거나 별도 구현 필요(SFR-009, 미구현)
// ============================================

package controller.auth;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import dto.MemberDTO;
import exception.AuthenticationException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.AuthService;

@WebServlet("/api/auth")
public class AuthController extends HttpServlet {
    private final AuthService authService = new AuthService();
    private final Gson gson = new Gson();

    // GET /api/auth — 세션 확인
    // 세션엔 있는데 닉네임 값이 비어있는 경우(예: 서버 재시작 등으로 일부만 남은 상태)
    // DB에서 다시 조회해 세션을 스스로 복구함(자가치유)
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepare(response);
        HttpSession session = request.getSession(false);
        Object memberId = session == null ? null : session.getAttribute("loginMemberId");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("loggedIn", memberId instanceof Number);
        if (memberId instanceof Number) {
            if (session.getAttribute("loginNickname") == null) {
                MemberDTO member = authService.findMember(((Number) memberId).longValue());
                if (member != null) {
                    session.setAttribute("loginId", member.getLoginId());
                    session.setAttribute("loginNickname", member.getNickname());
                    session.setAttribute("loginRole", member.getRole());
                }
            }
            result.put("memberId", ((Number) memberId).longValue());
            result.put("loginId", session.getAttribute("loginId"));
            result.put("nickname", session.getAttribute("loginNickname"));
            result.put("role", session.getAttribute("loginRole"));
        }
        gson.toJson(result, response.getWriter());
    }

    // POST /api/auth — 로그인
    // 기존 세션을 무효화한 뒤 새 세션을 발급함(세션 고정 공격 방지 목적) —
    // 로그인 전에 있던 세션 ID를 그대로 재사용하면, 공격자가 미리 알고 있던
    // 세션 ID로 피해자의 로그인 상태를 가로챌 수 있어 이를 차단하기 위함
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepare(response);
        try {
            request.setCharacterEncoding("UTF-8");
            MemberDTO input = gson.fromJson(request.getReader(), MemberDTO.class);
            MemberDTO member = authService.login(input == null ? null : input.getLoginId(), input == null ? null : input.getPassword());
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) oldSession.invalidate();
            HttpSession session = request.getSession(true);
            session.setAttribute("loginMemberId", member.getMemberId());
            session.setAttribute("loginId", member.getLoginId());
            session.setAttribute("loginNickname", member.getNickname());
            session.setAttribute("loginRole", member.getRole());
            session.setMaxInactiveInterval(30 * 60);
            gson.toJson(Map.of("success", true, "message", "로그인했습니다."), response.getWriter());
        } catch (JsonSyntaxException | IllegalArgumentException exception) {
            // 요청 형식 오류, 필수값 누락 등 사용자 입력 문제 → 400
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (AuthenticationException exception) {
            // 아이디/비밀번호 불일치 → 401 (AuthService가 "아이디 없음"과 "비번 틀림"을
            // 구분하지 않고 같은 메시지로 던지므로, 여기서도 그대로 전달)
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
        } catch (RuntimeException exception) {
            // DB 오류 등 예상 못 한 서버 문제 → 500, 내부 원인은 사용자에게 노출 안 함
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "로그인 중 오류가 발생했습니다.");
        }
    }

    // DELETE /api/auth — 로그아웃
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        prepare(response);
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        gson.toJson(Map.of("success", true, "message", "로그아웃했습니다."), response.getWriter());
    }

    // 모든 응답에 공통으로 적용 — JSON 응답임을 명시하고,
    // 로그인 응답이 브라우저 캐시에 남지 않도록 Cache-Control 설정
    private void prepare(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        gson.toJson(Map.of("success", false, "message", message == null ? "요청을 처리하지 못했습니다." : message), response.getWriter());
    }
}
