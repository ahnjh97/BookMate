// Figma 커뮤니티 표에 API 게시글을 연결합니다.
const postListElement = document.querySelector("#post-list");
const postStatusElement = document.querySelector("#post-status");

const categoryNames = {
    NOTICE: "공지",
    FREE: "자유",
    RECOMMEND: "추천",
    REVIEW: "리뷰",
    TIER: "티어",
    AUTHOR: "작가"
};

async function loadPosts() {
    postStatusElement.textContent = "게시글을 불러오는 중입니다.";
    postListElement.replaceChildren();

    try {
        const response = await fetch("/api/posts");
        const result = await response.json();

        if (!response.ok || !result.success) {
            throw new Error(result.message || "게시글을 불러오지 못했습니다.");
        }

        renderPosts(result.posts);
    } catch (error) {
        console.error(error);
        postStatusElement.textContent = error.message || "게시글을 불러오지 못했습니다.";
    }
}

// 카테고리를 제목 셀의 배지로 합치고 순위·작성자·날짜·조회수를 렌더링합니다.
function renderPosts(posts) {
    if (!Array.isArray(posts) || posts.length === 0) {
        postStatusElement.textContent = "등록된 게시글이 없습니다.";
        return;
    }

    const fragment = document.createDocumentFragment();

    posts.forEach((post, index) => {
        const row = document.createElement("tr");

        const numberCell = document.createElement("td");
        numberCell.className = "post-number";
        numberCell.textContent = index + 1;

        const titleCell = document.createElement("td");
        titleCell.className = "post-title";

        const titleInner = document.createElement("div");
        titleInner.className = "post-title-inner";

        const categoryBadge = document.createElement("span");
        categoryBadge.className = "post-category-badge";
        categoryBadge.textContent = categoryNames[post.category] || post.category;

        const titleLink = document.createElement("a");
        titleLink.href = `/pages/post/detail.html?postId=${post.postId}`;
        titleLink.textContent = post.title;

        titleInner.append(categoryBadge, titleLink);
        titleCell.append(titleInner);

        const writerCell = document.createElement("td");
        writerCell.className = "post-writer";
        writerCell.textContent = post.memberNickname || "알 수 없음";

        const dateCell = document.createElement("td");
        dateCell.className = "post-date";
        dateCell.textContent = formatDate(post.createdAt);

        const viewCell = document.createElement("td");
        viewCell.className = "post-views";
        viewCell.textContent = post.viewCount ?? 0;

        row.append(numberCell, titleCell, writerCell, dateCell, viewCell);
        fragment.append(row);
    });

    postListElement.append(fragment);
    postStatusElement.textContent = `게시글 ${posts.length}개를 표시하고 있습니다.`;
}

function formatDate(value) {
    return value ? String(value).substring(0, 10) : "-";
}

loadPosts();
