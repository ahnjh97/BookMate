// ============================================
// 파일: exception/AuthenticationException.java
// 이 예외: 로그인 시 아이디/비밀번호 불일치, 즉 "누구인지 증명 안 됨"(401)
// ============================================
package exception;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) { super(message); }
}