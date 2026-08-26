// 서버 실행 진입점. 내장 Tomcat을 코드로 직접 구동한다 (외부 Tomcat 설치 불필요)
import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;

import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        // 접속 포트 지정 (localhost:8080)
        tomcat.setPort(8080);

        // HTTP 요청 수신용 커넥터 초기화
        tomcat.getConnector();

        // webapp 폴더(WEB-INF/web.xml 등)를 웹 루트로 등록
        // "" = 컨텍스트 경로를 루트("/")로 지정
        String webappDir = "src/main/webapp";
        Context ctx = tomcat.addWebapp("", new File(webappDir).getAbsolutePath());

        // 컴파일된 클래스 경로(target/classes)를 웹앱에 등록
        // 이게 없으면 @WebServlet 붙은 클래스 자체를 Tomcat이 찾지 못해 전부 404가 남
        File classesDir = new File("target/classes");
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(
                new DirResourceSet(resources, "/WEB-INF/classes", classesDir.getAbsolutePath(), "/")
        );
        ctx.setResources(resources);

        // jar 안 TLD(JSP 태그 라이브러리) 스캔 생략 - 속도 향상용, 기능에 영향 없음
        StandardJarScanner scanner = (StandardJarScanner) ctx.getJarScanner();
        StandardJarScanFilter filter = new StandardJarScanFilter();
        filter.setTldSkip("*.jar");
        scanner.setJarScanFilter(filter);

        // 서버 구동 시작
        tomcat.start();
        check.AllChecks.main(null);
        // 메인 스레드 대기 - 없으면 프로세스 즉시 종료되며 서버도 같이 꺼짐
        tomcat.getServer().await();
    }
}