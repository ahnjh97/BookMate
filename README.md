```Shell
bookmate/
├── .env                           # 환경변수: DB 등 민감정보 통합 관리 (gitignore 대상)
├── .env.example                   # 팀 공유용 템플릿
├── .gitattributes                 # Windows/Mac 줄바꿈 통일: * text=auto eol=lf
├── .gitignore
├── README.md                      # 프로젝트 개요, 필수 환경(JDK버전 등) 명시
├── docker-compose.yml             # .env 참조
│
├── backend/                       # Java + JDBC, 4계층 구조(controller-service-dao-dto)
│   ├── pom.xml                    # 라이브러리 버전 고정(Gson/HikariCP/jBCrypt 등)
│   └── src/main/
│       ├── java
│       │   ├── controller/        # HTTP 요청/응답만 담당, 로직은 service에 위임
│       │   ├── service/           # 비즈니스 로직 전담(권한검증, 베이지안 계산, 취향매칭 등)
│       │   ├── dao/               # JDBC 쿼리 전담, DB와 직접 대화
│       │   ├── dto/               # 계층 간 데이터 전달 객체(DB 행 표현)
│       │   ├── filter/            # 세션 검증, 요청 속도 제한(로그인 무차별대입 방지)
│       │   └── util/              # DBUtil(HikariCP), JsonUtil, ValidationUtil, DB_init(DB 스키마 초기화)
│       └── resources/
│           └── config.properties  # 루트 .env 값을 읽어오는 설정
│
├── frontend/                      # HTML + CSS + 바닐라 JavaScript
│   ├── pages/                     # 화면별 html
│   ├── components/                # 2곳 이상에서 재사용되는 HTML 조각(헤더 등)
│   ├── js/
│   │   ├── api/                   # 서버 통신 전담(fetch 래퍼 + 도메인별 API 함수)
│   │   ├── components/            # 재사용 UI의 JS 로직
│   │   ├── utils/                 # 화면·통신과 무관한 순수 함수
│   │   └── pages/                 # 화면 1개 전용 스크립트
│   └── assets/                    # CSS/이미지/아이콘
│
├── db/                           # Oracle DB 컨테이너 정의
│   ├── schema.sql                # 테이블 생성(DDL) — 순서대로 한 파일에 다 넣어도 무방
│   └── seed.sql                  # 관리자 계정 등 초기 데이터
│
├── scripts/                       # 개발 및 로컬 실행 자동화 스크립트
│   ├── start.sh / start.bat       # DB 컨테이너 실행
│   ├── reset.sh / reset.bat       # DB 컨테이너와 볼륨 초기화
│   └── tomcat-run.sh / .bat       # WAR 빌드·배포 후 Tomcat 터미널 실행
│
└── docs/
    ├── README.md                   # 산출물 인덱스(노션/Figma 링크 모음)
    ├── requirements.md             # 요구사항정의서 + IA/유저플로우
    ├── screen_spec.md              # 화면정의서(Figma 링크+설명)
    ├── erd.mmd                     # ERD(텍스트로 관리하는 다이어그램)
    ├── table_spec.md               # 테이블정의서 + DB 네이밍규칙
    ├── interface_spec.md           # 인터페이스정의서
    └── coding_convention.md        # 코딩컨벤션
```

## 웹 화면 실행

Spring 없이 Tomcat 10.1이 HTML/CSS/JavaScript와 Jakarta Servlet을 실행합니다.

필수 환경: Java 21, Maven, Apache Tomcat 10.1

### 터미널에서 실행 — 모든 IntelliJ 에디션

IntelliJ의 Tomcat 통합 기능 없이도 실행할 수 있습니다. Java 21과 Docker Desktop만 먼저 설치하면 됩니다. Windows 실행 스크립트는 Maven과 Apache Tomcat을 찾지 못할 경우 프로젝트의 `.tools` 폴더에 정식 안정판 Maven 3.9.16과 Tomcat 10.1.59를 최초 1회 자동 설치합니다.

> **Windows에서 Tomcat을 실행하려면 `scripts\tomcat-run.bat` 파일을 더블클릭하세요.** `scripts\start.bat`은 데이터베이스만 실행하며, 웹 애플리케이션과 Tomcat은 `tomcat-run.bat`이 실행합니다.

1. `.env.example`을 복사해 프로젝트 루트에 `.env`를 만듭니다.
2. Docker Desktop을 실행한 뒤 DB 컨테이너를 시작합니다.

   Windows:

   ```bat
   scripts\start.bat
   ```

   macOS/Linux:

   ```bash
   bash scripts/start.sh
   ```

3. 애플리케이션을 실행합니다.

   Windows:

   ```bat
   scripts\tomcat-run.bat
   ```

   Windows에서는 `scripts\tomcat-run.bat`을 더블클릭해도 됩니다. 경로 입력은 필요하지 않으며, 오류가 발생해도 원인을 확인할 수 있도록 창이 유지됩니다.

   macOS/Linux:

   ```bash
   bash scripts/tomcat-run.sh /opt/apache-tomcat-10.1.x
   ```

   macOS/Linux 스크립트는 Tomcat 경로를 인자로 받습니다. `CATALINA_HOME` 또는 `TOMCAT_HOME` 환경변수를 등록하면 경로 인자 없이 실행할 수 있습니다.

