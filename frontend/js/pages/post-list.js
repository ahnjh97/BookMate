/* 1. 커뮤니티 게시글 목록 요소 */
const postListElement = document.querySelector("#post-list");
const postTableHead = document.querySelector("#post-table-head");
const postStatusElement = document.querySelector("#post-status");
const postResultMessageElement = document.querySelector("#post-result-message");
const postTotalCountElement = document.querySelector("#post-total-count");
const postSearchForm = document.querySelector("#post-search-form");
const postKeywordInput = document.querySelector("#post-keyword");
const postSortSelect = document.querySelector("#post-sort");
const categoryButtons = document.querySelectorAll("[data-category]");
const genreButtons = document.querySelectorAll("[data-genre]");

const categoryNames = {
    NOTICE: "공지",
    FREE: "자유",
    RECOMMEND: "추천",
    REVIEW: "리뷰",
    TIER: "티어리스트",
    WORLDCUP: "이상형월드컵"
};

/* 2. 현재 필터 상태 */
const filterState = {
    category: "",
    genre: "",
    keyword: "",
    sort: "latest"
};

let postRequestController = null;
let postRequestSequence = 0;

const postPagination = window.BookMateListPagination.create({
    root: document.querySelector("#post-pagination"),
    pageSize: 10,
    onRender: renderPosts,
    onPageChange: loadPosts
});

/* 3. 게시글 목록 불러오기 */
async function loadPosts(page = 1) {
    if (postRequestController) {
        postRequestController.abort();
    }

    postRequestController = new AbortController();
    const requestController = postRequestController;
    const requestSequence = ++postRequestSequence;
    const requestFilter = { ...filterState };

    postStatusElement.hidden = false;
    postStatusElement.textContent = "게시글을 불러오는 중입니다.";
    postListElement.replaceChildren();

    try {
        const params = new URLSearchParams({ page: String(page), size: "10" });
        if (requestFilter.category) params.set("category", requestFilter.category);
        if (requestFilter.genre) params.set("genre", requestFilter.genre);
        if (requestFilter.keyword) params.set("keyword", requestFilter.keyword);
        params.set("sort", requestFilter.sort);

        const response = await fetch(`/api/posts?${params}`, {
            signal: requestController.signal
        });
        const result = await response.json();

        if (requestSequence !== postRequestSequence) {
            return;
        }

        if (!response.ok || !result.success) {
            throw new Error(result.message || "게시글을 불러오지 못했습니다.");
        }

        const posts = Array.isArray(result.posts) ? result.posts : [];
        const pinnedPosts = Array.isArray(result.pinnedPosts) ? result.pinnedPosts : [];
        const visiblePosts = [...pinnedPosts, ...posts];
        const totalCount = Number(result.totalCount);
        const paginationTotalCount = Number(result.paginationTotalCount);
        renderTableHeader();
        if (Number.isFinite(totalCount)) {
            postPagination.setPage(
                visiblePosts,
                Number(result.page) || page,
                Number.isFinite(paginationTotalCount) ? paginationTotalCount : totalCount,
                Number(result.totalPages) || undefined
            );
            updateResultMessage(totalCount, requestFilter);
        } else {
            // 구버전 API 응답도 화면이 깨지지 않도록 전체 배열 방식으로 처리합니다.
            postPagination.setItems(posts);
            updateResultMessage(posts.length, requestFilter);
        }
    } catch (error) {
        if (error.name === "AbortError") {
            return;
        }

        console.error(error);
        postStatusElement.hidden = false;
        postStatusElement.textContent = error.message || "게시글을 불러오지 못했습니다.";
    }
}

/* 4. 전체 필터 적용 */
function applyFilters() {
    loadPosts(1);
}

/* 5. 표 제목 변경 */
function renderTableHeader() {
    postTableHead.innerHTML = `
        <tr>
            <th>카테고리</th>
            <th>제목</th>
            <th>장르</th>
            <th>작성자</th>
            <th>작성일</th>
            <th>댓글</th>
            <th>좋아요</th>
            <th>조회수</th>
        </tr>
    `;
}

/* 6. 게시글 목록 렌더링 */
function renderPosts(posts, startIndex, totalCount) {
    postListElement.replaceChildren();

    if (!Array.isArray(posts) || posts.length === 0) {
        postStatusElement.hidden = true;
        postStatusElement.textContent = "";
        return;
    }

    const fragment = document.createDocumentFragment();

    posts.forEach((post, index) => {
        const row = document.createElement("tr");

        row.classList.toggle("is-pinned", post.isPinned === "Y" || post.isPinned === true);

        renderRealtimeRow(row, post, startIndex + index);

        fragment.append(row);
    });

    postListElement.append(fragment);
    postStatusElement.textContent = "";
    postStatusElement.hidden = true;
}

/* 7. 실시간 게시글 행 */
function renderRealtimeRow(row, post, index) {
    const categoryCell = createCell(
        "post-category",
        categoryNames[post.category] || post.category || "-"
    );

    const titleCell = createTitleCell(post);
    const genreCell = createCell("post-genre", post.genre || "-");
    const writerCell = createWriterCell(
        post.memberNickname || "알 수 없음",
        post.memberId
    );
    const dateCell = createCell("post-date", formatDate(post.createdAt));
    const commentCell = createCell("post-comments-count", post.commentCount ?? 0);
    const likeCell = createCell("post-likes", post.likeCount ?? 0);
    const viewCell = createCell("post-views", post.viewCount ?? 0);

    row.append(
        categoryCell,
        titleCell,
        genreCell,
        writerCell,
        dateCell,
        commentCell,
        likeCell,
        viewCell
    );
}

/* 9. 일반 셀 생성 */
function createCell(className, value) {
    const cell = document.createElement("td");

    cell.className = className;
    cell.textContent = value;

    return cell;
}

function createWriterCell(value, memberId) {
    const cell = document.createElement("td");
    const text = document.createElement("span");

    cell.className = "post-writer";
    text.className = "post-writer-text";
    window.BookMateBookshelfVisit.render(text, value, memberId);
    cell.append(text);

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

/* 13. 검색 및 필터 결과 안내 */
function updateResultMessage(totalCount, appliedFilter = filterState) {
    postTotalCountElement.textContent = `총 ${totalCount}개`;

    if (appliedFilter.keyword) {
        const keyword = document.createElement("strong");
        const description = document.createElement("span");

        keyword.textContent = `'${appliedFilter.keyword}'`;
        description.textContent = "에 대한 검색결과입니다.";
        postResultMessageElement.classList.add("list-search-result-heading");
        postResultMessageElement.replaceChildren(keyword, description);
        return;
    }

    const messages = [];

    if (appliedFilter.category) {
        messages.push(categoryNames[appliedFilter.category] || appliedFilter.category);
    }

    if (appliedFilter.genre) {
        messages.push(appliedFilter.genre);
    }

    if (messages.length === 0) {
        postResultMessageElement.classList.remove("list-search-result-heading");
        postResultMessageElement.textContent = "";
        return;
    }

    postResultMessageElement.classList.remove("list-search-result-heading");
    postResultMessageElement.textContent = `${messages.join(" | ")} | ${totalCount}개`;
}

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

postSortSelect.addEventListener("change", () => {
    filterState.sort = postSortSelect.value;
    loadPosts(1);
});

document.querySelector("#community-reset-button").addEventListener("click", () => {
    window.location.assign("/pages/post/list.html");
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
