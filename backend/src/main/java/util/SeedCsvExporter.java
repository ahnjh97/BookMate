package util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import io.github.cdimascio.dotenv.Dotenv;

public class SeedCsvExporter {
    private static final Path PROJECT_ROOT = ProjectPaths.findProjectRoot();

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.configure().directory(PROJECT_ROOT.toString()).load();
        Class.forName("oracle.jdbc.OracleDriver");
        try (Connection connection = DriverManager.getConnection(
                dotenv.get("DB_URL"), dotenv.get("DB_USER"), dotenv.get("DB_PASSWORD"))) {
            export(connection, "books.csv", """
                    SELECT B.book_id,A.author_name AS author,B.title,B.genre,B.publisher,
                           TO_CHAR(B.published_date,'YYYY-MM-DD') AS published_date,
                           B.description,B.image_url
                    FROM BOOK B JOIN AUTHOR A ON A.author_id=B.author_id ORDER BY B.book_id
                    """, true);
            export(connection, "tier-templates.csv", """
                    SELECT T.template_id,M.login_id AS creator_login_id,T.title,T.description,
                           T.category,T.status
                    FROM TIER_TEMPLATE T JOIN MEMBER M ON M.member_id=T.member_id ORDER BY T.template_id
                    """, false);
            export(connection, "tier-template-items.csv", """
                    SELECT template_id,book_id,sort_order FROM TIER_TEMPLATE_ITEM
                    ORDER BY template_id,sort_order
                    """, false);
            export(connection, "ideal-templates.csv", """
                    SELECT T.template_id,M.login_id AS creator_login_id,T.title,T.description,T.category
                    FROM IDEAL_TEMPLATE T JOIN MEMBER M ON M.member_id=T.member_id ORDER BY T.template_id
                    """, false);
            export(connection, "ideal-template-items.csv", """
                    SELECT template_id,book_id,sort_order FROM IDEAL_TEMPLATE_ITEM
                    ORDER BY template_id,sort_order
                    """, false);
            export(connection, "tier-results.csv", """
                    SELECT L.tier_list_id,M.login_id,L.template_id,L.title,L.description
                    FROM TIER_LIST L JOIN MEMBER M ON M.member_id=L.member_id ORDER BY L.tier_list_id
                    """, false);
            export(connection, "tier-result-items.csv", """
                    SELECT tier_list_id,book_id,tier_grade,sort_order FROM TIER_ITEM
                    ORDER BY tier_list_id,sort_order
                    """, false);
            export(connection, "ideal-results.csv", """
                    SELECT R.run_id,R.template_id,M.login_id,R.bracket_size,R.winner_book_id
                    FROM IDEAL_RUN R JOIN MEMBER M ON M.member_id=R.member_id ORDER BY R.run_id
                    """, false);
            export(connection, "ideal-result-matches.csv", """
                    SELECT run_id,round_size,match_order,left_book_id,right_book_id,winner_book_id
                    FROM IDEAL_MATCH ORDER BY run_id,round_size DESC,match_order
                    """, false);
        }
    }

    private static void export(Connection connection, String fileName, String sql,
                               boolean shortenDescription) throws Exception {
        Path path = PROJECT_ROOT.resolve("db").resolve(fileName);
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql);
             var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            ResultSetMetaData metadata = resultSet.getMetaData();
            for (int column = 1; column <= metadata.getColumnCount(); column++) {
                if (column > 1) writer.write(',');
                writer.write(csv(metadata.getColumnLabel(column).toLowerCase()));
            }
            writer.newLine();

            int count = 0;
            while (resultSet.next()) {
                for (int column = 1; column <= metadata.getColumnCount(); column++) {
                    if (column > 1) writer.write(',');
                    String value = resultSet.getString(column);
                    if (shortenDescription && "DESCRIPTION".equals(metadata.getColumnLabel(column))) {
                        value = shortDescription(value);
                    }
                    writer.write(csv(value));
                }
                writer.newLine();
                count++;
            }
            System.out.printf("%s %,d개 내보냄%n", fileName, count);
        }
    }

    private static String shortDescription(String value) {
        if (value == null || value.length() <= 120) return value;
        int boundary = -1;
        for (int index = 20; index < 120; index++) {
            char current = value.charAt(index);
            if (current == '.' || current == '!' || current == '?') {
                boundary = index + 1;
                break;
            }
        }
        if (boundary > 0) return value.substring(0, boundary).trim();
        String shortened = value.substring(0, 119);
        int lastSpace = shortened.lastIndexOf(' ');
        if (lastSpace >= 60) shortened = shortened.substring(0, lastSpace);
        return shortened.stripTrailing().replaceAll("[.,!?]+$", "") + "…";
    }

    private static String csv(String value) {
        if (value == null) return "";
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