4. `http://localhost:8080/bookmate/`에 접속합니다. 서버를 종료할 때는 Tomcat이 실행 중인 터미널에서 `Ctrl+C`를 누릅니다.

스크립트는 실행할 때마다 Maven으로 `bookmate.war`를 빌드하고 Tomcat의 `webapps` 폴더에 배포합니다. 프로젝트 루트의 `.env` 위치도 Tomcat에 자동으로 전달합니다.

### 프론트엔드 실시간 반영 개발 모드 — Windows

먼저 `scripts\tomcat-run.bat`을 한 번 실행해 Maven과 Tomcat을 준비합니다. 이후 평소 프론트엔드 개발에는 다음 파일을 실행합니다.

```bat
scripts\tomcat-dev.bat
```

`tomcat-dev.bat`이 실행되지 않거나 Maven 또는 Tomcat을 찾을 수 없다는 메시지가 나오면, `tomcat-run.bat`을 먼저 한 번 실행한 뒤 다시 시도합니다. `tomcat-run.bat`이 필요한 개발 도구를 프로젝트의 `.tools` 폴더에 준비합니다.

개발 모드는 `frontend` 폴더 전체를 Tomcat의 웹 루트에 직접 연결합니다. 기존 폴더뿐 아니라 나중에 새 폴더를 추가해도 별도 설정 없이 실시간 반영 대상에 포함됩니다. HTML, CSS, JavaScript 파일은 저장한 뒤 브라우저를 새로고침하면 재빌드 없이 반영됩니다. Java 파일을 변경한 경우에는 실행 중인 서버를 종료하고 `tomcat-dev.bat`을 다시 실행해야 합니다.

실제 로그인 기능이 완성되기 전까지 개발 모드의 navbar 로그인 버튼은 임시 개발 회원을 생성하거나 조회해 서버 세션에 로그인 상태를 저장합니다. 이 기능은 `tomcat-dev.bat`으로 실행했을 때만 활성화되며 일반 `tomcat-run.bat` 실행에서는 사용할 수 없습니다.

> **임시 인증 제거 필요:** 실제 회원가입·로그인·로그아웃 기능이 병합되면 개발용 인증 API(`controller/dev`, `DevAuthService`, `DevAuthDAO`)와 navbar의 개발용 인증 코드를 삭제해야 합니다. `tomcat-dev.ps1`의 `BOOKMATE_DEV_MODE` 설정도 함께 제거하고, 평점 기능은 실제 로그인에서 생성한 `loginMemberId` 세션을 사용하도록 확인합니다.

커밋 또는 병합 전에는 `scripts\tomcat-run.bat`으로 clean WAR 빌드와 실제 배포 구성을 최종 확인합니다.

### IntelliJ IDEA Ultimate 또는 평가판

1. `backend/pom.xml`을 Maven 프로젝트로 불러옵니다.
2. `Run > Edit Configurations`에서 `Tomcat Server > Local`을 추가합니다.
3. Application server에 설치한 Tomcat 10.1을 지정합니다.
4. Deployment에 `backend:war exploded`를 추가하고 context path를 `/bookmate`로 설정합니다.
5. Tomcat을 실행하고 `http://localhost:8080/bookmate/`로 접속합니다.

서버를 다시 시작할 때는 IntelliJ의 동일한 Tomcat 실행 설정에서 `Rerun`을 사용합니다.

## IntelliJ Maven으로 책 목록 연결 확인

1. IntelliJ에서 `backend/pom.xml`을 Maven 프로젝트로 불러오고 Maven 창에서 `Reload All Maven Projects`를 실행합니다.
2. `util.DBInit` 실행 설정을 만들고 Working directory를 프로젝트 루트(`book-inven`)로 지정한 뒤 실행합니다.
   이 작업은 기존 테이블을 삭제하고 `db/schema.sql`, `db/seed.sql` 순서로 다시 생성하므로 필요한 데이터가 있으면 먼저 백업합니다.
3. Maven 창의 `backend > Lifecycle > package`를 실행합니다.
4. Tomcat 10.1 실행 설정의 Deployment에 `backend:war exploded`, context path에 `/bookmate`를 지정합니다.
5. Tomcat이 프로젝트 루트의 `.env`를 읽지 못하면 실행 설정의 Environment variables에 `DB_URL`, `DB_USER`, `DB_PASSWORD`를 등록합니다.
6. `http://localhost:8080/bookmate/pages/book/list.html`에서 전체 목록, 제목/작가 검색, 장르 필터를 확인합니다.
7. 목록의 책 표지나 제목을 선택해 상세 화면에서 책·작가·출판·평점 정보를 확인합니다.

책 API는 `GET /bookmate/api/books`를 사용하며 `keyword`, `genre` 쿼리 파라미터를 지원합니다. 책 한 권은 `GET /bookmate/api/books/{bookId}`로 조회할 수 있습니다.
