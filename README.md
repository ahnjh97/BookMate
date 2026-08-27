# BookMate

정적 HTML/CSS/JavaScript, Jakarta Servlet, JDBC, 로컬 Oracle로 구성된 CRUD 미니프로젝트입니다. Java `Main`이 내장 Tomcat을 실행하고 프론트 화면과 `/api/*` Servlet을 같은 8080 포트에서 제공합니다.

## 실행 구조

```text
브라우저 http://localhost:8080/
        ├─ /pages, /assets, /js  → frontend 정적 파일
        └─ /api/*                → Jakarta Servlet
                                      ↓
                                Service → DAO → JDBC → 로컬 Oracle
```

Docker, 별도 프론트 서버, 리버스 프록시, 외부 Tomcat은 사용하지 않습니다.

## 필수 환경

- JDK 21
- Maven 3.9 이상
- 로컬 Oracle Database
- IntelliJ IDEA Community 또는 Ultimate(선택)

## DB 설정

1. `.env.example`을 복사해 프로젝트 루트에 `.env`를 만듭니다.
2. 로컬 Oracle 계정에 맞게 값을 수정합니다.

```env
DB_URL=jdbc:oracle:thin:@localhost:1521:xe
DB_USER=bookmate
DB_PASSWORD=book
```

서비스명 접속을 사용하는 경우 URL 예시는 `jdbc:oracle:thin:@localhost:1521/XEPDB1`입니다.

### DB 초기화

DB 초기화는 기존 BookMate 테이블과 데이터를 삭제한 뒤 `db/schema.sql`, `db/seed.sql`을 다시 실행합니다. 필요한 데이터가 있으면 먼저 백업하세요.

IntelliJ에서는 `util.DBInit`을 실행합니다. 프로젝트 루트와 `backend` 작업 디렉터리를 모두 지원합니다.

터미널에서는 다음 스크립트를 사용할 수 있습니다.

```bat
scripts\db-init.bat
```

```bash
bash scripts/db-init.sh
```

## 애플리케이션 실행

### IntelliJ IDEA Community

1. `backend/pom.xml`을 Maven 프로젝트로 불러옵니다.
2. 로컬 Oracle이 실행 중인지 확인합니다.
3. `backend/src/main/java/Main.java`를 실행합니다.
4. 브라우저에서 `http://localhost:8080/`에 접속합니다.

`Main`은 실행 위치를 기준으로 프로젝트 루트를 자동 탐색하므로 별도의 Tomcat 실행 구성이나 `/bookmate` 컨텍스트 설정이 필요 없습니다.

### 터미널

```bat
scripts\start.bat
```

```bash
bash scripts/start.sh
```

또는 Maven 명령을 직접 사용할 수 있습니다.

```bash
mvn -f backend/pom.xml compile exec:java
```

서버 종료는 실행 중인 터미널에서 `Ctrl+C`를 누릅니다.

## 주요 URL

- 메인: `http://localhost:8080/`
- 책 목록: `http://localhost:8080/pages/book/list.html`
- 로그인: `http://localhost:8080/pages/auth/login.html`
- 회원가입: `http://localhost:8080/pages/auth/signup.html`
- 책 API: `GET http://localhost:8080/api/books`
- 인증 상태: `GET http://localhost:8080/api/auth`

프론트엔드 파일은 `frontend` 폴더가 내장 Tomcat에 직접 연결되므로 HTML/CSS/JavaScript 수정 후 브라우저를 새로고침하면 반영됩니다. Java 코드를 수정한 경우 서버를 다시 시작합니다.
