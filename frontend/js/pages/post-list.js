const postListElement =
    document.querySelector("#post-list");

const postStatusElement =
    document.querySelector("#post-status");

const categoryNames = {
    NOTICE: "공지",
    FREE: "자유",
    RECOMMEND: "추천",
    REVIEW: "리뷰",
    TIER: "티어",
    AUTHOR: "작가"
};

async function loadPosts() {
    postStatusElement.textContent =
        "게시글을 불러오는 중입니다.";

    postListElement.replaceChildren();

    try {
        const response = await fetch("/api/posts");

        const result = await response.json();

        if (!response.ok || !result.success) {
            throw new Error(
                result.message ||
                "게시글을 불러오지 못했습니다."
            );
        }

        renderPosts(result.posts);

    } catch (error) {
        console.error(error);

        postStatusElement.textContent =
            error.message ||
            "게시글을 불러오지 못했습니다.";
    }
}

function renderPosts(posts) {
    if (!Array.isArray(posts) || posts.length === 0) {
        postStatusElement.textContent =
            "등록된 게시글이 없습니다.";

        return;
    }

    const fragment = document.createDocumentFragment();

    posts.forEach((post) => {
        const row = document.createElement("tr");

        const numberCell = document.createElement("td");
        numberCell.className = "post-number";
        numberCell.textContent = post.postId;

        const categoryCell = document.createElement("td");
        categoryCell.className = "post-category";
        categoryCell.textContent =
            categoryNames[post.category] || post.category;

        const titleCell = document.createElement("td");
        titleCell.className = "post-title";

        const titleLink = document.createElement("a");

        titleLink.href =
            `/pages/post/detail.html?postId=${post.postId}`;

        // 사용자 입력을 HTML로 삽입하지 않고 안전하게 출력합니다.
        titleLink.textContent = post.title;

        titleCell.append(titleLink);

        const writerCell = document.createElement("td");
        writerCell.className = "post-writer";
        writerCell.textContent =
            post.memberNickname || "알 수 없음";

        const viewCell = document.createElement("td");
        viewCell.className = "post-views";
        viewCell.textContent = post.viewCount ?? 0;

        const dateCell = document.createElement("td");
        dateCell.className = "post-date";
        dateCell.textContent = formatDate(post.createdAt);

        row.append(
            numberCell,
            categoryCell,
            titleCell,
            writerCell,
            viewCell,
            dateCell
        );

        fragment.append(row);
    });

    postListElement.append(fragment);

    postStatusElement.textContent =
        `게시글 ${posts.length}개를 표시하고 있습니다.`;
}

function formatDate(value) {
    if (!value) {
        return "-";
    }

    /*
     * 백엔드가 yyyy-MM-dd HH:mm:ss 형식으로 반환하므로
     * 날짜 부분만 표시합니다.
     */
    return String(value).substring(0, 10);
}

loadPosts();