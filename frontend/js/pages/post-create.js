const form = document.querySelector("#post-create-form");
const message = document.querySelector("#message");
const category = document.querySelector("#category");
const title = document.querySelector("#title");
const content = document.querySelector("#content");
const cancelLink = document.querySelector("#post-cancel-link");
const postCreatePage = document.querySelector("#post-create-page");

let isDirty = false;
let isSubmitting = false;
let isRedirecting = false;

/* 1. 로그인 여부 확인 */
async function checkLogin() {
    try {
        const response = await fetch("/api/auth/session", { cache: "no-store" });
        const auth = response.ok ? await response.json() : { loggedIn: false };

        /* 로그인 회원 */
        if (auth.loggedIn) {
            postCreatePage.hidden = false;
            return;
        }

        /* 비회원 */
        postCreatePage.hidden = true;
        isRedirecting = true;

        const goLogin = confirm(
            "로그인이 필요한 기능입니다.\n" +
            "로그인 페이지로 이동하시겠습니까?"
        );

        if (goLogin) {
            window.location.replace("/pages/auth/login.html");
        } else {
            window.location.replace("/pages/post/list.html");
        }
    } catch (error) {
        console.error("로그인 상태 확인 실패:", error);
        postCreatePage.hidden = true;
        window.location.replace("/pages/post/list.html");
    }
}

/* 페이지 진입 즉시 로그인 확인 */
checkLogin();

/* 2. 현재 작성 내용이 있는지 확인 */
function hasWrittenContent() {
    return (
        category.value !== "" ||
        title.value.trim() !== "" ||
        content.value.trim() !== ""
    );
}

/* 3. 카테고리 / 제목 / 내용 변경 감지 */
function updateDirtyState() {
    isDirty = hasWrittenContent();
}

category.addEventListener("change", updateDirtyState);
title.addEventListener("input", updateDirtyState);
content.addEventListener("input", updateDirtyState);

/* 4. 작성 취소 여부 확인 */
function confirmLeavePostCreate() {
    if (!isDirty) {
        return true;
    }

    return confirm(
        "게시글 작성을 취소하시겠습니까?\n" +
        "게시글은 임시저장 되지 않습니다."
    );
}

/* 5. 아래쪽 '취소' 버튼 */
cancelLink.addEventListener("click", (event) => {
    if (confirmLeavePostCreate()) {
        return;
    }

    event.preventDefault();
});

/* 6. 상단 네비게이션 이동 감지 */
document.addEventListener("click", (event) => {
    const link = event.target.closest("[data-navbar] a[href]");

    if (!link || !isDirty) {
        return;
    }

    const confirmed = confirmLeavePostCreate();

    if (!confirmed) {
        event.preventDefault();
    }
});

/* 7. 새로고침 / 창 닫기 방지 */
window.addEventListener("beforeunload", (event) => {
    if (!isDirty || isSubmitting || isRedirecting) {
        return;
    }

    event.preventDefault();

    /* Chrome / Edge에서는 사용자 지정 문구 대신 브라우저 기본 경고문 표시 */
    event.returnValue = "";
});

/* 8. 게시글 등록 */
form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const post = {
        category: category.value,
        title: title.value.trim(),
        content: content.value.trim()
    };

    message.textContent = "등록 중입니다.";

    try {
        isSubmitting = true;

        const response = await fetch("/api/posts/create", {
            method: "POST",
            headers: {
                "Content-Type": "application/json;charset=UTF-8"
            },
            body: JSON.stringify(post)
        });

        const result = await response.json();

        /* 세션이 중간에 만료된 경우 */
        if (response.status === 401) {
            isSubmitting = false;
            isRedirecting = true;

            const goLogin = confirm(
                "로그인이 필요합니다.\n" +
                "로그인 페이지로 이동하시겠습니까?"
            );

            if (goLogin) {
                window.location.replace("/pages/auth/login.html");
            } else {
                window.location.replace("/pages/post/list.html");
            }

            return;
        }

        if (!response.ok) {
            isSubmitting = false;
            throw new Error(result.message || "게시글 등록에 실패했습니다.");
        }

        /* 정상 등록 완료 후 상세 페이지 이동 */
        isDirty = false;
        isSubmitting = false;
        window.location.href = `/pages/post/detail.html?postId=${result.postId}`;
    } catch (error) {
        isSubmitting = false;
        console.error(error);
        message.textContent = error.message;
    }
});