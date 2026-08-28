const detailElement = document.querySelector("#post-detail");
const statusElement = document.querySelector("#post-detail-status");
const titleElement = document.querySelector("#post-title");
const categoryElement = document.querySelector("#post-category");
const genreElement = document.querySelector("#post-genre");
const writerElement = document.querySelector("#post-writer");
const viewCountElement = document.querySelector("#post-view-count");
const createdAtElement = document.querySelector("#post-created-at");
const contentElement = document.querySelector("#post-content");
const tierListElement = document.querySelector("#post-tier-list");
const tierBoardElement = document.querySelector("#post-tier-board");
const likeCountElement = document.querySelector("#post-like-count");
const writerActions = document.querySelector("#writer-actions");
const memberActions = document.querySelector("#member-actions");
const editLink = document.querySelector("#post-edit-link");
const hideButton = document.querySelector("#post-hide-button");
const deleteButton = document.querySelector("#post-delete-button");
const likeButton = document.querySelector("#post-like-button");
const reportButton = document.querySelector("#post-report-button");

const categoryNames = {
    NOTICE: "공지",
    FREE: "자유",
    RECOMMEND: "추천",
    REVIEW: "리뷰",
    TIER: "티어리스트",
    AUTHOR: "작가"
};

let currentPost = null;
let currentMember = null;

/* 1. 게시글 상세 조회 */
async function loadPostDetail() {
    const params = new URLSearchParams(location.search);
    const postId = params.get("postId");

    if (!postId || !/^\d+$/.test(postId)) {
        showError("올바른 게시글 번호가 아닙니다.");
        return;
    }

    try {
        const [postResponse, sessionResponse] = await Promise.all([
            fetch(`/api/posts/detail?postId=${encodeURIComponent(postId)}`),
            fetch("/api/auth/session", { cache: "no-store" })
        ]);

        const postResult = await postResponse.json();
        const sessionResult = sessionResponse.ok
            ? await sessionResponse.json()
            : { loggedIn: false };

        if (!postResponse.ok || !postResult.success) {
            throw new Error(
                postResult.message ||
                "게시글을 불러오지 못했습니다."
            );
        }

        currentPost = postResult.post;
        currentMember = sessionResult;

        renderPost(currentPost);
        renderTierList(postResult.tierList);
        renderActions(currentPost, currentMember, postResult.tierList);
    } catch (error) {
        console.error(error);
        showError(
            error.message ||
            "게시글을 불러오지 못했습니다."
        );
    }
}

function renderTierList(tierList) {
    tierBoardElement.replaceChildren();
    tierListElement.hidden = !tierList;
    detailElement.classList.toggle("has-tier-list", Boolean(tierList));
    if (!tierList) return;
    ["S", "A", "B", "C", "D"].forEach(grade => {
        const row = document.createElement("div");
        row.className = "post-tier-row";
        const label = document.createElement("strong");
        label.textContent = grade;
        const books = document.createElement("div");
        books.className = "post-tier-books";
        (tierList.items || []).filter(item => item.grade === grade).forEach(item => {
            const link = document.createElement("a");
            link.className = "post-tier-book";
            link.href = `/pages/book/detail.html?id=${encodeURIComponent(item.bookId)}`;
            link.title = `${item.title} - ${item.authorName}`;
            link.dataset.tooltip = `${item.title} - ${item.authorName}`;
            const image = document.createElement("img");
            image.src = item.imageUrl || "";
            image.alt = `${item.title} 표지`;
            const title = document.createElement("span");
            title.textContent = item.title;
            link.append(image, title);
            books.append(link);
        });
        row.append(label, books);
        tierBoardElement.append(row);
    });
}

