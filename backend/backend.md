```
backend/
├── pom.xml                        # groupId: bookmate, 라이브러리 버전 고정
├── backend.md                     # 계층 구조·호출규칙·라이브러리·API포맷 상세
└── src/main/
    ├── java
    │   ├── check/                  # JUnit 대용 순수 로직 검정 자체 프레임워크(AllChecks: 진입점)
    │   ├── controller/             # HTTP 요청/응답만 담당, 로직은 service에 위임
    │   ├── service/                # 비즈니스 로직 전담(try-catch: 권한검증, 베이지안 계산, 취향매칭 등)
    │   ├── dao/                    # JDBC 쿼리 전담, DB핸들링
    │   ├── dto/                    # 계층 간 데이터 전달 객체(DB 행 표현)
    │   ├── exception/              # 커스텀 예외 클래스(GlobalExceptionFilter: 타입별 HTTP 상태코드 매핑)
    │   │
    │   ├── filter/                 # 세션 검증, 요청 속도 제한
    │   │   ├── SessionFilter.java           # 로그인 여부 확인
    │   │   ├── RateLimitFilter.java         # 요청 속도 제한
    │   │   └── GlobalExceptionFilter.java   # 예외 → 공통 응답 변환
    │   │
    │   └── util/                   # DBUtil(HikariCP), JsonUtil, ValidationUtil
    │       ├── DBUtil.java                  # DB 접속 로직 재사용 코드
    │       ├── ResponseWrapper.java         # 응답 형식 통일
    │       └── ValidationUtil.java          # 입력값 검증
    │
    ├── resources/
    │   └── config.properties       # .env 값 로드
    │
    └── webapp/
        └── WEB-INF/
            └── web.xml              # Servlet 스펙이 강제하는 고정 위치, Main.java가 이 폴더를 웹 루트로 등록
```

| 파일                  | 동작 방식                                                      | 배치 이유                                                            | 없을 때 문제                                    |
| --------------------- | -------------------------------------------------------------- | -------------------------------------------------------------------- | ----------------------------------------------- |
| SessionFilter         | 세션에 로그인 정보 있는지 확인, 없으면 401 응답 후 차단        | 전 기능 공통 로직이라 controller 앞단에서 한 번만 처리               | controller마다 로그인 확인 코드 반복            |
| RateLimitFilter       | 같은 세션/IP가 짧은 시간에 반복 요청하면 429 응답 후 차단      | 로그인 무차별대입·도배 등 전 기능에 걸친 위협이라 공통 처리         | 무차별 시도를 막을 수단 없음                    |
| GlobalExceptionFilter | service/dao가 던진 예외를 붙잡아 ResponseWrapper 형식으로 응답 | 예외처리도 전 기능 공통이라 한 곳으로 모음                           | controller마다 try-catch 반복, 응답 형식 제각각 |
| DBUtil                | HikariCP 초기화, 모든 dao가 여기서만 커넥션을 가져감           | DB 연결은 앱 전체에서 하나로 통일 관리                               | dao마다 연결 코드 중복                          |
| ResponseWrapper       | {success, data, message} 고정 포맷 응답 생성                   | 담당자별로 응답 모양이 달라지면 프론트가 기능마다 다르게 처리해야 함 | 응답 형식 불일치                                |
| ValidationUtil        | 이메일형식·길이 등 검증 함수 모음                             | 회원가입·게시글 등 여러 기능에서 재사용                             | 검증 로직 중복 작성                             |
| config.properties     | 루트 .env 값을 읽어옴                                          | DB 비밀번호 등을 코드에 직접 안 쓰기 위함                            | 민감정보 코드 노출                              |
