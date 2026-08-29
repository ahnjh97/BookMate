# BookMate

정적 HTML/CSS/JavaScript, Jakarta Servlet, JDBC, 로컬 Oracle로 구성된 CRUD 미니프로젝트입니다. 개발 환경에서는 내장 Tomcat 백엔드를 `8080`에서 실행하고, `DevProxyServer`가 프론트엔드를 `5501`에서 제공하면서 `/api/*` 요청을 백엔드로 전달합니다.

## 실행 구조

```text
브라우저 http://localhost:5501/
        ↓
DevProxyServer
        ├─ /pages, /assets, /js  → frontend 정적 파일
        └─ /api/*                → http://localhost:8080/api/*
                                      ↓
                                Jakarta Servlet
                                      ↓
                                Service → DAO → JDBC → 로컬 Oracle
```

로컬 개발의 기본 접속 주소는 `http://localhost:5501`입니다. 내장 Tomcat은 백엔드와 직접 확인용 정적 파일을 `8080`에서 제공하지만, 일반적인 화면 개발과 테스트는 프록시를 통해 진행합니다. Docker와 외부 Tomcat은 사용하지 않습니다.

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

DB 초기화는 기존 BookMate 테이블과 데이터를 삭제한 뒤 `db/schema.sql`로 스키마를 만들고 `db` 폴더의 CSV를 다시 적재합니다. 필요한 데이터가 있으면 먼저 백업하세요.

IntelliJ에서는 `util.DBInit`을 실행합니다. 프로젝트 루트와 `backend` 작업 디렉터리를 모두 지원합니다.

터미널에서는 다음 스크립트를 사용할 수 있습니다.

```bat
scripts\db-init.bat
```

```bash
bash scripts/db-init.sh
```

초기화가 완료되면 다음 개발 데이터가 준비됩니다.

- 승인 도서 1,000권
- 테스트 회원 `user1`~`user100` 100명
- 테스트 회원 공통 비밀번호: `qwerasdf`

### 책 표지 이미지

- 목록·통계용: `frontend/assets/images/books/{bookId}-240.webp`
- 상세·월드컵 선택용: `frontend/assets/images/books/{bookId}-520.webp`
- 티어·월드컵 템플릿 콜라주: `frontend/assets/images/templates/`
- 전체 표지와 콜라주 재생성: `python scripts/build-book-images.py`
- 표지를 내려받거나 이미지로 검증하지 못한 책이 하나라도 있으면 생성 작업이 실패합니다.
- 승인된 티어리스트 템플릿 8개와 회원별 참여 결과
- 이상형 월드컵 템플릿 8개와 회원별 최신 참여 결과
- 회원별 약 100개의 평점

초기화 데이터는 모두 `db` 폴더의 CSV에서 관리합니다. 도서·회원·평점과 후기뿐 아니라 티어리스트 및 이상형 월드컵의 템플릿, 항목, 참여 결과도 각각의 CSV로 분리되어 있습니다. `schema.sql`에는 테이블·시퀀스·인덱스 정의만 둡니다.
도서 CSV를 다시 생성하려면 PowerShell에서 다음 명령을 실행합니다.

```powershell
./scripts/generate-book-seed.ps1
```

## 애플리케이션 실행

### IntelliJ IDEA Community

1. `backend/pom.xml`을 Maven 프로젝트로 불러옵니다.
2. 로컬 Oracle이 실행 중인지 확인합니다.
3. `backend/src/main/java/Main.java`를 실행합니다.
4. 프로젝트 루트에서 `scripts\proxy`를 실행합니다.
5. 브라우저에서 `http://localhost:5501/`에 접속합니다.

`Main`은 실행 위치를 기준으로 프로젝트 루트를 자동 탐색하므로 별도의 Tomcat 실행 구성이나 `/bookmate` 컨텍스트 설정이 필요 없습니다.

### 터미널

첫 번째 터미널에서 백엔드를 실행합니다.

```bat
scripts\start.bat
```

```bash
bash scripts/start.sh
```

두 번째 터미널에서 프론트 개발 프록시를 실행합니다.

```bat
scripts\proxy
```

백엔드는 Maven 명령으로 직접 실행할 수도 있습니다.

```bash
mvn -f backend/pom.xml compile exec:java
```

백엔드와 프록시는 각각 실행 중인 터미널에서 `Ctrl+C`로 종료합니다.

## 주요 URL

- 메인: `http://localhost:5501/`
- 책 목록: `http://localhost:5501/pages/book/list.html`
- 로그인: `http://localhost:5501/pages/auth/login.html`
- 회원가입: `http://localhost:5501/pages/auth/signup.html`
- 책 API: `GET http://localhost:5501/api/books`
- 인증 상태: `GET http://localhost:5501/api/auth`
- 백엔드 직접 확인: `http://localhost:8080/`

## 프론트 개발 프록시

백엔드를 `8080` 포트에서 먼저 실행한 다음, 프로젝트 루트에서 아래 명령을 실행합니다.

```bat
scripts\proxy
```

`proxy.bat`이 `scripts` 폴더 이동, `DevProxyServer.java` 컴파일, 실행을 모두 처리합니다. 브라우저에서는 `http://localhost:5501`에 접속합니다.

- 프론트엔드 정적 파일: `frontend` 폴더에서 제공
- `/api/*` 요청: `http://localhost:8080`으로 전달
- 프록시 종료: 실행 중인 터미널에서 `Ctrl+C`
- 백엔드 주소 변경: `BACKEND_URL` 환경변수 사용

HTML/CSS/JavaScript 수정은 브라우저 새로고침으로 반영됩니다. 백엔드 Java 코드를 수정하면 백엔드를 다시 시작하고, `DevProxyServer.java`를 수정하면 `scripts\proxy`를 다시 실행합니다.
