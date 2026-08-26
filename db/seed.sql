SET DEFINE OFF;
-- BookMate 개발용 샘플 데이터
-- 소설 중심 구성: 판타지, SF, 스릴러, 추리, 디스토피아 + 비문학

INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, 'J. K. 롤링', '해리 포터 시리즈로 세계적인 사랑을 받은 영국 판타지 작가');
/*END*/
INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, '스티븐 굴드', '순간이동 능력과 인간의 선택을 다룬 점퍼 시리즈의 SF 작가');
/*END*/
INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, 'J. R. R. 톨킨', '중간계 세계관을 창조한 현대 판타지 문학의 거장');
/*END*/
INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, '조지 R. R. 마틴', '복잡한 인물과 정치적 갈등을 그리는 얼음과 불의 노래 시리즈 작가');
/*END*/
INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, '프랭크 허버트', '정치와 종교, 생태를 결합한 듄 시리즈의 SF 작가');
/*END*/
INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, '앤디 위어', '과학적 문제 해결을 흥미로운 이야기로 풀어내는 SF 작가');
/*END*/
INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, '조지 오웰', '전체주의와 사회 통제를 날카롭게 비판한 영국 작가');
/*END*/
INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, '애거사 크리스티', '정교한 트릭과 추리로 사랑받는 미스터리 소설가');
/*END*/
INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, '알렉스 마이클리디스', '심리와 반전을 결합한 스릴러 작가');
/*END*/
INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, '유발 하라리', '인류의 역사와 미래를 거시적으로 설명하는 역사학자');
/*END*/
INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, '칼 세이건', '우주와 과학을 대중에게 친근하게 전달한 천문학자');
/*END*/
INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, '제임스 클리어', '습관 형성과 행동 변화 방법을 연구하고 알리는 작가');
/*END*/
INSERT INTO AUTHOR (author_id, author_name, bio)
VALUES (SEQ_AUTHOR.NEXTVAL, '로버트 C. 마틴', '소프트웨어 장인 정신과 클린 코드 원칙을 알린 개발자');
/*END*/

-- 판타지: 해리 포터
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '해리 포터와 마법사의 돌', '판타지', 'Scholastic', DATE '1998-09-01', '자신이 마법사임을 알게 된 해리가 호그와트에서 처음 맞는 모험', '/bookmate/assets/images/books/9780439708180.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = 'J. K. 롤링';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '해리 포터와 비밀의 방', '판타지', 'Scholastic', DATE '1999-06-02', '호그와트에 숨겨진 비밀의 방과 어둠의 존재를 추적하는 두 번째 모험', '/bookmate/assets/images/books/9780439064873.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = 'J. K. 롤링';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '해리 포터와 아즈카반의 죄수', '판타지', 'Scholastic', DATE '1999-09-08', '탈옥수 시리우스 블랙의 진실과 해리의 과거가 드러나는 이야기', '/bookmate/assets/images/books/9780439136365.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = 'J. K. 롤링';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '해리 포터와 불의 잔', '판타지', 'Scholastic', DATE '2000-07-08', '트라이위저드 시합에 참가하게 된 해리와 어둠의 마법사의 귀환', '/bookmate/assets/images/books/9780439139601.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = 'J. K. 롤링';
/*END*/

-- SF: 점퍼, 듄, 프로젝트 헤일메리
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '점퍼', 'SF', 'Tor Books', DATE '2008-01-01', '원하는 장소로 순간이동할 수 있게 된 청년 데이비의 자유와 선택', '/bookmate/assets/images/books/9780765357694.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '스티븐 굴드';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '듄', 'SF', 'Ace', DATE '1990-09-01', '사막 행성 아라키스를 둘러싼 권력과 예언, 생존의 대서사시', '/bookmate/assets/images/books/9780441172719.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '프랭크 허버트';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '프로젝트 헤일메리', 'SF', 'Ballantine Books', DATE '2021-05-04', '기억을 잃은 과학자가 우주에서 인류를 구할 방법을 찾아가는 생존기', '/bookmate/assets/images/books/9780593135204.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '앤디 위어';
/*END*/

