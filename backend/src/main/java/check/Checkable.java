// ============================================
// 파일: check/Checkable.java
// 목적: 모든 검증 클래스가 지켜야 할 공통 규격(인터페이스)
//       "이름 하나 + 검증메서드 하나"라는 동일한 모양을 강제해서,
//       AllChecks가 각 클래스의 세부 내용을 몰라도 목록만 돌리면 되게 함(다형성)
//
// ── 사용법 (새 기능 검증 추가할 때) ──
//   1. check/ 폴더에 새 파일 생성: {대상}Check.java
//   2. 아래처럼 이 인터페이스(Checkable)를 implements
//   3. name() : 결과 출력용 이름, "번호. 대상명" 형식(예: "6. Book")
//   4. check() : 인자 없음, 확인할 로직만 작성, 통과하면 true 반환
//   5. check/AllChecks.java의 CHECKS 목록에 new 클래스명() 한 줄만 추가
//
// ── 주의 ──
//   DB에 실제로 저장/조회하는 코드(Dao, Service)는 여기 쓰지 말 것
//   → 진짜 데이터가 쌓이므로 별도 파일(DbLinkedChecks 등)로 분리
//
// ── 고정값/규약이 있는 로직을 검증할 때 (예: 상태코드 매핑, 등급 분류 등) ──
//   check() 로직 안에 숫자·문자열 같은 고정값이 나온다면,
//   그 값이 "어떤 근거로 정해졌는지"를 파일 상단에 표(규약)로 반드시 명시
//
// ── 상태코드 판정 규약 (interface_spec.md와 동일) ──
//   DuplicateUserException  → 400
//   AuthenticationException → 401
//   그 외                   → 500
//
// ── 예시 (아래를 그대로 복사해서 새 파일에 붙여넣고 이름/규약/로직만 바꿀 것) ──
//
//   package check;
//   import exception.*;
//
//   public class StatusCodeCheck implements Checkable {
//       public String name() { return "6. 상태코드 판정"; }
//       public boolean check() {
//           return resolveCode(new DuplicateUserException("x")) == 400
//               && resolveCode(new AuthenticationException("x")) == 401
//               && resolveCode(new RuntimeException("x")) == 500;
//       }
//       // resolveCode(): 이 파일 전용 헬퍼(자바 표준 아님) — 위 규약표 그대로 구현
//       private int resolveCode(RuntimeException e) {
//           if (e instanceof DuplicateUserException) return 400;
//           if (e instanceof AuthenticationException) return 401;
//           return 500;
//       }
//   }
//
// ============================================

package check;

public interface Checkable {
    String name();   // 결과 출력용 이름
    boolean check();  // 검증 로직, 통과 시 true
}