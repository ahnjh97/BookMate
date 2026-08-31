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
const worldcupResultElement = document.querySelector("#post-worldcup-result");
const worldcupHeadingElement = document.querySelector("#post-worldcup-heading");
const worldcupWinnerElement = document.querySelector("#post-worldcup-winner");
const likeCountElement = document.querySelector("#post-like-count");
const writerActions = document.querySelector("#writer-actions");
const memberActions = document.querySelector("#member-actions");
const editLink = document.querySelector("#post-edit-link");
const hideButton = document.querySelector("#post-hide-button");
const deleteButton = document.querySelector("#post-delete-button");
const likeButton = document.querySelector("#post-like-button");
const reportButton = document.querySelector("#post-report-button");
const commentForm = document.querySelector("#post-comment-form");
const commentContent = document.querySelector("#post-comment-content");
const commentLength = document.querySelector("#post-comment-length");
const commentCount = document.querySelector("#post-comment-count");
const commentMessage = document.querySelector("#post-comment-message");
const commentList = document.querySelector("#post-comment-list");

const categoryNames = {
    NOTICE: "공지",
    FREE: "자유",
    RECOMMEND: "추천",
    REVIEW: "리뷰",
    TIER: "티어리스트",
    WORLDCUP: "이상형월드컵"
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
            fetch("/api/auth", { cache: "no-store" })
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

        renderPost(currentPost, postResult.likeCount);
        renderTierList(postResult.tierList);
        renderWorldcupResult(postResult.worldcupResult);
        renderActions(currentPost, currentMember, postResult.tierList, postResult.worldcupResult, postResult.liked);
        await loadComments();
    } catch (error) {
        console.error(error);
        showError(
            error.message ||
            "게시글을 불러오지 못했습니다."
        );
    }
}