-- 판타지: 중간계
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '호빗', '판타지', 'Mariner Books', DATE '2012-09-18', '호빗 빌보가 난쟁이들과 함께 외로운 산으로 떠나는 모험', '/bookmate/assets/images/books/9780547928227.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = 'J. R. R. 톨킨';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '반지의 제왕: 반지 원정대', '판타지', 'Mariner Books', DATE '2012-09-18', '절대반지를 파괴하기 위해 중간계를 가로지르는 원정대의 시작', '/bookmate/assets/images/books/9780547928210.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = 'J. R. R. 톨킨';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '반지의 제왕: 두 개의 탑', '판타지', 'Mariner Books', DATE '2012-09-18', '갈라진 원정대와 전쟁의 그림자 속에서 이어지는 중간계의 여정', '/bookmate/assets/images/books/9780547928203.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = 'J. R. R. 톨킨';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '반지의 제왕: 왕의 귀환', '판타지', 'Mariner Books', DATE '2012-09-18', '곤도르의 최후 전투와 절대반지를 둘러싼 여정의 결말', '/bookmate/assets/images/books/9780547928197.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = 'J. R. R. 톨킨';
/*END*/

-- 판타지: 얼음과 불의 노래
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '왕좌의 게임', '판타지', 'Bantam', DATE '2011-03-22', '웨스테로스의 철왕좌를 둘러싼 가문들의 욕망과 배신', '/bookmate/assets/images/books/9780553593716.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '조지 R. R. 마틴';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '왕들의 전쟁', '판타지', 'Bantam', DATE '1999-09-05', '여러 왕이 권력을 주장하며 대륙 전체가 전쟁에 휩싸이는 이야기', '/bookmate/assets/images/books/9780553579901.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '조지 R. R. 마틴';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '성검의 폭풍', '판타지', 'Bantam', DATE '2003-03-04', '전쟁과 음모가 절정으로 치달으며 운명이 크게 뒤바뀌는 세 번째 이야기', '/bookmate/assets/images/books/9780553573428.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '조지 R. R. 마틴';
/*END*/

-- 다른 소설 장르
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '1984', '디스토피아', 'Signet Classic', DATE '1950-07-01', '빅 브라더가 모든 생각과 행동을 감시하는 전체주의 사회의 악몽', '/bookmate/assets/images/books/9780451524935.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '조지 오웰';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '오리엔트 특급 살인', '추리', 'William Morrow', DATE '2017-01-24', '눈 속에 멈춘 열차에서 벌어진 살인사건을 푸아로가 추적하는 고전 미스터리', '/bookmate/assets/images/books/9780062693662.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '애거사 크리스티';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '사일런트 페이션트', '스릴러', 'Celadon Books', DATE '2019-02-05', '남편을 살해한 뒤 침묵한 화가와 진실을 밝히려는 심리치료사의 이야기', '/bookmate/assets/images/books/9781250301697.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '알렉스 마이클리디스';
/*END*/

-- 비문학: 인문, 과학, 자기계발, IT
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '사피엔스', '인문', 'Harper', DATE '2015-02-10', '인지혁명부터 현대까지 호모 사피엔스의 역사를 거시적으로 살펴보는 책', '/bookmate/assets/images/books/9780062316097.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '유발 하라리';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '코스모스', '과학', 'Ballantine Books', DATE '2013-12-10', '우주의 기원과 생명, 인류의 탐구 정신을 아름답게 풀어낸 과학 교양서', '/bookmate/assets/images/books/9780345539434.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '칼 세이건';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '아주 작은 습관의 힘', '자기계발', 'Avery', DATE '2018-10-16', '작은 행동을 반복해 좋은 습관을 만들고 삶을 변화시키는 실천법', '/bookmate/assets/images/books/9780735211292.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '제임스 클리어';
/*END*/
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
SELECT SEQ_BOOK.NEXTVAL, author_id, '클린 코드', 'IT', 'Prentice Hall', DATE '2008-08-01', '읽기 쉽고 유지보수하기 좋은 코드를 작성하는 원칙과 사례', '/bookmate/assets/images/books/9780132350884.jpg', 'APPROVED' FROM AUTHOR WHERE author_name = '로버트 C. 마틴';
/*END*/

