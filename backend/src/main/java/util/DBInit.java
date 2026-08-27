package util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import io.github.cdimascio.dotenv.Dotenv;

public class DBInit {
    private static final Path PROJECT_ROOT = ProjectPaths.findProjectRoot();
    private static final Dotenv dotenv = Dotenv.configure()
            .directory(PROJECT_ROOT.toString())
            .ignoreIfMissing()
            .load();

    private static final String URL = requireEnv("DB_URL");
    private static final String USER = requireEnv("DB_USER");
    private static final String PASSWORD = requireEnv("DB_PASSWORD");

    private static String requireEnv(String key) {
        String value = dotenv.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " 환경변수 미설정 — 프로젝트 루트의 .env를 확인하세요.");
        }
        return value;
    }

    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");

            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                runSql(conn, PROJECT_ROOT.resolve("db/schema.sql"));
                runSql(conn, PROJECT_ROOT.resolve("db/seed.sql"));
            }

            System.out.println("DB 초기화 완료");

        } catch (Exception e) {
            throw new IllegalStateException("DB 초기화에 실패했습니다.", e);
        }
    }

    private static void runSql(Connection conn, Path path) throws Exception {
        String script = Files.readString(path, StandardCharsets.UTF_8);

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