function renderWorldcupResult(result) {
    worldcupWinnerElement.replaceChildren();
    worldcupResultElement.hidden = !result;
    detailElement.classList.toggle("has-worldcup-result", Boolean(result));
    if (!result) return;
    worldcupHeadingElement.textContent = `${result.title} 결과`;
    const finalMatch = (result.matches || []).find(match => match.roundSize === 2);
    if (!finalMatch) return;
    const winner = finalMatch.winner;
    const link = document.createElement("a");
    link.className = "post-worldcup-winner";
    link.href = `/pages/worldcup/result.html?id=${encodeURIComponent(result.runId)}`;
    link.innerHTML = `${winner.imageUrl ? `<img src="${escapeHtml(winner.imageUrl)}" alt="">` : ""}`
        + `<span><small class="post-worldcup-winner-label">최종 우승</small><strong>${escapeHtml(winner.title)}</strong>`
        + `<b>전체 대진표 보기</b></span>`;
    worldcupWinnerElement.append(link);
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
function renderPost(post, likeCount) {
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
function renderActions(post, auth, tierList, worldcupResult, liked) {
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
            : worldcupResult?.runId
                ? `/pages/worldcup/result.html?id=${encodeURIComponent(worldcupResult.runId)}`
                : `/pages/post/update.html?postId=${post.postId}`;
        return;
    }

    memberActions.hidden = false;
    likeButton.textContent = liked ? "좋아요 취소" : "좋아요";
}

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

/* 6. 게시글 좋아요 */
likeButton.addEventListener("click", async () => {
    if (!currentPost) {
        return;
    }

    try {
        const response = await fetch("/api/posts/like", {
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
            throw new Error(result.message || "좋아요 처리에 실패했습니다.");
        }

        likeCountElement.textContent = result.likeCount;
        likeButton.textContent = result.liked ? "좋아요 취소" : "좋아요";
    } catch (error) {
        console.error(error);
        alert(error.message || "좋아요 처리 중 오류가 발생했습니다.");
    }
});

/* 7. 신고 */
reportButton.addEventListener("click", () => {
    alert("신고 기능은 신고 테이블과 API 연결 후 사용할 수 있습니다.");
});

async function loadComments() {
    try {
        const response = await fetch(`/api/posts/comments?postId=${encodeURIComponent(currentPost.postId)}`, { cache: "no-store" });
        const result = await response.json();
        if (!response.ok || !result.success) throw new Error(result.message || "댓글을 불러오지 못했습니다.");
        commentCount.textContent = Number(result.count || 0).toLocaleString();
        renderComments(result.comments || []);
        commentMessage.textContent = "";
    } catch (error) {
        commentMessage.textContent = error.message || "댓글을 불러오지 못했습니다.";
    }
}

function renderComments(comments) {
    commentList.replaceChildren();
    if (!comments.length) {
        const empty = document.createElement("p");
        empty.className = "post-comments-empty";
        empty.textContent = "첫 댓글을 남겨보세요.";
        commentList.append(empty);
        return;
    }
    comments.forEach(comment => commentList.append(createCommentElement(comment)));
}

function createCommentElement(comment) {
    const article = document.createElement("article");
    article.className = "post-comment";
    if (comment.parentCommentId) article.classList.add("is-reply");
    const heading = document.createElement("div");
    heading.className = "post-comment-heading";
    const writer = document.createElement("strong");
    writer.textContent = comment.memberNickname || "회원";
    const time = document.createElement("time");
    time.textContent = formatDateTime(comment.updatedAt || comment.createdAt);
    heading.append(writer, time);
    const body = document.createElement("p");
    body.className = "post-comment-content";
    const active = comment.status === "ACTIVE";
    body.textContent = active ? comment.content : "삭제된 댓글입니다.";
    if (!active) article.classList.add("is-deleted");
    article.append(heading, body);
    if (active) {
        const actions = document.createElement("div");
        actions.className = "post-comment-actions";
        if (currentMember?.loggedIn) actions.append(commentAction("답글", () => showReplyEditor(article, comment.commentId)));
        const ownComment = Number(currentMember?.memberId ?? currentMember?.loginMemberId) === Number(comment.memberId);
        if (ownComment) actions.append(commentAction("수정", () => showCommentEditor(article, comment)));
        if (ownComment || currentMember?.role === "ADMIN") actions.append(commentAction("삭제", () => deleteComment(comment.commentId)));
        article.append(actions);
    }
    return article;
}

function commentAction(label, handler) {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = label;
    button.addEventListener("click", handler);
    return button;
}

function showReplyEditor(article, parentCommentId) {
    document.querySelectorAll(".post-comment-inline-form").forEach(form => form.remove());
    article.append(inlineCommentForm("답글을 입력해 주세요.", "답글 등록", content =>
        saveComment({ postId: currentPost.postId, parentCommentId, content }, "POST")));
}

function showCommentEditor(article, comment) {
    document.querySelectorAll(".post-comment-inline-form").forEach(form => form.remove());
    article.append(inlineCommentForm("댓글을 수정해 주세요.", "수정 완료", content =>
        saveComment({ commentId: comment.commentId, content }, "PUT"), comment.content));
}

function inlineCommentForm(placeholder, submitLabel, onSubmit, initialValue = "") {
    const form = document.createElement("form");
    form.className = "post-comment-inline-form";
    const textarea = document.createElement("textarea");
    textarea.maxLength = 1000;
    textarea.rows = 2;
    textarea.placeholder = placeholder;
    textarea.value = initialValue;
    const actions = document.createElement("div");
    const cancel = document.createElement("button");
    cancel.type = "button";
    cancel.className = "button button-outline";
    cancel.textContent = "취소";
    cancel.addEventListener("click", () => form.remove());
    const submit = document.createElement("button");
    submit.type = "submit";
    submit.className = "button button-teal";
    submit.textContent = submitLabel;
    actions.append(cancel, submit);
    form.append(textarea, actions);
    form.addEventListener("submit", async event => {
        event.preventDefault();
        submit.disabled = true;
        try { await onSubmit(textarea.value); }
        catch (error) { commentMessage.textContent = error.message; }
        finally { submit.disabled = false; }
    });
    return form;
}

async function saveComment(payload, method) {
    const response = await fetch("/api/posts/comments", {
        method,
        headers: { "Content-Type": "application/json;charset=UTF-8" },
        body: JSON.stringify(payload)
    });
    const result = await response.json();
    if (response.status === 401) { requestLogin(); throw new Error(result.message); }
    if (!response.ok || !result.success) throw new Error(result.message || "댓글을 저장하지 못했습니다.");
    commentForm.reset();
    commentLength.textContent = "0 / 1000";
    await loadComments();
}

async function deleteComment(commentId) {
    if (!confirm("댓글을 삭제하시겠습니까?")) return;
    try { await saveComment({ commentId }, "DELETE"); }
    catch (error) { commentMessage.textContent = error.message; }
}

function requestLogin() {
    if (!confirm("로그인이 필요한 기능입니다.\n로그인 페이지로 이동하시겠습니까?")) return;
    const returnUrl = `${location.pathname}${location.search}`;
    location.assign(`/pages/auth/login.html?redirect=${encodeURIComponent(returnUrl)}`);
}

commentContent.addEventListener("input", () => {
    commentLength.textContent = `${commentContent.value.length} / 1000`;
});

commentForm.addEventListener("submit", async event => {
    event.preventDefault();
    if (!currentMember?.loggedIn) { requestLogin(); return; }
    const submit = commentForm.querySelector('[type="submit"]');
    submit.disabled = true;
    try {
        await saveComment({ postId: currentPost.postId, content: commentContent.value }, "POST");
    } catch (error) {
        commentMessage.textContent = error.message || "댓글을 등록하지 못했습니다.";
    } finally {
        submit.disabled = false;
    }
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

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

/* 11. 게시글 상세 초기 실행 */
loadPostDetail();
