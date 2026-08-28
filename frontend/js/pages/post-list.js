/* 1. 커뮤니티 게시글 목록 요소 */
const postListElement = document.querySelector("#post-list");
const postTableHead = document.querySelector("#post-table-head");
const postStatusElement = document.querySelector("#post-status");
const postResultMessageElement = document.querySelector("#post-result-message");
const postSearchForm = document.querySelector("#post-search-form");
const postKeywordInput = document.querySelector("#post-keyword");
const communityFilter = document.querySelector("#community-filter");
const viewButtons = document.querySelectorAll("[data-view]");
const periodFilter = document.querySelector("#popular-period-filter");
const periodButtons = document.querySelectorAll("[data-period]");
const categoryButtons = document.querySelectorAll("[data-category]");
const genreButtons = document.querySelectorAll("[data-genre]");

const categoryNames = {
    NOTICE: "공지",
    FREE: "자유",
    RECOMMEND: "추천",
    REVIEW: "리뷰",
    TIER: "티어리스트",
    AUTHOR: "작가"
};

/* 2. 현재 필터 상태 */
const filterState = {
    view: "realtime",
    period: "today",
    category: "",
    genre: "",
    keyword: ""
};

let allPosts = [];

/* 3. 게시글 목록 불러오기 */
async function loadPosts() {
    postStatusElement.textContent = "게시글을 불러오는 중입니다.";
    postListElement.replaceChildren();

    try {
        const response = await fetch("/api/posts");
        const result = await response.json();

        if (!response.ok || !result.success) {
            throw new Error(result.message || "게시글을 불러오지 못했습니다.");
        }

        allPosts = Array.isArray(result.posts) ? result.posts : [];

        applyFilters();
    } catch (error) {
        console.error(error);
        postStatusElement.textContent = error.message || "게시글을 불러오지 못했습니다.";
    }
}

/* 4. 전체 필터 적용 */
function applyFilters() {
    let posts = [...allPosts];

    if (filterState.view === "realtime") {
        if (filterState.category) {
            posts = posts.filter(post => post.category === filterState.category);
        }

        if (filterState.genre) {
            posts = posts.filter(post => post.genre === filterState.genre);
        }
    }

    if (filterState.keyword) {
        const keyword = filterState.keyword.toLowerCase();

        posts = posts.filter(post => {
            const title = String(post.title || "").toLowerCase();
            const writer = String(post.memberNickname || "").toLowerCase();

            return title.includes(keyword) || writer.includes(keyword);
        });
    }

    if (filterState.view === "popular") {
        posts = filterPostsByPeriod(posts, filterState.period);

        posts.sort((a, b) => {
            const likeDifference = (b.likeCount ?? 0) - (a.likeCount ?? 0);

            if (likeDifference !== 0) {
                return likeDifference;
            }

            const viewDifference = (b.viewCount ?? 0) - (a.viewCount ?? 0);

            if (viewDifference !== 0) {
                return viewDifference;
            }

            return getTime(b.createdAt) - getTime(a.createdAt);
        });
    } else {
        posts.sort((a, b) => getTime(b.createdAt) - getTime(a.createdAt));
    }

    renderTableHeader();
    renderPosts(posts);
    updateResultMessage(posts);
}

/* 5. 표 제목 변경 */
function renderTableHeader() {
    if (filterState.view === "popular") {
        postTableHead.innerHTML = `
            <tr>
                <th>순위</th>
                <th>장르</th>
                <th>제목</th>
                <th>작성자</th>
                <th>작성일</th>
                <th>조회수</th>
                <th>좋아요</th>
            </tr>
        `;

        return;
    }

    postTableHead.innerHTML = `
        <tr>
            <th>순위</th>
            <th>카테고리</th>
            <th>제목</th>
            <th>장르</th>
            <th>작성자</th>
            <th>작성일</th>
            <th>조회수</th>
        </tr>
    `;
}

/* 6. 게시글 목록 렌더링 */
function renderPosts(posts) {
    postListElement.replaceChildren();

    if (!Array.isArray(posts) || posts.length === 0) {
        postStatusElement.textContent = "조건에 맞는 게시글이 없습니다.";
        return;
    }

    const fragment = document.createDocumentFragment();

    posts.forEach((post, index) => {
        const row = document.createElement("tr");

        if (filterState.view === "popular") {
            renderPopularRow(row, post, index);
        } else {
            renderRealtimeRow(row, post, index);
        }

        fragment.append(row);
    });

    postListElement.append(fragment);
    postStatusElement.textContent = `게시글 ${posts.length}개를 표시하고 있습니다.`;
}

/* 7. 실시간 게시글 행 */
function renderRealtimeRow(row, post, index) {
    const numberCell = createCell("post-number", index + 1);
    const categoryCell = createCell(
        "post-category",
        categoryNames[post.category] || post.category || "-"
    );

    const titleCell = createTitleCell(post);
    const genreCell = createCell("post-genre", post.genre || "-");
    const writerCell = createCell("post-writer", post.memberNickname || "알 수 없음");
    const dateCell = createCell("post-date", formatDate(post.createdAt));
    const viewCell = createCell("post-views", post.viewCount ?? 0);

    row.append(
        numberCell,
        categoryCell,
        titleCell,
        genreCell,
        writerCell,
        dateCell,
        viewCell
    );
}

