```mermaid
erDiagram
    MEMBER ||--o| AUTHOR_ACCOUNT : "작가 인증 시 보유"
    AUTHOR ||--o{ AUTHOR_ACCOUNT : "카탈로그와 매칭(선택)"
    MEMBER ||--o{ POST : "작성"

    MEMBER {
        NUMBER member_id PK
        VARCHAR2 login_id UK "로그인 아이디"
        VARCHAR2 password "jBCrypt 해시"
        VARCHAR2 nickname UK "공개 표시명"
        VARCHAR2 email UK
        VARCHAR2 role "USER / ADMIN"
        NUMBER fail_count
        CHAR is_locked "Y/N"
        DATE last_login_at
        DATE created_at
    }

    AUTHOR {
        NUMBER author_id PK
        VARCHAR2 author_name "실존 저자명(카탈로그)"
        VARCHAR2 bio "공식 소개, 관리자만 수정"
    }

    AUTHOR_ACCOUNT {
        NUMBER member_id PK "MEMBER 참조 겸용"
        NUMBER linked_author_id FK "NULL 허용"
        DATE verified_at
        VARCHAR2 self_intro "작가 본인 작성, 본인만 수정"
        DATE self_intro_updated_at
    }

    POST {
        NUMBER post_id PK
        NUMBER member_id FK
        VARCHAR2 category "FREE/REVIEW/RECOMMEND/TIER/AUTHOR/NOTICE"
        VARCHAR2 title
        CLOB content
        VARCHAR2 genre "자유텍스트, GENRE 연동 여부 확인필요"
        VARCHAR2 tag
        NUMBER view_count
        CHAR is_spoiler "Y/N"
        CHAR is_pinned
        VARCHAR2 status
        DATE created_at
        DATE updated_at
    }
```

**관계 표기**

| 관계   | 의미 |
| ------ | ---- |
| MEMBER |      |
| AUTHOR |      |
| MEMBER |      |

J/L 파트 테이블(BOOK, GENRE, RATING, TIER_LIST/ITEM, REPORT, COMMENT) 확정시 이어서 관계 추가필요
POST.book_id같은  FK 등