-- 시리즈 및 장르 확장용 저자
INSERT INTO AUTHOR (author_id, author_name, bio)
WITH AUTHOR_SEED (author_name, bio) AS (
    SELECT '수전 콜린스', '헝거 게임 시리즈를 쓴 미국 소설가' FROM DUAL
    UNION ALL SELECT '릭 라이어던', '신화를 현대적으로 재해석한 판타지 작가' FROM DUAL
    UNION ALL SELECT 'C. S. 루이스', '나니아 연대기를 쓴 영국 판타지 작가' FROM DUAL
    UNION ALL SELECT '아서 코난 도일', '셜록 홈즈를 창조한 추리 소설가' FROM DUAL
    UNION ALL SELECT '아이작 아시모프', '로봇과 은하 문명을 탐구한 과학소설가' FROM DUAL
    UNION ALL SELECT '올더스 헉슬리', '과학기술과 통제사회를 비판한 영국 작가' FROM DUAL
    UNION ALL SELECT '레이 브래드버리', '상상력과 사회비판을 결합한 미국 작가' FROM DUAL
    UNION ALL SELECT '댄 브라운', '역사와 암호를 소재로 한 미스터리 작가' FROM DUAL
    UNION ALL SELECT '길리언 플린', '관계의 어두운 면을 파고드는 스릴러 작가' FROM DUAL
)
SELECT SEQ_AUTHOR.NEXTVAL, author_name, bio FROM AUTHOR_SEED;
/*END*/

