// ============================================
// 파일: check/AllChecks.java
// 목적: 순수 로직 검증(Checkable 구현체)을 순서대로 실행하고 결과를 한 화면에 요약
//       JUnit 없이도 "무엇이 왜 깨졌는지"를 한눈에 파악하기 위한 유일한 진입점
//       DB에 접근하는 Dao/Service는 여기 안 넣음(진짜 데이터가 쌓이므로 별도 관리)
//
// ── 사용법 ──
//   실행: 이 파일 우클릭 → Run 'AllChecks.main()'
//   결과 읽는 법: 위에서부터 순서대로 실행되므로, 특정 번호에서 FAIL이 뜨면
//                그 계층부터 원인을 좁혀나갈 것(이후 번호는 그 원인의 연쇄일 수 있음)
//
// ── 내 기능 검증 추가하는 법 ──
//   1. check/ 폴더에 {대상}Check.java 생성 후 Checkable 구현 (Checkable.java 참고)
//   2. 아래 CHECKS 목록에 new 클래스명() 한 줄만 추가
//   그 외에는 이 파일의 다른 부분을 건드릴 필요 없음
// ============================================

package check;

import java.util.List;

public class AllChecks {

    // 순서 = 구현 순서. 새 항목은 이 목록 끝에 한 줄만 추가하면 됨
    private static final List<Checkable> CHECKS = List.of(
//            new ExceptionCheck(),
//            new ResponseFormatCheck(),
//            new ValidationCheck(),
//            new FilterRoutingCheck(),
//            new DtoCheck()
    );

    public static void main(String[] args) {
        System.out.println("===== 순수 로직 검증 시작 =====");
        for (Checkable c : CHECKS) {
            boolean pass = safeCheck(c);
            System.out.println(c.name() + " ... " + (pass ? "PASS" : "FAIL"));
        }
        System.out.println("===== 순수 로직 검증 종료 =====");
        System.out.println("(Dao/Service는 DB 연동 필요 — 별도 DbLinkedChecks 실행)");
    }

    private static boolean safeCheck(Checkable c) {
        try {
            return c.check();
        } catch (Exception e) {
            System.out.println("   └ 예외: " + e.getMessage());
            return false;
        }
    }
}