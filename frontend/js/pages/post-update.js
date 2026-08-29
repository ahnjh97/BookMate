const form = document.querySelector("#post-update-form");
const message = document.querySelector("#message");
const category = document.querySelector("#category");
const genre = document.querySelector("#genre");
const title = document.querySelector("#title");
const content = document.querySelector("#content");
const cancelLink = document.querySelector("#post-update-cancel-link");
const updatePage = document.querySelector("#post-update-page");

let postId = null;
let isDirty = false;
let isSubmitting = false;
let isRedirecting = false;

/* 1. 게시글 번호 확인 */
function getPostId() {
    const params = new URLSearchParams(location.search);
    const value = params.get("postId");

    if (!value || !/^\d+$/.test(value)) {
        return null;
    }

    return Number(value);
}

/* 2. 수정 페이지 초기화 */
async function initializeUpdatePage() {
    postId = getPostId();

    if (!postId) {
        showError("올바른 게시글 번호가 아닙니다.");
        return;
    }

    try {
        const [sessionResponse, postResponse] = await Promise.all([
            fetch("/api/auth/session", { cache: "no-store" }),
            fetch(`/api/posts/detail?postId=${encodeURIComponent(postId)}`)
        ]);

        const auth = sessionResponse.ok
            ? await sessionResponse.json()
            : { loggedIn: false };

        if (!auth.loggedIn) {
            isRedirecting = true;
            alert("로그인이 필요한 기능입니다.");
            window.location.replace("/pages/auth/login.html");
            return;
        }

        const result = await postResponse.json();

        if (!postResponse.ok || !result.success) {
            throw new Error(
                result.message ||
                "게시글을 불러오지 못했습니다."
            );
        }

        const post = result.post;
        const loginMemberId = Number(
            auth.memberId ??
            auth.loginMemberId
        );

        if (loginMemberId !== Number(post.memberId)) {
            isRedirecting = true;
            alert("게시글 작성자만 수정할 수 있습니다.");
            window.location.replace(
                `/pages/post/detail.html?postId=${postId}`
            );
            return;
        }

        category.value = post.category || "";
        genre.value = post.genre || "";
        title.value = post.title || "";
        content.value = post.content || "";

        cancelLink.href =
            `/pages/post/detail.html?postId=${postId}`;

        updatePage.hidden = false;
    } catch (error) {
        console.error(error);

        showError(
            error.message ||
            "게시글을 불러오지 못했습니다."
        );
    }
}

/* 3. 수정 내용 변경 감지 */
function updateDirtyState() {
    isDirty = true;
}

category.addEventListener("change", updateDirtyState);
genre.addEventListener("change", updateDirtyState);
title.addEventListener("input", updateDirtyState);
content.addEventListener("input", updateDirtyState);

/* 4. 수정 취소 확인 */
function confirmLeaveUpdate() {
    if (!isDirty) {
        return true;
    }

    return confirm(
        "게시글 수정을 취소하시겠습니까?\n" +
        "수정한 내용은 저장되지 않습니다."
    );
}

/* 5. 취소 버튼 */
cancelLink.addEventListener("click", event => {
    if (confirmLeaveUpdate()) {
        return;
    }

    event.preventDefault();
});

/* 6. 새로고침 및 창 닫기 경고 */
window.addEventListener("beforeunload", event => {
    if (!isDirty || isSubmitting || isRedirecting) {
        return;
    }

    event.preventDefault();
    event.returnValue = "";
});

/* 7. 게시글 수정 */
form.addEventListener("submit", async event => {
    event.preventDefault();

    const post = {
        postId,
        category: category.value,
        genre: genre.value || null,
        title: title.value.trim(),
        content: content.value.trim()
    };

    message.textContent = "수정 중입니다.";

    try {
        isSubmitting = true;

        const response = await fetch("/api/posts/update", {
            method: "POST",
            headers: {
                "Content-Type": "application/json;charset=UTF-8"
            },
            body: JSON.stringify(post)
        });

        const result = await response.json();

        if (response.status === 401) {
            isSubmitting = false;
            isRedirecting = true;

            alert("로그인이 필요합니다.");
            window.location.replace("/pages/auth/login.html");
            return;
        }

        if (!response.ok || !result.success) {
            isSubmitting = false;

            throw new Error(
                result.message ||
                "게시글 수정에 실패했습니다."
            );
        }

        isDirty = false;
        isSubmitting = false;
        isRedirecting = true;

        window.location.replace(
            `/pages/post/detail.html?postId=${result.postId}`
        );
    } catch (error) {
        isSubmitting = false;
        console.error(error);

        message.textContent =
            error.message ||
            "게시글 수정 중 오류가 발생했습니다.";
    }
});

/* 8. 오류 출력 */
function showError(errorMessage) {
    updatePage.hidden = true;
    message.textContent = errorMessage;
}

/* 9. 수정 페이지 초기 실행 */
initializeUpdatePage();