-- 추가 도서 35권: 전체 시드 56권
INSERT INTO BOOK (book_id, author_id, title, genre, publisher, published_date, description, image_url, status)
WITH BOOK_SEED (author_name, title, genre, publisher, published_date, description, image_url) AS (
    SELECT 'J. K. 롤링', '해리 포터와 불사조 기사단', '판타지', 'Scholastic', DATE '2003-06-21', '볼드모트의 귀환을 믿지 않는 마법 세계와 맞서는 해리', '/bookmate/assets/images/books/9780439358071.jpg' FROM DUAL
    UNION ALL SELECT 'J. K. 롤링', '해리 포터와 혼혈 왕자', '판타지', 'Scholastic', DATE '2005-07-16', '볼드모트의 과거와 호크룩스의 비밀을 추적하는 여정', '/bookmate/assets/images/books/9780439785969.jpg' FROM DUAL
    UNION ALL SELECT 'J. K. 롤링', '해리 포터와 죽음의 성물', '판타지', 'Scholastic', DATE '2007-07-21', '호크룩스를 파괴하고 최후의 전투로 향하는 시리즈의 결말', '/bookmate/assets/images/books/9780545010221.jpg' FROM DUAL
    UNION ALL SELECT '조지 R. R. 마틴', '까마귀의 향연', '판타지', 'Bantam', DATE '2005-11-08', '전쟁 이후 권력의 공백을 차지하려는 새로운 세력들의 이야기', '/bookmate/assets/images/books/9780553582024.jpg' FROM DUAL
    UNION ALL SELECT '조지 R. R. 마틴', '드래곤과의 춤', '판타지', 'Bantam', DATE '2013-10-29', '장벽과 대너리스의 도시에서 위기가 동시에 깊어지는 이야기', '/bookmate/assets/images/books/9780553582017.jpg' FROM DUAL
    UNION ALL SELECT '프랭크 허버트', '듄의 메시아', 'SF', 'Ace', DATE '2019-10-01', '황제가 된 폴 아트레이데스를 무너뜨리려는 음모', '/bookmate/assets/images/books/9780593098233.jpg' FROM DUAL
    UNION ALL SELECT '프랭크 허버트', '듄의 아이들', 'SF', 'Ace', DATE '2020-06-30', '폴의 쌍둥이 자녀들이 제국의 미래를 선택하는 이야기', '/bookmate/assets/images/books/9780593098240.jpg' FROM DUAL
    UNION ALL SELECT '프랭크 허버트', '듄의 신황제', 'SF', 'Ace', DATE '2020-06-30', '수천 년 동안 인류의 황금의 길을 준비한 레토 2세의 통치', '/bookmate/assets/images/books/9780593098257.jpg' FROM DUAL
    UNION ALL SELECT '프랭크 허버트', '듄의 이단자들', 'SF', 'Ace', DATE '2020-06-30', '신황제 이후 흩어진 인류와 새로운 세력의 충돌', '/bookmate/assets/images/books/9780593098264.jpg' FROM DUAL
    UNION ALL SELECT '프랭크 허버트', '듄의 신전', 'SF', 'Ace', DATE '2020-08-25', '베네 게세리트가 생존을 위해 마지막 선택을 준비하는 이야기', '/bookmate/assets/images/books/9780593098271.jpg' FROM DUAL
    UNION ALL SELECT '수전 콜린스', '헝거 게임', '디스토피아', 'Scholastic', DATE '2008-09-14', '독재국가의 생존 경기에서 살아남아야 하는 캣니스의 이야기', '/bookmate/assets/images/books/9780439023481.jpg' FROM DUAL
    UNION ALL SELECT '수전 콜린스', '캣칭 파이어', '디스토피아', 'Scholastic', DATE '2009-09-01', '승리 이후 혁명의 상징이 된 캣니스가 다시 경기장에 서는 이야기', '/bookmate/assets/images/books/9780439023498.jpg' FROM DUAL
    UNION ALL SELECT '수전 콜린스', '모킹제이', '디스토피아', 'Scholastic', DATE '2010-08-24', '판엠의 혁명과 전쟁 속에서 선전의 상징이 된 캣니스', '/bookmate/assets/images/books/9780439023511.jpg' FROM DUAL
    UNION ALL SELECT '릭 라이어던', '퍼시 잭슨과 번개 도둑', '판타지', 'Disney Hyperion', DATE '2006-04-01', '포세이돈의 아들 퍼시가 제우스의 번개를 찾아 떠나는 모험', '/bookmate/assets/images/books/9780786838653.jpg' FROM DUAL
    UNION ALL SELECT '릭 라이어던', '퍼시 잭슨과 괴물의 바다', '판타지', 'Disney Hyperion', DATE '2007-04-01', '혼혈 캠프를 구하기 위해 황금 양피를 찾아가는 항해', '/bookmate/assets/images/books/9781423103349.jpg' FROM DUAL
    UNION ALL SELECT '릭 라이어던', '퍼시 잭슨과 티탄의 저주', '판타지', 'Disney Hyperion', DATE '2008-04-01', '사라진 여신과 친구를 구하기 위해 티탄의 위협에 맞서는 모험', '/bookmate/assets/images/books/9781423101475.jpg' FROM DUAL
    UNION ALL SELECT '릭 라이어던', '퍼시 잭슨과 미궁의 전투', '판타지', 'Disney Hyperion', DATE '2009-04-07', '캠프로 이어지는 미궁을 차지하려는 군대와 벌이는 전투', '/bookmate/assets/images/books/9781423101499.jpg' FROM DUAL
    UNION ALL SELECT '릭 라이어던', '퍼시 잭슨과 최후의 올림포스 신', '판타지', 'Disney Hyperion', DATE '2011-01-25', '맨해튼에서 펼쳐지는 올림포스와 티탄의 마지막 전쟁', '/bookmate/assets/images/books/9781423101505.jpg' FROM DUAL
    UNION ALL SELECT 'C. S. 루이스', '나니아 연대기: 사자와 마녀와 옷장', '판타지', 'HarperCollins', DATE '2002-03-05', '옷장 너머 나니아에서 네 남매가 하얀 마녀에 맞서는 모험', '/bookmate/assets/images/books/9780064471046.jpg' FROM DUAL
    UNION ALL SELECT 'C. S. 루이스', '나니아 연대기: 캐스피언 왕자', '판타지', 'HarperCollins', DATE '2002-03-05', '빼앗긴 왕좌를 되찾으려는 캐스피언과 네 남매의 전투', '/bookmate/assets/images/books/9780064471053.jpg' FROM DUAL
    UNION ALL SELECT 'C. S. 루이스', '나니아 연대기: 새벽 출정호의 항해', '판타지', 'HarperCollins', DATE '2002-03-05', '사라진 영주들을 찾아 나니아의 동쪽 바다로 떠나는 항해', '/bookmate/assets/images/books/9780064471077.jpg' FROM DUAL
    UNION ALL SELECT 'C. S. 루이스', '나니아 연대기: 은의자', '판타지', 'HarperCollins', DATE '2002-03-05', '실종된 왕자를 찾기 위해 북쪽 황무지로 향하는 탐험', '/bookmate/assets/images/books/9780064471091.jpg' FROM DUAL
    UNION ALL SELECT 'C. S. 루이스', '나니아 연대기: 말과 소년', '판타지', 'HarperCollins', DATE '2002-03-05', '자유를 찾아 나니아로 달아나는 소년과 말의 여정', '/bookmate/assets/images/books/9780064471060.jpg' FROM DUAL
    UNION ALL SELECT 'C. S. 루이스', '나니아 연대기: 마법사의 조카', '판타지', 'HarperCollins', DATE '2002-03-05', '나니아가 탄생하고 옷장이 만들어지게 된 최초의 이야기', '/bookmate/assets/images/books/9780064471107.jpg' FROM DUAL
    UNION ALL SELECT 'C. S. 루이스', '나니아 연대기: 마지막 전투', '판타지', 'HarperCollins', DATE '2002-03-05', '거짓 아슬란의 등장과 나니아 세계의 마지막 전투', '/bookmate/assets/images/books/9780064471084.jpg' FROM DUAL
    UNION ALL SELECT '아서 코난 도일', '주홍색 연구', '추리', 'Penguin Classics', DATE '2001-07-05', '셜록 홈즈와 왓슨이 처음 만나 의문의 살인사건을 해결하는 이야기', '/bookmate/assets/images/books/9780140439083.jpg' FROM DUAL
    UNION ALL SELECT '아서 코난 도일', '네 사람의 서명', '추리', 'Penguin Classics', DATE '2001-07-05', '사라진 보물과 비밀스러운 약속을 추적하는 홈즈의 사건', '/bookmate/assets/images/books/9780140439076.jpg' FROM DUAL
    UNION ALL SELECT '아서 코난 도일', '셜록 홈즈의 모험', '추리', 'Penguin Classics', DATE '2009-08-27', '보헤미아 스캔들을 비롯한 셜록 홈즈의 대표 단편 모음', '/bookmate/assets/images/books/9780141034355.jpg' FROM DUAL
    UNION ALL SELECT '아서 코난 도일', '바스커빌가의 개', '추리', 'Penguin Classics', DATE '2007-01-25', '황야의 저택을 둘러싼 전설적인 사냥개의 진실을 밝히는 사건', '/bookmate/assets/images/books/9780141032430.jpg' FROM DUAL
    UNION ALL SELECT '아이작 아시모프', '파운데이션', 'SF', 'Spectra', DATE '1991-10-01', '은하제국의 몰락을 예측하고 문명을 보존하려는 거대한 계획', '/bookmate/assets/images/books/9780553293357.jpg' FROM DUAL
    UNION ALL SELECT '올더스 헉슬리', '멋진 신세계', '디스토피아', 'Harper Perennial', DATE '2006-10-17', '쾌락과 조건화로 통제되는 미래사회를 그린 디스토피아', '/bookmate/assets/images/books/9780060850524.jpg' FROM DUAL
    UNION ALL SELECT '레이 브래드버리', '화씨 451', '디스토피아', 'Simon & Schuster', DATE '2012-01-10', '책이 금지된 사회에서 소방관이 질문을 시작하는 이야기', '/bookmate/assets/images/books/9781451673319.jpg' FROM DUAL
    UNION ALL SELECT '댄 브라운', '다빈치 코드', '추리', 'Anchor', DATE '2009-03-31', '루브르의 살인사건에서 시작된 암호와 역사적 비밀의 추적', '/bookmate/assets/images/books/9780307474278.jpg' FROM DUAL
    UNION ALL SELECT '길리언 플린', '나를 찾아줘', '스릴러', 'Crown', DATE '2012-06-05', '아내의 실종과 완벽해 보였던 결혼의 거짓을 파헤치는 심리 스릴러', '/bookmate/assets/images/books/9780307588371.jpg' FROM DUAL
    UNION ALL SELECT '앤디 위어', '마션', 'SF', 'Broadway Books', DATE '2014-10-28', '화성에 홀로 남은 우주비행사가 과학으로 생존하는 이야기', '/bookmate/assets/images/books/9780553418026.jpg' FROM DUAL
)
SELECT SEQ_BOOK.NEXTVAL, A.author_id, S.title, S.genre, S.publisher,
       S.published_date, S.description, S.image_url, 'APPROVED'
  FROM BOOK_SEED S
  JOIN AUTHOR A ON A.author_name = S.author_name;
/*END*/

COMMIT;
/*END*/
