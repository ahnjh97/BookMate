# Backend

BookMate: Java 21, Jakarta Servlet, JDBC, 내장 Tomcat으로 구성

## 아키텍처

```text
HTTP 요청
  → controller   (@WebServlet, 요청/응답만 담당)
  → service      (비즈니스 로직 전담)
  → dao          (JDBC 쿼리 전담)
  → DBUtil / HikariCP
  → JDBC
  → 로컬 Oracle
```

계층별 책임과, 여러 기능에 걸쳐 반복되는 로직을 어디에 모았는지는 아래 [디렉터리 구조](#디렉터리-구조)에 정리했습니다.

## 실행

`Main.java`가 내장 Tomcat을 **8080 포트, 루트 컨텍스트(`/`)**로 직접 실행
`DevProxyServer`가 `5501` 포트에서 프론트엔드를 제공하고 `/api/*` 요청을 이 백엔드로 전달합니다.

```bash
mvn -f backend/pom.xml compile exec:java
```

| 접속 대상        | 주소                       | 용도                                                |
| ---------------- | -------------------------- | --------------------------------------------------- |
| 백엔드 직접      | `http://localhost:8080/` | API(`/api/*`) 직접 호출·디버깅                   |
| 로컬 화면 테스트 | `http://localhost:5501/` | `scripts/proxy` 실행 후, 프론트엔드+API 통합 확인 |

로컬 개발 환경에서는 `DevProxyServer`가 5501 포트에서 프론트엔드 정적 파일을 제공하고,
`/api/*` 요청만 백엔드(8080)로 전달합니다. 배포 환경의 리버스 프록시 역할을 로컬에서 재현합니다.
백엔드 직접 주소는 `http://localhost:8080/`이며 Servlet API는 `/api/*`입니다.
브라우저 테스트는 `scripts\proxy.bat`를 실행한 뒤 `http://localhost:5501/`로 접근

## DB

프로젝트 루트의 `.env`에서 로컬 Oracle JDBC 접속정보를 읽습니다

```env
DB_URL=jdbc:oracle:thin:@localhost:1521:xe
DB_USER=<값>
DB_PASSWORD=<값>
```

`util.DBInit`은 `db/schema.sql`, `db/seed.sql`을 실행해 스키마와 초기 데이터 구성
**기존 테이블과 데이터를 모두 삭제하므로, 명시적으로 초기화가 필요할 때만 실행**

## 디렉터리 구조

```text
backend/
├── backend.md
├── pom.xml                    # groupId: bookmate, 의존성 버전 고정 등(Gson/HikariCP/jBCrypt 등)
└── src/main/
    ├── java/
    │   ├── controller/        # HTTP 요청·응답, 세션 처리
    │   ├── service/           # 비즈니스 로직 전담(권한검증, 베이지안 계산, 취향매칭 등)
    │   ├── dao/               # JDBC SQL 처리
    │   ├── dto/               # 계층 간 데이터 전달 객체(DB 행 표현)
    │   ├── exception/         # 커스텀 예외 (GlobalExceptionFilter가 HTTP 상태코드로 변환)
    │   ├── filter/            # 세션 검증·요청 속도 제한·예외 처리
    │   ├── check/             # 서버 기동 시 자동 검증
    │   └── util/              # DB 연결·초기화, 프로젝트 경로, 응답 포맷
    │					       # DBUtil(HikariCP), JsonUtil, ValidationUtil, DB_init(DB 스키마 초기화)
    ├── resources/   # 백엔드 설정 파일 등
    └── webapp/WEB-INF/web.xml # Servlet 스펙 고정 위치, Main.java가 웹 루트로 등록
```

controller는 요청을 해석하고 응답을 조립할 뿐 로직을 갖지 않으며, 비즈니스 로직은 전부 service에,
SQL 실행은 dao에 위치합니다. 여러 기능에 걸쳐 반복될 로직은 아래처럼 별도 계층으로 모아
controller마다 같은 코드가 중복되지 않도록 합니다.

| 컴포넌트                  | 역할                                                  | 배치 이유                                                                        |
| ------------------------- | ----------------------------------------------------- | -------------------------------------------------------------------------------- |
| `SessionFilter`         | 세션에 로그인 정보가 없으면 401로 차단                | 로그인 확인이 전 기능 공통이라 controller 앞단에서 한 번만 처리                  |
| `RateLimitFilter`       | 짧은 시간 반복 요청을 429로 차단                      | 무차별 대입·도배는 특정 기능이 아닌 API 전체의 공통 위협                        |
| `GlobalExceptionFilter` | service/dao의 예외를 공통 응답 포맷으로 변환          | 에러 응답 형식이 controller마다 달라지는 걸 방지                                 |
| `DBUtil`                | HikariCP 초기화, 모든 dao가 여기서만 커넥션을 가져감  | DB 연결 로직을 한 곳으로 통일                                                    |
| `ResponseWrapper`       | `{success, data, message}` 고정 포맷 응답 생성      | 프론트가 기능마다 다른 응답 형식을 처리하지 않도록 통일                          |
| `ValidationUtil`        | 이메일 형식·길이 등 입력값 검증 함수 모음            | 회원가입·게시글 등 여러 기능에서 중복 없이 재사용                               |
| `check.AllChecks`       | 서버 기동 시 자동으로 API 동작 검증, 콘솔에 결과 출력 | `Checkable` 구현체를 등록만 하면 검증이 추가됨 - JUnit 없이도 눈으로 즉시 확인 |
| ,                         |                                                       |                                                                                  |
