package util;

import dto.MemberDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class SessionUtil {
    private static final String MEMBER_ID = "loginMemberId";
    private static final String LOGIN_ID = "loginId";
    private static final String NICKNAME = "loginNickname";
    private static final String ROLE = "loginRole";

    private SessionUtil() {
    }

    public static HttpSession start(HttpServletRequest request, MemberDTO member) {
        HttpSession previous = request.getSession(false);
        if (previous != null) previous.invalidate();

        HttpSession session = request.getSession(true);
        store(session, member);
        session.setMaxInactiveInterval(30 * 60);
        return session;
    }

    public static void store(HttpSession session, MemberDTO member) {
        session.setAttribute(MEMBER_ID, member.getMemberId());
        session.setAttribute(LOGIN_ID, member.getLoginId());
        session.setAttribute(NICKNAME, member.getNickname());
        session.setAttribute(ROLE, member.getRole());
    }

    public static Long memberId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(MEMBER_ID);
        return value instanceof Number number ? number.longValue() : null;
    }

    public static String role(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(ROLE);
        return value == null ? null : value.toString();
    }

    public static String loginId(HttpSession session) {
        return value(session, LOGIN_ID);
    }

    public static String nickname(HttpSession session) {
        return value(session, NICKNAME);
    }

    public static String role(HttpSession session) {
        return value(session, ROLE);
    }

    private static String value(HttpSession session, String key) {
        Object value = session == null ? null : session.getAttribute(key);
        return value == null ? null : value.toString();
    }
}
