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
├── frontend/                      # 바닐라 JS + Tailwind (상세: frontend/frontend.md)
│   ├── pages/                     # 화면별 html
│   ├── components/                # 2곳 이상에서 재사용되는 HTML 조각(헤더 등)
│   ├── js/
│   │   ├── api/                   # 서버 통신 전담(fetch 래퍼 + 도메인별 API 함수)
│   │   ├── components/            # 재사용 UI의 JS 로직
│   │   ├── utils/                 # 화면·통신과 무관한 순수 함수
│   │   └── pages/                 # 화면 1개 전용 스크립트
│   ├── assets/                    # 이미지/아이콘
│   └── tailwind.config.js
│
├── db/                           # Oracle DB 컨테이너 정의
│   ├── schema.sql                # 테이블 생성(DDL) — 순서대로 한 파일에 다 넣어도 무방
│   └── seed.sql                  # 관리자 계정 등 초기 데이터
│
├── scripts/                       # 개발 및 로컬 테스트용 docker 자동화 스크립트
│   ├── start.sh / start.bat       # 컨테이너 실행 + schema.sql, seed.sql 적용까지 먼저 자동화
│   └── dev_reset.sh               # 초기화하고 싶을 때(볼륨 삭제 후 재생성)
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
