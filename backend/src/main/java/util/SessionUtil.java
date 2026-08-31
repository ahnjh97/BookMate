// 파일: util/SessionUtil.java
// 목적: "지금 로그인한 회원 ID가 뭔지"를 여러 컨트롤러가 각자 다르게 캐스팅하지 않도록 통일
// 세션 생성/무효화는 (현재 AuthController )
package util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class SessionUtil {
    public static Long getLoginMemberId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object v = session == null ? null : session.getAttribute("loginMemberId");
        return v instanceof Number ? ((Number) v).longValue() : null;
    }
}