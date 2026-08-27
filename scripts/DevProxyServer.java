// ================================================================================
// DevProxyServer.java
//
// ▶ 이 파일이 scripts/ 에 있는 이유
//   backend/ Maven 프로젝트 밖에 의도적으로 분리
//   mvn package로 만드는 배포용 jar에는 쓰이지않는 로컬 테스트용 도구
//
// ▶ 왜 필요한가
//   로컬에서 프론트(html/js)랑 백엔드(Main.java, Tomcat)는 포트가 다름
//     - 백엔드: localhost:8080
//     - 프론트: localhost:5500 (이 서버가 띄우는 포트)
//   브라우저는 포트 다른 서버끼리 통신을 CORS로 기본 차단
//   fetch('/api/...') 호출불가
//
//   지금 이 파일이 하는 일 = "/api는 8080으로 넘기고, 나머지는 정적파일 서빙"
//   배포환경에선 아래 둘 중 하나로 대체
//     1) Docker 컨테이너 네트워크 - 프론트/백엔드가 각자 컨테이너로 뜨고
//        docker-compose.yml에 "같은 네트워크"로 묶어두면, 두 컨테이너가
//        서로의 이름으로 통신 가능해짐. 예를 들어 프론트 nginx 설정 안에
//        "location /api { proxy_pass http://backend:8080; }" 딱 한 줄이면
//        지금 이 자바 코드 200줄이 하던 일 전부 대체
//     2) 아니면 백엔드가 프론트까지 통째로 서빙하는 구조로 배포시 불필요해짐
//        (Main.java가 frontend 폴더까지 서빙)
//   배포 환경엔 원래 있는 중계 기능을 처리
//
// ▶ 이 파일이 뭘 하는가
//   5500번 하나에서 두 가지 처리
//     1) /api로 시작하는 요청 -> 그대로 8080(백엔드)으로 전달, 응답 그대로 돌려줌
//     2) 그 외 요청 -> frontend 폴더 안 html/css/js 파일 직접 읽어서 응답
//   결과: 브라우저 입장에선 전부 같은 서버(5500)에서 오는 것처럼 보여서
//         CORS 차단 자체가 아예 발동 안 함
//
// ▶ 실행 방법
//   1) 백엔드 먼저 실행 (IntelliJ에서 Main 클래스 Run:  8080에서)
//   2) 터미널에서
//        cd scripts
//        javac DevProxyServer.java
//        java DevProxyServer
//   3) 브라우저에서 localhost:5500 접속
// ================================================================================

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class DevProxyServer {

    // 이 프록시 서버 자신이 쓰는 포트 - 브라우저는 항상 여기로 접속
    private static final int PROXY_PORT = 5501;

    // 실제 API를 처리하는 백엔드 주소
    // 코드에 고정값 안 박고 환경변수로 뺌 - 백엔드 포트 바뀌어도 코드 수정 불필요
    // 값 안 주면 기본값(localhost:8080) 그대로 쓰임 - 팀원은 설정 없이 실행만 하면 됨
    private static final String BACKEND_URL =
            System.getenv().getOrDefault("BACKEND_URL", "http://localhost:8080");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PROXY_PORT), 0);

        // /api로 시작하는 요청은 전부 백엔드로 중계
        server.createContext("/api", DevProxyServer::proxyToBackend);

        // 나머지는 frontend 정적 파일로 응답
        server.createContext("/", DevProxyServer::serveStatic);

        server.setExecutor(null); // 개발용 서버라 별도 스레드풀 설정 불필요
        server.start();

        System.out.println("프론트 개발 서버 실행 : http://localhost:" + PROXY_PORT);
        System.out.println("API 요청 중계 대상 : " + BACKEND_URL);
    }

    // /api/... 요청을 그대로 백엔드로 넘기고 응답을 그대로 돌려주는 중계 역할
    private static void proxyToBackend(HttpExchange exchange) throws IOException {
        // 예 : /api/books/1 -> http://localhost:8080/api/books/1
        URI targetUrl = URI.create(BACKEND_URL + exchange.getRequestURI());
        HttpURLConnection connection = (HttpURLConnection) targetUrl.toURL().openConnection();

        // 원래 요청과 같은 메서드(GET/POST/DELETE 등)로 재요청
        String requestMethod = exchange.getRequestMethod();
        connection.setRequestMethod(requestMethod);

        // 요청 헤더 그대로 복사
        exchange.getRequestHeaders().forEach((headerName, headerValues) ->
                headerValues.forEach(value -> connection.addRequestProperty(headerName, value)));

        // 요청 본문(JSON 등)이 있는 메서드만 출력 스트림을 연다.
        // GET에서도 getOutputStream()을 호출하면 HttpURLConnection이 POST로 바꿔 버린다.
        if (requestMethod.equals("POST") || requestMethod.equals("PUT") || requestMethod.equals("PATCH")) {
            connection.setDoOutput(true);
            try (OutputStream requestBodyOut = connection.getOutputStream()) {
                exchange.getRequestBody().transferTo(requestBodyOut);
            }
        }

        // 4xx/5xx는 getErrorStream, 나머지는 getInputStream에서 읽어야 함
        int statusCode = connection.getResponseCode();
        InputStream responseBodyIn = statusCode >= 400
                ? connection.getErrorStream()
                : connection.getInputStream();
        byte[] responseBody = responseBodyIn == null ? new byte[0] : responseBodyIn.readAllBytes();

        // 백엔드 응답 그대로 브라우저에 전달
        exchange.getResponseHeaders().add("Content-Type", connection.getContentType());
        exchange.sendResponseHeaders(statusCode, responseBody.length);
        try (OutputStream responseOut = exchange.getResponseBody()) {
            responseOut.write(responseBody);
        }
    }

    // /api 아닌 나머지 요청 - frontend 폴더에서 파일 찾아 그대로 응답
    private static void serveStatic(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();

        // DB의 이미지 URL은 배포 컨텍스트 경로(/bookmate)를 포함한다.
        // 로컬 개발 서버는 frontend를 루트(/)에서 제공하므로 접두사를 제거한다.
        if (requestPath.equals("/bookmate")) {
            requestPath = "/";
        } else if (requestPath.startsWith("/bookmate/")) {
            requestPath = requestPath.substring("/bookmate".length());
        }

        // 루트("/") 접속 시 보여줄 시작 페이지 - 실제 경로에 맞게 수정 필요
        if (requestPath.equals("/")) {
            requestPath = "/pages/index.html";
        }

        // scripts 폴더 기준 상대경로로 frontend 폴더 접근
        Path requestedFile = Paths.get("../frontend" + requestPath);

        if (!Files.exists(requestedFile) || Files.isDirectory(requestedFile)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        byte[] fileContent = Files.readAllBytes(requestedFile);
        exchange.getResponseHeaders().add("Content-Type", resolveContentType(requestPath));
        exchange.sendResponseHeaders(200, fileContent.length);
        try (OutputStream responseOut = exchange.getResponseBody()) {
            responseOut.write(fileContent);
        }
    }

    // 확장자 보고 Content-Type 결정 - 틀리면 브라우저가 js를 텍스트로 표시하는 등 화면이 깨짐
    private static String resolveContentType(String path) {
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".webp")) return "image/webp";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
