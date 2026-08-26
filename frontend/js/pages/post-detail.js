const detailElement =
    document.querySelector("#post-detail");

const statusElement =
    document.querySelector("#post-detail-status");

const titleElement =
    document.querySelector("#post-title");

const categoryElement =
    document.querySelector("#post-category");

const writerElement =
    document.querySelector("#post-writer");

const viewCountElement =
    document.querySelector("#post-view-count");

const createdAtElement =
    document.querySelector("#post-created-at");

const contentElement =
    document.querySelector("#post-content");

const categoryNames = {
    NOTICE: "공지",
    FREE: "자유",
    RECOMMEND: "추천",
    REVIEW: "리뷰",
    TIER: "티어",
    AUTHOR: "작가"
};

async function loadPostDetail() {
    const params = new URLSearchParams(location.search);
    const postId = params.get("postId");

    if (!postId || !/^\d+$/.test(postId)) {
        showError("올바른 게시글 번호가 아닙니다.");
        return;
    }

    try {
        const response = await fetch(
            `/bookmate/api/posts/detail?postId=${encodeURIComponent(postId)}`
        );

        const result = await response.json();

        if (!response.ok || !result.success) {
            throw new Error(
                result.message ||
                "게시글을 불러오지 못했습니다."
            );
        }

        renderPost(result.post);

    } catch (error) {
        console.error(error);

        showError(
            error.message ||
            "게시글을 불러오지 못했습니다."
        );
    }
}

function renderPost(post) {
    document.title = `${post.title} | BookMate`;

    titleElement.textContent = post.title;

    categoryElement.textContent =
        categoryNames[post.category] || post.category;

    writerElement.textContent =
        post.memberNickname || "알 수 없음";

    viewCountElement.textContent =
        post.viewCount ?? 0;

    createdAtElement.textContent =
        formatDateTime(post.createdAt);

    createdAtElement.dateTime =
        toDateTimeAttribute(post.createdAt);

    /*
     * innerHTML을 사용하지 않아 게시글 내용에 포함된
     * 스크립트가 실행되지 않도록 합니다.
     *
     * CSS white-space: pre-wrap으로 줄바꿈을 유지합니다.
     */
    contentElement.textContent = post.content || "";

    statusElement.hidden = true;
    detailElement.hidden = false;
}

function showError(message) {
    detailElement.hidden = true;
    statusElement.hidden = false;
    statusElement.textContent = message;
}

function formatDateTime(value) {
    if (!value) {
        return "-";
    }

    return String(value).substring(0, 19);
}

function toDateTimeAttribute(value) {
    if (!value) {
        return "";
    }

    return String(value)
        .substring(0, 19)
        .replace(" ", "T");
}

loadPostDetail();