/* 8. 인기글 행 */
function renderPopularRow(row, post, index) {
    const numberCell = createCell("post-number", index + 1);
    const genreCell = createCell("post-genre", post.genre || "-");
    const titleCell = createTitleCell(post);
    const writerCell = createCell("post-writer", post.memberNickname || "알 수 없음");
    const dateCell = createCell("post-date", formatDate(post.createdAt));
    const viewCell = createCell("post-views", post.viewCount ?? 0);
    const likeCell = createCell("post-likes", post.likeCount ?? 0);

    row.append(
        numberCell,
        genreCell,
        titleCell,
        writerCell,
        dateCell,
        viewCell,
        likeCell
    );
}

/* 9. 일반 셀 생성 */
function createCell(className, value) {
    const cell = document.createElement("td");

    cell.className = className;
    cell.textContent = value;

    return cell;
}

/* 10. 게시글 제목 셀 생성 */
function createTitleCell(post) {
    const cell = document.createElement("td");
    const link = document.createElement("a");

    cell.className = "post-title";

    link.href = `/pages/post/detail.html?postId=${post.postId}`;
    link.textContent = post.title || "제목 없음";

    cell.append(link);

    return cell;
}

/* 11. 인기글 기간 필터 */
function filterPostsByPeriod(posts, period) {
    const now = new Date();

    return posts.filter(post => {
        if (!post.createdAt) {
            return false;
        }

        const createdAt = new Date(post.createdAt);

        if (Number.isNaN(createdAt.getTime())) {
            return false;
        }

        if (period === "today") {
            return createdAt.getFullYear() === now.getFullYear()
                && createdAt.getMonth() === now.getMonth()
                && createdAt.getDate() === now.getDate();
        }

        if (period === "week") {
            return createdAt >= getStartOfWeek(now);
        }

        if (period === "month") {
            return createdAt.getFullYear() === now.getFullYear()
                && createdAt.getMonth() === now.getMonth();
        }

        return true;
    });
}

/* 12. 이번주의 시작일 계산 */
function getStartOfWeek(date) {
    const result = new Date(date);

    result.setHours(0, 0, 0, 0);

    const day = result.getDay();
    const diff = day === 0 ? -6 : 1 - day;

    result.setDate(result.getDate() + diff);

    return result;
}

/* 13. 검색 및 필터 결과 안내 */
function updateResultMessage(posts) {
    const messages = [];

    if (filterState.view === "popular") {
        const periodNames = {
            today: "오늘",
            week: "이번주",
            month: "이번달"
        };

        messages.push(`${periodNames[filterState.period]} 인기글`);
    } else {
        messages.push("실시간 게시글");

        if (filterState.category) {
            messages.push(categoryNames[filterState.category] || filterState.category);
        }

        if (filterState.genre) {
            messages.push(filterState.genre);
        }
    }

    if (filterState.keyword) {
        messages.push(`"${filterState.keyword}" 검색`);
    }

    postResultMessageElement.textContent = `${messages.join(" · ")} — ${posts.length}개`;
}

/* 14. 실시간 및 인기글 전환 */
viewButtons.forEach(button => {
    button.addEventListener("click", () => {
        filterState.view = button.dataset.view;

        setActiveButton(viewButtons, button);

        const isPopular = filterState.view === "popular";

        communityFilter.hidden = isPopular;
        periodFilter.hidden = !isPopular;

        applyFilters();
    });
});

/* 15. 인기글 기간 변경 */
periodButtons.forEach(button => {
    button.addEventListener("click", () => {
        filterState.period = button.dataset.period;

        setActiveButton(periodButtons, button);
        applyFilters();
    });
});

/* 16. 게시글 카테고리 변경 */
categoryButtons.forEach(button => {
    button.addEventListener("click", () => {
        filterState.category = button.dataset.category;

        setActiveButton(categoryButtons, button);
        applyFilters();
    });
});

/* 17. 게시글 장르 변경 */
genreButtons.forEach(button => {
    button.addEventListener("click", () => {
        filterState.genre = button.dataset.genre;

        setActiveButton(genreButtons, button);
        applyFilters();
    });
});

/* 18. 게시글 검색 */
postSearchForm.addEventListener("submit", event => {
    event.preventDefault();

    filterState.keyword = postKeywordInput.value.trim();

    applyFilters();
});

/* 19. 검색어 삭제 시 검색 조건 해제 */
postKeywordInput.addEventListener("input", () => {
    if (postKeywordInput.value.trim() !== "") {
        return;
    }

    filterState.keyword = "";

    applyFilters();
});

/* 20. 선택 버튼 활성화 */
function setActiveButton(buttons, selectedButton) {
    buttons.forEach(button => {
        button.classList.toggle("is-active", button === selectedButton);
    });
}

/* 21. 날짜 변환 */
function formatDate(value) {
    return value ? String(value).substring(0, 10) : "-";
}

/* 22. 날짜 정렬용 시간값 변환 */
function getTime(value) {
    const time = new Date(value).getTime();

    return Number.isNaN(time) ? 0 : time;
}

/* 23. 게시글 목록 초기 실행 */
loadPosts();
