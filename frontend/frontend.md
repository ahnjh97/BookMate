```Shell
frontend/
├── pages/    # 화면별 HTML 프레임 "URL하나 = 파일하나". 사용자가 실제로 접속하는 화면 단위로만 분할.
│             # 로직은 js/pages, 스타일은 assets/css에서 처리
│   ├── auth/
│   ├── book/
│   ├── board/
│   ├── mypage/
│   └── admin/
│
├── components/   # 2곳 이상에서 재사용되는 HTML 조각(헤더 등). 1회용은 pages에 직접.
│   ├── header.html
│   ├── navbar.html
│   ├── footer.html
│   └── loader.html
│
├── js/
│   ├── api/    # 서버 통신 전담(fetch 래퍼 + 도메인별 API 함수) - UI로직 금지
│   │   ├── http.js        # 공통 fetch 래퍼, 통신방식(에러처리, 헤더, 세션만료) 담당
│   │   ├── authApi.js     # 통신대상(회원 관련 엔드포인트만) — http.js 가져다 쓰기만 함
│   │   ├── bookApi.js
│   │   └── postApi.js
│   │
│   ├── components/ # "UI구현 재사용 JS 로직". components/*.html과 세트로 동작
│   │   ├── genreFilter.js
│   │   ├── pagination.js
│   │   └── toast.js
│   │
│   ├── utils/      # "화면·통신과 무관한, 순수 계산/변환 함수". 입→출력만 있고 화면·서버와 무관
│   │   ├── includePartial.js
│   │   ├── validators.js
│   │   └── formatters.js
│   │
│   └── pages/      # "딱 1개 화면에서만 쓰이는 로직". 재사용 안 되는 건 전부 여기로 — 여기 있는 코드가
│                   # 나중에 2번째 화면에서도 필요해지면, 그때 components나 utils로 "승격"
│       ├── login.js
│       └── bookList.js
│
├── assets/         # CSS/이미지/아이콘
│
├── frontend.md # 폴더 배치 기준·fetch래퍼·디자인토큰 상세?
```
