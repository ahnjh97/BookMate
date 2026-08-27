const form = document.querySelector("#post-create-form");
const message = document.querySelector("#message");

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const post = {
        category: document.querySelector("#category").value,
        title: document.querySelector("#title").value.trim(),
        content: document.querySelector("#content").value.trim()
    };

    message.textContent = "등록 중입니다.";

    try {
        const response = await fetch("/api/posts/create", {
            method: "POST",
            headers: {
                "Content-Type": "application/json;charset=UTF-8"
            },
            body: JSON.stringify(post)
        });

        const result = await response.json();

        if (response.status === 401) {
            alert("로그인이 필요합니다.");

            location.href =
                "/pages/auth/login.html";

            return;
        }

        if (!response.ok) {
            throw new Error(
                result.message || "게시글 등록에 실패했습니다."
            );
        }

        location.href =
            `/pages/post/detail.html?postId=${result.postId}`;

    } catch (error) {
        console.error(error);
        message.textContent = error.message;
    }
});