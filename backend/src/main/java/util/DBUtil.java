// 파일: util/DBUtil.java
// 목적: DB 접속 로직을 한 곳에서만 관리(HikariCP 커넥션풀)
// 모든 dao는 이 클래스의 getConnection()만 호출해서 연결을 가져다 씀
// HikariCP 관련 코드는 이 파일에만 존재 — 다른 계층(dao/service 등)은
// 커넥션 풀이 뭔지 몰라도 되고, 나중에 라이브러리를 바꿔도 이 파일만 수정

package util;

import java.sql.Connection;      // HikariCP 설정값(URL/계정/풀크기 등)을 담는 객체
import java.sql.SQLException;  // 실제 커넥션 풀 — Connection을 빌려주고 반납받는 주체

import com.zaxxer.hikari.HikariConfig;   // 루트 .env 파일을 읽어오는 라이브러리
import com.zaxxer.hikari.HikariDataSource;                 // DB 연결 객체 타입(자바 표준 API)

import io.github.cdimascio.dotenv.Dotenv;               // DB 연결 실패 시 발생하는 예외 타입(자바 표준 API)

public class DBUtil {
    // static + final: 앱 전체에서 커넥션 풀은 유일
    // 요청이 반복되고 쌓여도 하나를 계속 재사용
    private static final HikariDataSource dataSource;
    // static : 앱이 시작하고 클래스가 로드될때 한번만 실행되며 커넥션 풀을 미리 준비
    static {
        // 시스템 환경변수를 우선인식하고 .env 가 있으면 경로에서 인식
        Dotenv dotenv = Dotenv.configure().directory("./").load();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dotenv.get("DB_URL"));
        config.setUsername(dotenv.get("DB_USER"));
        config.setPassword(dotenv.get("DB_PASSWORD"));
        config.setMaximumPoolSize(10);

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }


    // TEMP: 코드검증
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("DB 연결 성공: " + conn.getMetaData().getURL());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}