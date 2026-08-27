// 로컬 Oracle과 연결되는 BookMate 내장 Tomcat 실행 진입점
import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;

import util.ProjectPaths;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        Path projectRoot = ProjectPaths.findProjectRoot();
        Path backendRoot = projectRoot.resolve("backend");
        Path webappRoot = backendRoot.resolve("src/main/webapp");
        Path classesRoot = backendRoot.resolve("target/classes");
        Path frontendRoot = projectRoot.resolve("frontend");

        requireDirectory(webappRoot, "Servlet 웹 설정 폴더");
        requireDirectory(classesRoot, "컴파일 결과 폴더(target/classes)");
        requireDirectory(frontendRoot, "프론트엔드 폴더");

        int port = Integer.getInteger("bookmate.port", 8080);
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.setBaseDir(backendRoot.resolve("target/tomcat").toString());
        tomcat.getConnector();

        // 빈 컨텍스트 경로를 사용하므로 접속 주소는 http://localhost:8080/ 입니다.
        Context context = tomcat.addWebapp("", webappRoot.toString());
        context.setParentClassLoader(Thread.currentThread().getContextClassLoader());

        WebResourceRoot resources = new StandardRoot(context);
        resources.addPreResources(
                new DirResourceSet(resources, "/WEB-INF/classes", classesRoot.toString(), "/")
        );
        // 8080 직접 접근도 가능하도록 frontend를 연결한다.
        // 일반적인 로컬 화면 개발은 DevProxyServer의 5501 포트를 사용한다.
        resources.addPreResources(
                new DirResourceSet(resources, "/", frontendRoot.toString(), "/")
        );
        context.setResources(resources);

        StandardJarScanner scanner = (StandardJarScanner) context.getJarScanner();
        StandardJarScanFilter filter = new StandardJarScanFilter();
        filter.setTldSkip("*.jar");
        scanner.setJarScanFilter(filter);
        scanner.setScanManifest(false);

        tomcat.start();
        System.out.println("BookMate 실행: http://localhost:" + port + "/");
        tomcat.getServer().await();
    }

    private static void requireDirectory(Path path, String label) {
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException(label + "를 찾을 수 없습니다: " + path);
        }
    }
}
