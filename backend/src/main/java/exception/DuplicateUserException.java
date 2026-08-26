// ============================================
// 파일: exception/DuplicateUserException.java
// 왜 커스텀 예외를 쓰는가(이 패키지 3개 공통 이유):
//   자바 표준 IllegalArgumentException은 범용이라 의도가 안 드러남 —
//   타입별로 나누면 GlobalExceptionFilter가 상태코드를 바로 판단 가능
//   (400/401/403 매핑은 interface_spec.md 규약 참고)
//
// 이 예외: 아이디/닉네임/이메일 중복 시(400)
// ============================================
package exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String message) { super(message); }
}