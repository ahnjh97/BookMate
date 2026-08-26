// ============================================
// 파일: exception/AuthorizationException.java
// 이 예외: 로그인은 됐지만 권한 부족, 즉 "누군지는 알지만 못 하게 막음"(403)
// AuthenticationException(401)과 차이: 신원 확인 여부가 다름
// ============================================
package exception;

public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) { super(message); }
}