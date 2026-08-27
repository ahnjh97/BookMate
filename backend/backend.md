# Backend

BookMate 백엔드는 Java 21, Jakarta Servlet, JDBC, 내장 Tomcat으로 구성됩니다.

```text
HTTP 요청
  → controller (@WebServlet)
  → service
  → dao
  → DBUtil/HikariCP
  → JDBC
  → 로컬 Oracle
```

## 실행

`Main.java`가 내장 Tomcat을 8080 포트의 루트 컨텍스트로 실행합니다. `frontend` 폴더도 같은 서버의 웹 루트로 연결되므로 별도 프록시나 외부 Tomcat이 필요 없습니다.

```bash
mvn -f backend/pom.xml compile exec:java
```

접속 주소는 `http://localhost:8080/`이며 Servlet API는 `/api/*`입니다.

## DB

프로젝트 루트의 `.env`에서 로컬 Oracle JDBC 설정을 읽습니다.

```env
DB_URL=jdbc:oracle:thin:@localhost:1521:xe
DB_USER=bookmate
DB_PASSWORD=book
```

`util.DBInit`은 `db/schema.sql`과 `db/seed.sql`을 실행합니다. 기존 테이블과 데이터가 삭제되므로 명시적으로 실행할 때만 사용합니다.

## 디렉터리

```text
src/main/java/
├─ controller/  HTTP 요청·응답 및 세션 처리
├─ service/     비즈니스 로직
├─ dao/         JDBC SQL 처리
├─ dto/         계층 간 데이터 전달
├─ exception/   도메인 예외
├─ check/       간단한 실행 검증
└─ util/        DB 연결, DB 초기화, 프로젝트 경로, 비밀번호 처리
```
