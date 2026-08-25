import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DB_Init {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String USER = "bookmate";
    private static final String PASSWORD = "book";

    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");

            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                runSql(conn, "db/init.sql");
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