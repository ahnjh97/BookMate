```mermaid
MEMBER ||--o| AUTHOR_ACCOUNT : "작가 인증 시 보유(선택)"
AUTHOR ||--o| AUTHOR_ACCOUNT : "카탈로그 매칭(선택, UK)"
MEMBER ||--o{ AUTHOR_ACCOUNT_REQUEST : "인증 신청"
AUTHOR |o--o{ AUTHOR_ACCOUNT_REQUEST : "매칭 대상(선택)"
MEMBER |o--o{ AUTHOR_ACCOUNT_REQUEST : "승인 처리(선택)"
AUTHOR ||--o{ AUTHOR_REVIEW : "작가 후기 대상"
MEMBER ||--o{ AUTHOR_REVIEW : "후기 작성"

AUTHOR ||--o{ BOOK : "저술(필수)"
MEMBER ||--o{ BOOK_REQUEST : "도서 등록 요청"
MEMBER |o--o{ BOOK_REQUEST : "검토(선택)"
BOOK ||--o{ RATING : "평가 대상"
MEMBER ||--o{ RATING : "평점 작성"

MEMBER ||--o{ TIER_TEMPLATE : "템플릿 신청"
MEMBER |o--o{ TIER_TEMPLATE : "승인 처리(선택)"
TIER_TEMPLATE ||--o{ TIER_TEMPLATE_ITEM : "구성 도서"
BOOK ||--o{ TIER_TEMPLATE_ITEM : "템플릿 포함"
MEMBER ||--o{ TIER_LIST : "개인 리스트 생성"
TIER_TEMPLATE ||--o{ TIER_LIST : "템플릿 기반(UK)"
TIER_LIST ||--o{ TIER_ITEM : "구성 도서"
BOOK ||--o{ TIER_ITEM : "리스트 배치"

MEMBER ||--o{ IDEAL_TEMPLATE : "월드컵 템플릿 신청"
MEMBER |o--o{ IDEAL_TEMPLATE : "승인 처리(선택)"
IDEAL_TEMPLATE ||--o{ IDEAL_TEMPLATE_ITEM : "구성 도서"
BOOK ||--o{ IDEAL_TEMPLATE_ITEM : "템플릿 포함"
IDEAL_TEMPLATE ||--o{ IDEAL_RUN : "템플릿으로 진행(UK)"
MEMBER ||--o{ IDEAL_RUN : "월드컵 진행"
BOOK ||--o{ IDEAL_RUN : "우승 도서"
IDEAL_RUN ||--o{ IDEAL_MATCH : "대진 기록"
BOOK ||--o{ IDEAL_MATCH : "왼쪽 도서"
BOOK ||--o{ IDEAL_MATCH : "오른쪽 도서"
BOOK ||--o{ IDEAL_MATCH : "승리 도서"

MEMBER ||--o{ POST : "게시글 CRUD"
TIER_LIST |o--o| POST : "리스트 공유(선택, 1:1)"
IDEAL_RUN |o--o| POST : "결과 공유(선택, 1:1)"
POST ||--o{ POST_COMMENT : "댓글 CRUD"
MEMBER ||--o{ POST_COMMENT : "댓글 작성"
POST_COMMENT |o--o{ POST_COMMENT : "대댓글(선택)"
POST ||--o{ POST_LIKE : "좋아요"
MEMBER ||--o{ POST_LIKE : "좋아요"

MEMBER ||--o{ REPORT : "신고(필수)"
MEMBER |o--o{ REPORT : "처리(선택)"
MEMBER ||--o{ ADMIN_LOG : "처리 로그"

MEMBER {
    NUMBER member_id PK
    VARCHAR2 login_id UK "로그인 아이디"
    VARCHAR2 password "해시값"
    VARCHAR2 nickname UK
    VARCHAR2 email UK
    VARCHAR2 role "USER/ADMIN"
    NUMBER fail_count
    CHAR is_locked
    DATE last_login_at
    DATE created_at
}

AUTHOR {
    NUMBER author_id PK
    VARCHAR2 author_name
    VARCHAR2 bio
}

AUTHOR_ACCOUNT {
    NUMBER member_id PK, FK
    NUMBER linked_author_id FK, UK "NULL 허용, 작가당 1계정"
    DATE verified_at
    VARCHAR2 self_intro
    DATE self_intro_updated_at
}

AUTHOR_ACCOUNT_REQUEST {
    NUMBER request_id PK
    NUMBER member_id FK
    NUMBER author_id FK "선택"
    VARCHAR2 introduction
    VARCHAR2 proof_url
    VARCHAR2 status
    NUMBER admin_id FK "선택"
    VARCHAR2 reject_reason
    DATE requested_at
    DATE processed_at
}

AUTHOR_REVIEW {
    NUMBER author_review_id PK
    NUMBER author_id FK
    NUMBER member_id FK
    VARCHAR2 title
    CLOB content
    CHAR is_spoiler
    VARCHAR2 status
    DATE created_at
    DATE updated_at
}

BOOK {
    NUMBER book_id PK
    NUMBER author_id FK "필수"
    VARCHAR2 isbn UK
    VARCHAR2 title
    VARCHAR2 genre
    VARCHAR2 publisher
    DATE published_date
    VARCHAR2 description
    VARCHAR2 image_url
    VARCHAR2 source_url
    VARCHAR2 status "PENDING/APPROVED/REJECTED"
}

BOOK_REQUEST {
    NUMBER request_id PK
    NUMBER member_id FK
    VARCHAR2 isbn UK
    VARCHAR2 title
    VARCHAR2 author_name "텍스트, FK 아님"
    VARCHAR2 genre
    VARCHAR2 publisher
    DATE published_date
    VARCHAR2 description
    VARCHAR2 image_url
    VARCHAR2 source_url
    VARCHAR2 status
    VARCHAR2 reject_reason
    NUMBER reviewed_by FK "선택"
    DATE requested_at
    DATE reviewed_at
}

RATING {
    NUMBER rating_id PK
    NUMBER book_id FK
    NUMBER member_id FK
    NUMBER score "1~5"
    VARCHAR2 comment_text
}

TIER_TEMPLATE {
    NUMBER template_id PK
    NUMBER member_id FK
    VARCHAR2 title
    VARCHAR2 description
    VARCHAR2 category "자유/장르/작가"
    VARCHAR2 status
    NUMBER admin_id FK "선택"
    VARCHAR2 reject_reason
    DATE requested_at
    DATE processed_at
}

TIER_TEMPLATE_ITEM {
    NUMBER template_item_id PK
    NUMBER template_id FK
    NUMBER book_id FK
    NUMBER sort_order
}

TIER_LIST {
    NUMBER tier_list_id PK
    NUMBER member_id FK
    NUMBER template_id FK
    VARCHAR2 title
    VARCHAR2 description
    DATE created_at
}

TIER_ITEM {
    NUMBER tier_item_id PK
    NUMBER tier_list_id FK
    NUMBER book_id FK
    VARCHAR2 tier_grade "S/A/B/C/D"
    NUMBER sort_order
}

IDEAL_TEMPLATE {
    NUMBER template_id PK
    NUMBER member_id FK
    VARCHAR2 title
    VARCHAR2 description
    VARCHAR2 category "자유/장르"
    VARCHAR2 status
    NUMBER admin_id FK "선택"
    VARCHAR2 reject_reason
    DATE processed_at
    DATE created_at
}

IDEAL_TEMPLATE_ITEM {
    NUMBER template_item_id PK
    NUMBER template_id FK
    NUMBER book_id FK
    NUMBER sort_order
}

IDEAL_RUN {
    NUMBER run_id PK
    NUMBER template_id FK
    NUMBER member_id FK
    NUMBER bracket_size "8 또는 16"
    NUMBER winner_book_id FK
    DATE created_at
}

IDEAL_MATCH {
    NUMBER match_id PK
    NUMBER run_id FK
    NUMBER round_size "2/4/8/16"
    NUMBER match_order
    NUMBER left_book_id FK
    NUMBER right_book_id FK
    NUMBER winner_book_id FK
}

POST {
    NUMBER post_id PK
    NUMBER member_id FK
    NUMBER tier_list_id FK, UK "선택, 1:1"
    NUMBER ideal_run_id FK, UK "선택, 1:1"
    VARCHAR2 category "FREE/REVIEW/RECOMMEND/TIER/WORLDCUP/AUTHOR/NOTICE"
    VARCHAR2 title
    CLOB content
    VARCHAR2 genre
    VARCHAR2 tag
    NUMBER view_count
    CHAR is_pinned
    VARCHAR2 status
    DATE created_at
    DATE updated_at
}

POST_COMMENT {
    NUMBER comment_id PK
    NUMBER post_id FK
    NUMBER member_id FK
    NUMBER parent_comment_id FK "자기참조, 선택"
    VARCHAR2 content
    VARCHAR2 status
    DATE created_at
    DATE updated_at
}

POST_LIKE {
    NUMBER post_like_id PK
    NUMBER post_id FK
    NUMBER member_id FK
    DATE created_at
}

REPORT {
    NUMBER report_id PK
    NUMBER reporter_id FK "필수"
    VARCHAR2 target_type "POST/COMMENT/AUTHOR_REVIEW, 폴리모픽"
    NUMBER target_id "FK 아님"
    VARCHAR2 reason_type
    VARCHAR2 reason_detail
    VARCHAR2 status
    NUMBER admin_id FK "선택"
    VARCHAR2 admin_memo
    DATE created_at
    DATE processed_at
}

ADMIN_LOG {
    NUMBER admin_log_id PK
    NUMBER admin_id FK "필수"
    VARCHAR2 target_type "폴리모픽"
    NUMBER target_id "FK 아님"
    VARCHAR2 action_type
    VARCHAR2 reason
    DATE created_at
}
```
