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

`Main.java`가 내장 Tomcat을 8080 포트의 루트 컨텍스트로 실행합니다. 로컬 개발에서는 `DevProxyServer`가 `5501` 포트에서 프론트엔드를 제공하고 `/api/*` 요청을 이 백엔드로 전달합니다.

```bash
mvn -f backend/pom.xml compile exec:java
```

백엔드 직접 주소는 `http://localhost:8080/`이며 Servlet API는 `/api/*`입니다. 일반적인 브라우저 테스트는 프로젝트 루트에서 `scripts\proxy`를 실행한 뒤 `http://localhost:5501/`을 사용합니다.

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
