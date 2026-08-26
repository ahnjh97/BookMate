package util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import io.github.cdimascio.dotenv.Dotenv;

public class DBInit {
    // 원본:
    // private static final String URL = "jdbc:oracle:thin:@localhost:1521:xe";
    // private static final String USER = "bookmate";
    // private static final String PASSWORD = "book";
    // 수정: dotenv-java로 .env 값을 읽어옴. 값 없으면 즉시 에러(하드코딩 fallback 없음)
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .ignoreIfMissing()
            .load();

    private static final String URL = requireEnv("DB_URL");
    private static final String USER = requireEnv("DB_USER");
    private static final String PASSWORD = requireEnv("DB_PASSWORD");

    private static String requireEnv(String key) {
    String value = dotenv.get(key);
    if (value == null || value.isBlank())
        throw new IllegalStateException(key + " 환경변수 미설정 — .env 확인 필요");
    return value;
    }

    public static void main(String[] args) {
        // 원본: System.out.println(System.getProperty("user.dir"));
        // 수정: 디버깅용 출력 제거(불필요한 로그)
        try {
            Class.forName("oracle.jdbc.OracleDriver");

            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                // 원본: runSql(conn, "backend/db/schema.sql");
                // 수정: db 폴더가 루트로 이동됨. 실행 위치가 backend/ 기준이면 ../db/schema.sql,
                //       루트 기준이면 db/schema.sql — 본인 Working directory 설정에 맞게 확인 필요
                runSql(conn, "db/schema.sql");
                runSql(conn, "db/seed.sql");
            }

            System.out.println("DB 초기화 완료");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runSql(Connection conn, String path) throws Exception {
        String script = Files.readString(Path.of(path), StandardCharsets.UTF_8);

        try (Statement stmt = conn.createStatement()) {
            for (String sql : script.split("/\\*END\\*/")) {
                sql = sql.trim();
                if (sql.isEmpty()) continue;
                if (!sql.toUpperCase().startsWith("BEGIN") && sql.endsWith(";")) {
                    sql = sql.substring(0, sql.length() - 1);
                }
                stmt.execute(sql);
            }
        }
    }
}
