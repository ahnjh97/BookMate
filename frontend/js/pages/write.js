/* 1. 게시글 작성 폼 요소 */
const postForm = document.querySelector("#post-form");
const categoryInput = document.querySelector("#post-category");
const genreInput = document.querySelector("#post-genre");
const titleInput = document.querySelector("#post-title");
const contentInput = document.querySelector("#post-content");
const statusElement = document.querySelector("#post-status");

/* 2. 게시글 작성 요청 */
postForm.addEventListener("submit", async event => {
    event.preventDefault();

    const requestData = {
        category: categoryInput.value,
        genre: genreInput.value || null,
        title: titleInput.value.trim(),
        content: contentInput.value.trim()
    };

    statusElement.textContent = "게시글을 등록하는 중입니다.";

    try {
        const response = await fetch("/api/posts/create", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(requestData)
        });

        const result = await response.json();

        if (!response.ok || !result.success) {
            throw new Error(result.message || "게시글 등록에 실패했습니다.");
        }

        statusElement.textContent = "게시글이 등록되었습니다.";

        window.location.href =
            `/pages/post/detail.html?postId=${result.postId}`;
    } catch (error) {
        console.error(error);
        statusElement.textContent =
            error.message || "게시글 등록 중 오류가 발생했습니다.";
    }
});