/* 2. 게시글 내용 출력 */
function renderPost(post) {
    document.title = `${post.title} | BookMate`;

    titleElement.textContent = post.title;
    categoryElement.textContent =
        categoryNames[post.category] || post.category || "-";
    genreElement.textContent = post.genre || "장르 없음";
    writerElement.textContent =
        post.memberNickname || "알 수 없음";
    viewCountElement.textContent =
        post.viewCount ?? 0;
    likeCountElement.textContent =
        post.likeCount ?? 0;

    createdAtElement.textContent =
        formatDateTime(post.createdAt);
    createdAtElement.dateTime =
        toDateTimeAttribute(post.createdAt);

    /* innerHTML을 사용하지 않아 게시글 내용에 포함된 스크립트 실행을 방지 */
    contentElement.textContent = post.content || "";

    statusElement.hidden = true;
    detailElement.hidden = false;
}

/* 3. 로그인 상태와 작성자에 따라 버튼 표시 */
function renderActions(post, auth, tierList) {
    writerActions.hidden = true;
    memberActions.hidden = true;

    if (!auth.loggedIn) {
        return;
    }

    const loginMemberId = Number(
        auth.memberId ??
        auth.loginMemberId
    );

    const postWriterId = Number(post.memberId);

    if (loginMemberId === postWriterId) {
        writerActions.hidden = false;
        editLink.href = tierList?.templateId
            ? `/pages/tier/maker.html?id=${encodeURIComponent(tierList.templateId)}`
            : `/pages/post/update.html?postId=${post.postId}`;
        return;
    }

    memberActions.hidden = false;
}

/* 4. 게시글 숨김 */
/* 4. 게시글 숨김 */
hideButton.addEventListener("click", async () => {
    if (!currentPost) {
        return;
    }

    const confirmed = confirm(
        "게시글을 숨기시겠습니까?\n숨긴 게시글은 커뮤니티 목록에서 표시되지 않습니다."
    );

    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch("/api/posts/hide", {
            method: "POST",
            headers: {
                "Content-Type": "application/json;charset=UTF-8"
            },
            body: JSON.stringify({
                postId: currentPost.postId
            })
        });

        const result = await response.json();

        if (!response.ok || !result.success) {
            throw new Error(result.message || "게시글 숨김 처리에 실패했습니다.");
        }

        window.location.replace("/pages/post/list.html");
    } catch (error) {
        console.error(error);
        alert(error.message || "게시글 숨김 처리 중 오류가 발생했습니다.");
    }
});

/* 5. 게시글 삭제 */
deleteButton.addEventListener("click", async () => {
    if (!currentPost) {
        return;
    }

    const confirmed = confirm(
        "게시글을 삭제하시겠습니까?\n" +
        "삭제한 게시글은 목록에서 표시되지 않습니다."
    );

    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch("/api/posts/delete", {
            method: "POST",
            headers: {
                "Content-Type": "application/json;charset=UTF-8"
            },
            body: JSON.stringify({
                postId: currentPost.postId
            })
        });

        const result = await response.json();

        if (!response.ok || !result.success) {
            throw new Error(
                result.message ||
                "게시글 삭제에 실패했습니다."
            );
        }

        window.location.replace("/pages/post/list.html");
    } catch (error) {
        console.error(error);
        alert(error.message || "게시글 삭제 중 오류가 발생했습니다.");
    }
});

/* 6. 좋아요 */
likeButton.addEventListener("click", () => {
    alert("좋아요 기능은 좋아요 테이블과 API 연결 후 사용할 수 있습니다.");
});

/* 7. 신고 */
reportButton.addEventListener("click", () => {
    alert("신고 기능은 신고 테이블과 API 연결 후 사용할 수 있습니다.");
});

/* 8. 오류 출력 */
function showError(message) {
    detailElement.hidden = true;
    statusElement.hidden = false;
    statusElement.textContent = message;
}

/* 9. 날짜 출력 형식 */
function formatDateTime(value) {
    if (!value) {
        return "-";
    }

    return String(value).substring(0, 19);
}

/* 10. time 태그 날짜 형식 */
function toDateTimeAttribute(value) {
    if (!value) {
        return "";
    }

    return String(value)
        .substring(0, 19)
        .replace(" ", "T");
}

/* 11. 게시글 상세 초기 실행 */
loadPostDetail();
