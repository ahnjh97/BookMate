// ============================================
// 파일: check/ServerHealthCheck.java
// 목적: Tomcat 서버가 실제로 HTTP 요청을 받아 응답하는지 확인
//       /api/books/1 은 Service -> Dao -> DBUtil을 거쳐 DB까지 갔다오는 API라
//       이 호출 하나로 "서버 구동 + DB 연결"을 한 번에 검증함(DB 연결은 덤)
// ============================================
package check;

import java.net.HttpURLConnection;
import java.net.URI;

public class ServerHealthCheck implements Checkable {

    @Override
    public String name() {
        return "서버. Tomcat 구동 및 API 응답 확인";
    }

    @Override
    public boolean check() {
        try {
            // 실제 등록된 API를 호출 - 서버가 살아있어야만 응답이 옴
            URI uri = URI.create("http://localhost:8080/api/books/1");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);

            int status = connection.getResponseCode();

            // 200(데이터 있음) 또는 404(서버는 정상, 해당 데이터만 없음) 둘 다 "서버 정상 응답"으로 간주
            // 500이나 타임아웃이면 서버/DB에 실제 문제가 있는 것
            boolean isHealthy = status == 200 || status == 404;

            System.out.println("  응답 상태코드: " + status + (isHealthy ? " (정상)" : " (비정상)"));

            return isHealthy;

        } catch (Exception e) {
            // 연결 자체가 안 되면(서버 안 뜸, 포트 안 열림 등) 여기로 옴
            System.out.println("  서버 응답 없음: " + e.getMessage());
            return false;
        }
    }
}