// util/DBInit.java
// 필요 환경변수(.env): DB_URL, DB_USER, DB_PASSWORD

package util;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.cdimascio.dotenv.Dotenv;

public class DBInit {
    private static final String SQLPLUS_BLOCK_TERMINATOR = "(?m)^\\s*/\\s*$";
    private static final String SQLPLUS_SET_DEFINE = "(?im)^\\s*SET\\s+DEFINE\\s+OFF\\s*;?\\s*$";

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
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        try {
            Class.forName("oracle.jdbc.OracleDriver");

            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                runSql(conn, PROJECT_ROOT.resolve("db/schema.sql"));
                System.out.println("스키마 초기화됨");
                importUsers(conn, PROJECT_ROOT.resolve("db/users.csv"));
                System.out.printf("유저 %,d명 적재됨%n", queryCount(conn,
                        "SELECT COUNT(*) FROM MEMBER WHERE role='USER' AND login_id <> 'bookmate_system'"));
                importBooks(conn, PROJECT_ROOT.resolve("db/books.csv"));
                System.out.printf("책 %,d권 적재됨%n", queryCount(conn, "SELECT COUNT(*) FROM BOOK"));
                importRatings(conn, PROJECT_ROOT.resolve("db/ratings.csv"));
                System.out.printf("평점·후기 %,d개 적재됨%n", queryCount(conn, "SELECT COUNT(*) FROM RATING"));
                importSeedData(conn);
                verifySeedCounts(conn);
                System.out.println("초기 데이터 검증 완료");
            }

        } catch (Exception e) {
            throw new IllegalStateException("DB 초기화에 실패했습니다.", e);
        }
    }

    private static void importBooks(Connection conn, Path path) throws Exception {
        List<Map<String, String>> rows = readCsv(path);
        Map<String, Long> authorIds = new HashMap<>();

        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT author_id, author_name FROM AUTHOR")) {
            while (resultSet.next()) {
                authorIds.put(resultSet.getString("author_name"), resultSet.getLong("author_id"));
            }
        }

        String insertAuthor = "INSERT INTO AUTHOR(author_id,author_name,bio) VALUES(SEQ_AUTHOR.NEXTVAL,?,?)";
        String insertBook = """
                INSERT INTO BOOK(book_id,author_id,title,genre,publisher,published_date,description,image_url,status)
                VALUES(SEQ_BOOK.NEXTVAL,?,?,?,?,?,?,?,'APPROVED')
                """;

        conn.setAutoCommit(false);
        try (PreparedStatement authorStatement = conn.prepareStatement(insertAuthor, new String[]{"author_id"});
             PreparedStatement bookStatement = conn.prepareStatement(insertBook)) {
            for (int index = 0; index < rows.size(); index++) {
                Map<String, String> row = rows.get(index);
                requireOrderedId(row, "book_id", index + 1);
                String author = required(row, "author");
                Long authorId = authorIds.get(author);
                if (authorId == null) {
                    authorStatement.setString(1, author);
                    authorStatement.setString(2, row.get("genre") + " 분야의 도서를 쓴 작가");
                    authorStatement.executeUpdate();
                    try (ResultSet keys = authorStatement.getGeneratedKeys()) {
                        if (!keys.next()) throw new IllegalStateException("작가 번호를 생성하지 못했습니다: " + author);
                        authorId = keys.getLong(1);
                    }
                    authorIds.put(author, authorId);
                }

                bookStatement.setLong(1, authorId);
                bookStatement.setString(2, required(row, "title"));
                bookStatement.setString(3, required(row, "genre"));
                bookStatement.setString(4, emptyToNull(row.get("publisher")));
                String publishedDate = required(row, "published_date");
                bookStatement.setDate(5, java.sql.Date.valueOf(LocalDate.parse(publishedDate)));
                bookStatement.setString(6, emptyToNull(row.get("description")));
                bookStatement.setString(7, emptyToNull(row.get("image_url")));
                bookStatement.addBatch();
            }
            bookStatement.executeBatch();
            conn.commit();
        } catch (Exception exception) {
            conn.rollback();
            throw exception;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void importUsers(Connection conn, Path path) throws Exception {
        List<Map<String, String>> rows = readCsv(path);
        String sql = """
                INSERT INTO MEMBER(member_id,login_id,password,nickname,email,role)
                VALUES(SEQ_MEMBER.NEXTVAL,?,?,?,?,?)
                """;

        conn.setAutoCommit(false);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            for (Map<String, String> row : rows) {
                statement.setString(1, required(row, "login_id"));
                statement.setString(2, PasswordUtil.hash(required(row, "password")));
                statement.setString(3, required(row, "nickname"));
                statement.setString(4, required(row, "email"));
                statement.setString(5, required(row, "role"));
                statement.addBatch();
            }
            statement.executeBatch();
            conn.commit();
        } catch (Exception exception) {
            conn.rollback();
            throw exception;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void importRatings(Connection conn, Path path) throws Exception {
        List<Map<String, String>> rows = readCsv(path);
        Map<String, Long> memberIds = new HashMap<>();

        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT member_id, login_id FROM MEMBER")) {
            while (resultSet.next()) {
                memberIds.put(resultSet.getString("login_id"), resultSet.getLong("member_id"));
            }
        }

        String sql = """
                INSERT INTO RATING(rating_id,book_id,member_id,score,comment_text)
                VALUES(SEQ_RATING.NEXTVAL,?,?,?,?)
                """;

        conn.setAutoCommit(false);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            int batchSize = 0;
            for (Map<String, String> row : rows) {
                String loginId = required(row, "login_id");
                Long memberId = memberIds.get(loginId);
                if (memberId == null) {
                    throw new IllegalStateException("평점 CSV에 존재하지 않는 회원이 있습니다: " + loginId);
                }

                statement.setLong(1, Long.parseLong(required(row, "book_id")));
                statement.setLong(2, memberId);
                statement.setInt(3, Integer.parseInt(required(row, "score")));
                statement.setString(4, required(row, "comment_text"));
                statement.addBatch();

                if (++batchSize % 1000 == 0) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
            conn.commit();
        } catch (Exception exception) {
            conn.rollback();
            throw exception;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void importSeedData(Connection conn) throws Exception {
        Map<String, Long> memberIds = loadMemberIds(conn);

        batchImport(conn, "tier-templates.csv", """
                INSERT INTO TIER_TEMPLATE(template_id,member_id,title,description,category,status,processed_at)
                VALUES(SEQ_TIER_TEMPLATE.NEXTVAL,?,?,?,?,?,SYSDATE)
                """, (statement, row, index) -> {
            requireOrderedId(row, "template_id", index + 1);
            statement.setLong(1, requiredMemberId(memberIds, row));
            statement.setString(2, required(row, "title"));
            statement.setString(3, emptyToNull(row.get("description")));
            statement.setString(4, required(row, "category"));
            statement.setString(5, required(row, "status"));
        });
        batchImport(conn, "tier-template-items.csv", """
                INSERT INTO TIER_TEMPLATE_ITEM(template_item_id,template_id,book_id,sort_order)
                VALUES(SEQ_TIER_TEMPLATE_ITEM.NEXTVAL,?,?,?)
                """, (statement, row, index) -> {
            statement.setLong(1, requiredLong(row, "template_id"));
            statement.setLong(2, requiredLong(row, "book_id"));
            statement.setInt(3, requiredInt(row, "sort_order"));
        });
        batchImport(conn, "ideal-templates.csv", """
                INSERT INTO IDEAL_TEMPLATE(template_id,member_id,title,description,category,status,processed_at)
                VALUES(SEQ_IDEAL_TEMPLATE.NEXTVAL,?,?,?,?,?,SYSDATE)
                """, (statement, row, index) -> {
            requireOrderedId(row, "template_id", index + 1);
            statement.setLong(1, requiredMemberId(memberIds, row));
            statement.setString(2, required(row, "title"));
            statement.setString(3, emptyToNull(row.get("description")));
            statement.setString(4, required(row, "category"));
            statement.setString(5, required(row, "status"));
        });
        batchImport(conn, "ideal-template-items.csv", """
                INSERT INTO IDEAL_TEMPLATE_ITEM(template_item_id,template_id,book_id,sort_order)
                VALUES(SEQ_IDEAL_TEMPLATE_ITEM.NEXTVAL,?,?,?)
                """, (statement, row, index) -> {
            statement.setLong(1, requiredLong(row, "template_id"));
            statement.setLong(2, requiredLong(row, "book_id"));
            statement.setInt(3, requiredInt(row, "sort_order"));
        });
        batchImport(conn, "tier-results.csv", """
                INSERT INTO TIER_LIST(tier_list_id,member_id,template_id,title,description,created_at)
                VALUES(SEQ_TIER_LIST.NEXTVAL,?,?,
                       (SELECT title FROM TIER_TEMPLATE WHERE template_id=?),?,SYSDATE)
                """, (statement, row, index) -> {
            requireOrderedId(row, "tier_list_id", index + 1);
            statement.setLong(1, requiredMemberId(memberIds, row));
            long templateId = requiredLong(row, "template_id");
            statement.setLong(2, templateId);
            statement.setLong(3, templateId);
            statement.setString(4, emptyToNull(row.get("description")));
        });
        batchImport(conn, "tier-result-items.csv", """
                INSERT INTO TIER_ITEM(tier_item_id,tier_list_id,book_id,tier_grade,sort_order)
                VALUES(SEQ_TIER_ITEM.NEXTVAL,?,?,?,?)
                """, (statement, row, index) -> {
            statement.setLong(1, requiredLong(row, "tier_list_id"));
            statement.setLong(2, requiredLong(row, "book_id"));
            statement.setString(3, required(row, "tier_grade"));
            statement.setInt(4, requiredInt(row, "sort_order"));
        });
        batchImport(conn, "ideal-results.csv", """
                INSERT INTO IDEAL_RUN(run_id,template_id,member_id,bracket_size,winner_book_id,created_at)
                VALUES(SEQ_IDEAL_RUN.NEXTVAL,?,?,?,?,SYSDATE)
                """, (statement, row, index) -> {
            requireOrderedId(row, "run_id", index + 1);
            statement.setLong(1, requiredLong(row, "template_id"));
            statement.setLong(2, requiredMemberId(memberIds, row));
            statement.setInt(3, requiredInt(row, "bracket_size"));
            statement.setLong(4, requiredLong(row, "winner_book_id"));
        });
        batchImport(conn, "ideal-result-matches.csv", """
                INSERT INTO IDEAL_MATCH(match_id,run_id,round_size,match_order,left_book_id,right_book_id,winner_book_id)
                VALUES(SEQ_IDEAL_MATCH.NEXTVAL,?,?,?,?,?,?)
                """, (statement, row, index) -> {
            statement.setLong(1, requiredLong(row, "run_id"));
            statement.setInt(2, requiredInt(row, "round_size"));
            statement.setInt(3, requiredInt(row, "match_order"));
            statement.setLong(4, requiredLong(row, "left_book_id"));
            statement.setLong(5, requiredLong(row, "right_book_id"));
            statement.setLong(6, requiredLong(row, "winner_book_id"));
        });
        batchImport(conn, "posts.csv", """
                INSERT INTO POST(
                    post_id,member_id,tier_list_id,ideal_run_id,category,title,content,
                    genre,tag,view_count,is_pinned,status,created_at
                ) VALUES(SEQ_POST.NEXTVAL,?,?,?,?,?,?,?,?,?,?,?,SYSDATE-?)
                """, (statement, row, index) -> {
            requireOrderedId(row, "post_id", index + 1);
            statement.setLong(1, requiredMemberId(memberIds, row));
            setNullableLong(statement, 2, row.get("tier_list_id"));
            setNullableLong(statement, 3, row.get("ideal_run_id"));
            statement.setString(4, required(row, "category"));
            statement.setString(5, required(row, "title"));
            statement.setString(6, required(row, "content"));
            statement.setString(7, emptyToNull(row.get("genre")));
            statement.setString(8, emptyToNull(row.get("tag")));
            statement.setInt(9, requiredInt(row, "view_count"));
            statement.setString(10, required(row, "is_pinned"));
            statement.setString(11, required(row, "status"));
            statement.setInt(12, requiredInt(row, "created_days_ago"));
        });
        batchImport(conn, "post-comments.csv", """
                INSERT INTO POST_COMMENT(comment_id,post_id,member_id,parent_comment_id,content,created_at)
                VALUES(SEQ_POST_COMMENT.NEXTVAL,?,?,?,?,SYSDATE-?)
                """, (statement, row, index) -> {
            requireOrderedId(row, "comment_id", index + 1);
            statement.setLong(1, requiredLong(row, "post_id"));
            statement.setLong(2, requiredMemberId(memberIds, row));
            setNullableLong(statement, 3, row.get("parent_comment_id"));
            statement.setString(4, required(row, "content"));
            statement.setInt(5, requiredInt(row, "created_days_ago"));
        });
        batchImport(conn, "post-likes.csv", """
                INSERT INTO POST_LIKE(post_like_id,post_id,member_id,created_at)
                VALUES(SEQ_POST_LIKE.NEXTVAL,?,?,SYSDATE-?)
                """, (statement, row, index) -> {
            statement.setLong(1, requiredLong(row, "post_id"));
            statement.setLong(2, requiredMemberId(memberIds, row));
            statement.setInt(3, requiredInt(row, "created_days_ago"));
        });
    }

    private static void batchImport(Connection conn, String fileName, String sql,
                                    CsvBinder binder) throws Exception {
        List<Map<String, String>> rows = readCsv(PROJECT_ROOT.resolve("db").resolve(fileName));
        conn.setAutoCommit(false);
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            for (int index = 0; index < rows.size(); index++) {
                binder.bind(statement, rows.get(index), index);
                statement.addBatch();
                if ((index + 1) % 1000 == 0) statement.executeBatch();
            }
            statement.executeBatch();
            conn.commit();
            System.out.printf("%s %,d개 적재됨%n", seedLabel(fileName), rows.size());
        } catch (Exception exception) {
            conn.rollback();
            throw exception;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static String seedLabel(String fileName) {
        return switch (fileName) {
            case "tier-templates.csv" -> "티어리스트 템플릿";
            case "tier-template-items.csv" -> "티어리스트 템플릿 항목";
            case "ideal-templates.csv" -> "이상형 월드컵 템플릿";
            case "ideal-template-items.csv" -> "이상형 월드컵 템플릿 항목";
            case "tier-results.csv" -> "티어리스트 결과";
            case "tier-result-items.csv" -> "티어리스트 결과 항목";
            case "ideal-results.csv" -> "이상형 월드컵 결과";
            case "ideal-result-matches.csv" -> "이상형 월드컵 경기 결과";
            case "posts.csv" -> "커뮤니티 게시글";
            case "post-comments.csv" -> "게시글 댓글";
            case "post-likes.csv" -> "게시글 좋아요";
            default -> fileName;
        };
    }

    private static Map<String, Long> loadMemberIds(Connection conn) throws Exception {
        Map<String, Long> memberIds = new HashMap<>();
        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT member_id, login_id FROM MEMBER")) {
            while (resultSet.next()) {
                memberIds.put(resultSet.getString("login_id"), resultSet.getLong("member_id"));
            }
        }
        return memberIds;
    }

    private static long requiredMemberId(Map<String, Long> memberIds, Map<String, String> row) {
        String loginId = required(row, row.containsKey("creator_login_id") ? "creator_login_id" : "login_id");
        Long memberId = memberIds.get(loginId);
        if (memberId == null) throw new IllegalStateException("CSV에 존재하지 않는 회원이 있습니다: " + loginId);
        return memberId;
    }

    private static long requiredLong(Map<String, String> row, String key) {
        return Long.parseLong(required(row, key));
    }

    private static int requiredInt(Map<String, String> row, String key) {
        return Integer.parseInt(required(row, key));
    }

    private static void setNullableLong(PreparedStatement statement, int index, String value)
            throws Exception {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.NUMERIC);
            return;
        }
        statement.setLong(index, Long.parseLong(value.trim()));
    }

    private static void requireOrderedId(Map<String, String> row, String key, int expected) {
        long actual = requiredLong(row, key);
        if (actual != expected) {
            throw new IllegalStateException(key + " 순서 오류: 예상 " + expected + ", 실제 " + actual);
        }
    }

    @FunctionalInterface
    private interface CsvBinder {
        void bind(PreparedStatement statement, Map<String, String> row, int index) throws Exception;
    }

    private static List<Map<String, String>> readCsv(Path path) throws Exception {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) throw new IllegalStateException("비어 있는 CSV입니다: " + path);
        List<String> headers = parseCsvLine(lines.get(0));
        List<Map<String, String>> rows = new ArrayList<>();
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            if (lines.get(lineNumber).isBlank()) continue;
            List<String> values = parseCsvLine(lines.get(lineNumber));
            if (values.size() != headers.size()) {
                throw new IllegalStateException(path + "의 " + (lineNumber + 1) + "번째 줄 열 개수가 맞지 않습니다.");
            }
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) row.put(headers.get(i), values.get(i));
            rows.add(row);
        }
        return rows;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        if (quoted) throw new IllegalStateException("닫히지 않은 CSV 따옴표가 있습니다.");
        values.add(value.toString());
        return values;
    }

    private static String required(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("CSV 필수 값 누락: " + key);
        return value.trim();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void verifySeedCounts(Connection conn) throws Exception {
        assertCount(conn, "BOOK", 1000);
        assertCount(conn, "MEMBER", 102);
        assertCount(conn, "TIER_TEMPLATE", 8);
        assertCount(conn, "TIER_TEMPLATE_ITEM", 171);
        assertCount(conn, "IDEAL_TEMPLATE", 8);
        assertCount(conn, "IDEAL_TEMPLATE_ITEM", 128);
        assertCount(conn, "RATING", readCsv(PROJECT_ROOT.resolve("db/ratings.csv")).size());
        assertQueryCount(conn,
                "SELECT COUNT(*) FROM (SELECT book_id FROM RATING GROUP BY book_id"
                        + " HAVING COUNT(*) < 20 OR COUNT(*) > 100)",
                0,
                "도서별 평점 개수 범위");
        assertQueryCount(conn,
                "SELECT COUNT(*) FROM (SELECT book_id FROM RATING GROUP BY book_id)",
                1000,
                "평점이 있는 도서");
        assertQueryCount(conn,
                "SELECT COUNT(*) FROM RATING WHERE comment_text IS NULL OR TRIM(comment_text) IS NULL",
                0,
                "후기 내용 누락");
        assertCount(conn, "TIER_LIST", csvSize("tier-results.csv"));
        assertCount(conn, "TIER_ITEM", csvSize("tier-result-items.csv"));
        assertCount(conn, "IDEAL_RUN", csvSize("ideal-results.csv"));
        assertCount(conn, "IDEAL_MATCH", csvSize("ideal-result-matches.csv"));
        assertCount(conn, "POST", csvSize("posts.csv"));
        assertCount(conn, "POST_COMMENT", csvSize("post-comments.csv"));
        assertCount(conn, "POST_LIKE", csvSize("post-likes.csv"));
        assertQueryCount(conn,
                "SELECT COUNT(*) FROM POST P JOIN TIER_LIST T ON T.tier_list_id=P.tier_list_id"
                        + " WHERE P.member_id<>T.member_id",
                0,
                "티어리스트 공유 게시글 작성자");
        assertQueryCount(conn,
                "SELECT COUNT(*) FROM POST P JOIN IDEAL_RUN R ON R.run_id=P.ideal_run_id"
                        + " WHERE P.member_id<>R.member_id",
                0,
                "월드컵 공유 게시글 작성자");
        assertQueryCount(conn, "SELECT COUNT(*) FROM BOOK WHERE published_date IS NULL", 0,
                "출간일 누락 도서");
        assertQueryCount(conn, "SELECT COUNT(*) FROM BOOK WHERE LENGTH(description) > 120", 0,
                "간략 설명 길이 초과 도서");
        assertQueryCount(conn,
                "SELECT COUNT(*) FROM BOOK WHERE image_url IS NULL OR image_url NOT LIKE 'http%'",
                0,
                "외부 표지 URL 누락 도서");
        assertQueryCount(conn, "SELECT COUNT(*) FROM TIER_TEMPLATE WHERE category='장르'", 3,
                "장르 티어 템플릿");
        assertQueryCount(conn, "SELECT COUNT(*) FROM TIER_TEMPLATE WHERE category='작가'", 3,
                "작가 티어 템플릿");
        assertQueryCount(conn, "SELECT COUNT(*) FROM TIER_TEMPLATE WHERE category='자유'", 2,
                "자유 티어 템플릿");
        assertQueryCount(conn, "SELECT COUNT(*) FROM IDEAL_TEMPLATE WHERE category='장르'", 4,
                "장르 월드컵 템플릿");
        assertQueryCount(conn, "SELECT COUNT(*) FROM IDEAL_TEMPLATE WHERE category='자유'", 4,
                "자유 월드컵 템플릿");
        assertQueryCount(conn,
                "SELECT COUNT(*) FROM (SELECT T.template_id FROM TIER_TEMPLATE T"
                        + " JOIN TIER_TEMPLATE_ITEM I ON I.template_id=T.template_id"
                        + " JOIN BOOK B ON B.book_id=I.book_id"
                        + " WHERE T.category='장르' GROUP BY T.template_id HAVING COUNT(DISTINCT B.genre)<>1)",
                0,
                "장르 티어 구성 오류");
        assertQueryCount(conn,
                "SELECT COUNT(*) FROM (SELECT T.template_id FROM TIER_TEMPLATE T"
                        + " JOIN TIER_TEMPLATE_ITEM I ON I.template_id=T.template_id"
                        + " JOIN BOOK B ON B.book_id=I.book_id"
                        + " WHERE T.category='작가' GROUP BY T.template_id HAVING COUNT(DISTINCT B.author_id)<>1)",
                0,
                "작가 티어 구성 오류");
        assertQueryCount(conn,
                "SELECT COUNT(*) FROM (SELECT T.template_id FROM TIER_TEMPLATE T"
                        + " JOIN TIER_TEMPLATE_ITEM I ON I.template_id=T.template_id"
                        + " JOIN BOOK B ON B.book_id=I.book_id"
                        + " WHERE T.category='자유' GROUP BY T.template_id HAVING COUNT(DISTINCT B.genre)<2)",
                0,
                "자유 티어 구성 오류");
        assertQueryCount(conn,
                "SELECT COUNT(*) FROM (SELECT T.template_id FROM IDEAL_TEMPLATE T"
                        + " JOIN IDEAL_TEMPLATE_ITEM I ON I.template_id=T.template_id"
                        + " JOIN BOOK B ON B.book_id=I.book_id"
                        + " WHERE T.category='장르' GROUP BY T.template_id HAVING COUNT(DISTINCT B.genre)<>1)",
                0,
                "장르 월드컵 구성 오류");
        assertQueryCount(conn,
                "SELECT COUNT(*) FROM (SELECT T.template_id FROM IDEAL_TEMPLATE T"
                        + " JOIN IDEAL_TEMPLATE_ITEM I ON I.template_id=T.template_id"
                        + " JOIN BOOK B ON B.book_id=I.book_id"
                        + " WHERE T.category='자유' GROUP BY T.template_id HAVING COUNT(DISTINCT B.genre)<2)",
                0,
                "자유 월드컵 구성 오류");
        assertQueryCount(conn, "SELECT COUNT(DISTINCT member_id) FROM TIER_LIST", 100,
                "티어리스트 참여 회원");
        assertQueryCount(conn, "SELECT COUNT(DISTINCT member_id) FROM IDEAL_RUN", 100,
                "월드컵 참여 회원");
    }

    private static int csvSize(String fileName) throws Exception {
        return readCsv(PROJECT_ROOT.resolve("db").resolve(fileName)).size();
    }

    private static int queryCount(Connection conn, String sql) throws Exception {
        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static void assertCount(Connection conn, String table, int expected) throws Exception {
        assertQueryCount(conn, "SELECT COUNT(*) FROM " + table, expected, table);
    }

    private static void assertQueryCount(Connection conn, String sql, int expected, String label) throws Exception {
        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            int actual = resultSet.getInt(1);
            if (actual != expected) {
                throw new IllegalStateException(label + " 건수 불일치: 예상 " + expected + ", 실제 " + actual);
            }
        }
    }

    private static void runSql(Connection conn, Path path) throws Exception {
        String script = Files.readString(path, StandardCharsets.UTF_8);

        try (Statement stmt = conn.createStatement()) {
            for (String sql : script.split("/\\*END\\*/")) {
                // '/'와 SET DEFINE은 SQL*Plus 명령이며 Oracle SQL 문법이 아니다.
                // JDBC로 실행할 때는 제거해야 한다.
                sql = sql.replaceAll(SQLPLUS_BLOCK_TERMINATOR, "")
                        .replaceAll(SQLPLUS_SET_DEFINE, "")
                        .trim();
                if (sql.isEmpty()) continue;
                if (!sql.toUpperCase().startsWith("BEGIN") && sql.endsWith(";")) {
                    sql = sql.substring(0, sql.length() - 1);
                }
                stmt.execute(sql);
            }
        }
    }
}
