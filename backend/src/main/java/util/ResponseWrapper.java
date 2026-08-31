// ============================================
// 파일: util/ResponseWrapper.java
//
// [목적]
//   컨트롤러마다 gson.toJson(Map.of("success", ...)) 를 반복 작성하던 걸
//   한 곳으로 모음. 팀원 컨트롤러(AuthController, SignupController 등)가
//   실제로 쓰던 응답 모양(최상위에 success/message/필드, data로 안 감쌈)을
//   그대로 재현하는 얇은 헬퍼 — 새로운 규약을 만든 게 아니라 기존 패턴을 통일함
//
// [응답 형식 예시]
//   성공(데이터 있음): {"success":true, "memberId":1, "role":"USER"}
//   성공(메시지만):    {"success":true, "message":"로그인했습니다."}
//   실패:             {"success":false, "message":"아이디 또는 비밀번호가 일치하지 않습니다."}
//
// [메서드별 사용법]
//   success(res, Map)           : 성공 + 데이터, 상태코드 200 고정
//                                 예) ResponseWrapper.success(res, Map.of("memberId", id, "role", role));
//   success(res, int, Map)      : 성공 + 데이터, 상태코드 직접 지정(201 등)
//                                 예) ResponseWrapper.success(res, 201, Map.of("memberId", id));
//   successMessage(res, String) : 성공 + 메시지만(데이터 없음)
//                                 예) ResponseWrapper.successMessage(res, "로그인했습니다.");
//   fail(res, int, String)      : 실패, 상태코드+메시지
//                                 예) ResponseWrapper.fail(res, 401, "아이디 또는 비밀번호가 일치하지 않습니다.");
//                                 보통 컨트롤러가 직접 안 부르고, service가 던진 예외를
//                                 GlobalExceptionFilter가 잡아서 대신 호출함
//
// [주의]
//   Content-Type/CharacterEncoding은 이 안에서 매번 설정하므로,
//   호출부(컨트롤러)에서 prepare() 같은 걸로 미리 안 해줘도 됨(중복 설정 무방, 덮어써짐)
// ============================================

package util;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletResponse;

public class ResponseWrapper {

    private static final Gson gson = new Gson();

    public static void success(HttpServletResponse res, Map<String, Object> extra) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        if (extra != null) body.putAll(extra);
        write(res, HttpServletResponse.SC_OK, body);
    }

    public static void successMessage(HttpServletResponse res, String message) throws IOException {
        write(res, HttpServletResponse.SC_OK, Map.of("success", true, "message", message));
    }

    public static void success(HttpServletResponse res, int statusCode, Map<String, Object> extra) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        if (extra != null) body.putAll(extra);
        write(res, statusCode, body);
    }

    public static void fail(HttpServletResponse res, int statusCode, String message) throws IOException {
        write(res, statusCode, Map.of("success", false, "message",
            message == null ? "요청을 처리하지 못했습니다." : message));
    }

    private static void write(HttpServletResponse res, int status, Map<String, Object> body) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(gson.toJson(body));
    }
}