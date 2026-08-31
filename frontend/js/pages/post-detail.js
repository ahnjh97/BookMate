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
const commentContentElement = document.querySelector("#post-comment-content");
const commentSubmitButton = document.querySelector("#post-comment-submit");
const commentLengthElement = document.querySelector("#post-comment-length");
const commentStatusElement = document.querySelector("#post-comment-status");
const commentListElement = document.querySelector("#post-comment-list");
const commentCountElement = document.querySelector("#post-comment-count");

const reportModal = document.querySelector("#report-modal");
const reportForm = document.querySelector("#report-form");
const reportReasonTypeElement = document.querySelector("#report-reason-type");
const reportReasonDetailElement = document.querySelector("#report-reason-detail");
const reportDetailGroup = document.querySelector("#report-detail-group");
const reportDetailLengthElement = document.querySelector("#report-detail-length");
const reportSubmitButton = document.querySelector("#report-submit-button");
const reportCloseButtons = document.querySelectorAll("[data-report-close]");
const reportModalCloseButton = document.querySelector("#report-modal-close");

const categoryNames = {
    NOTICE: "공지",
    FREE: "자유",
    RECOMMEND: "추천",
    REVIEW: "리뷰",
    TIER: "티어리스트",
    WORLDCUP: "이상형월드컵",
    AUTHOR: "작가"
};

let currentPost = null;
let currentMember = null;
let currentReportTargetType = null;
let currentReportTargetId = null;

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

        renderPost(
            currentPost,
            postResult.likeCount
        );

        renderTierList(
            postResult.tierList
        );

        renderWorldcupResult(
            postResult.worldcupResult
        );

        renderActions(
            currentPost,
            currentMember,
            postResult.tierList,
            postResult.worldcupResult,
            postResult.liked
        );

        renderCommentForm(currentMember);

        await loadComments();
    } catch (error) {
        console.error(error);

        showError(
            error.message ||
            "게시글을 불러오지 못했습니다."
        );
    }
}

/* 2. 티어리스트 결과 출력 */
function renderTierList(tierList) {
    tierBoardElement.replaceChildren();

    tierListElement.hidden = !tierList;

    detailElement.classList.toggle(
        "has-tier-list",
        Boolean(tierList)
    );

    if (!tierList) {
        return;
    }

    ["S", "A", "B", "C", "D"].forEach(grade => {
        const row = document.createElement("div");
        row.className = "post-tier-row";

        const label = document.createElement("strong");
        label.textContent = grade;

        const books = document.createElement("div");
        books.className = "post-tier-books";

        (tierList.items || [])
            .filter(item => item.grade === grade)
            .forEach(item => {
                const link = document.createElement("a");
                link.className = "post-tier-book";
                link.href =
                    `/pages/book/detail.html?id=${encodeURIComponent(item.bookId)}`;
                link.title =
                    `${item.title} - ${item.authorName}`;
                link.dataset.tooltip =
                    `${item.title} - ${item.authorName}`;

                const image = document.createElement("img");
                image.src = item.imageUrl || "";
                image.alt = `${item.title} 표지`;

                const title = document.createElement("span");
                title.textContent = item.title;

                link.append(
                    image,
                    title
                );

                books.append(link);
            });

        row.append(
            label,
            books
        );

        tierBoardElement.append(row);
    });
}

/* 3. 이상형월드컵 결과 출력 */
function renderWorldcupResult(result) {
    worldcupWinnerElement.replaceChildren();
    worldcupResultElement.hidden = !result;

    detailElement.classList.toggle(
        "has-worldcup-result",
        Boolean(result)
    );

    if (!result) {
        return;
    }

    worldcupHeadingElement.textContent =
        `${result.title} 결과`;

    const finalMatch = (result.matches || [])
        .find(match => match.roundSize === 2);

    if (!finalMatch || !finalMatch.winner) {
        return;
    }

    const winner = finalMatch.winner;

    const link = document.createElement("a");
    link.className = "post-worldcup-winner";
    link.href =
        `/pages/worldcup/result.html?id=${encodeURIComponent(result.runId)}`;

    if (winner.imageUrl) {
        const image = document.createElement("img");
        image.src = winner.imageUrl;
        image.alt = `${winner.title} 표지`;
        link.append(image);
    }

    const text = document.createElement("span");

    const label = document.createElement("small");
    label.className = "post-worldcup-winner-label";
    label.textContent = "최종 우승";

    const title = document.createElement("strong");
    title.textContent = winner.title;

    const detail = document.createElement("b");
    detail.textContent = "전체 대진표 보기";

    text.append(
        label,
        title,
        detail
    );

    link.append(text);
    worldcupWinnerElement.append(link);
}

/* 4. 게시글 내용 출력 */
function renderPost(post, likeCount) {
    document.title =
        `${post.title} | BookMate`;

    titleElement.textContent =
        post.title;

    categoryElement.textContent =
        categoryNames[post.category] ||
        post.category ||
        "-";

    genreElement.textContent =
        post.genre ||
        "장르 없음";

    window.BookMateBookshelfVisit.render(
        writerElement,
        post.memberNickname || "알 수 없음",
        post.memberId
    );

    viewCountElement.textContent =
        post.viewCount ?? 0;

    likeCountElement.textContent =
        likeCount ??
        post.likeCount ??
        0;

    createdAtElement.textContent =
        formatDateTime(post.createdAt);

    createdAtElement.dateTime =
        toDateTimeAttribute(post.createdAt);

    /* innerHTML을 사용하지 않아 게시글 내용에 포함된 스크립트 실행을 방지 */
    contentElement.textContent =
        post.content || "";

    statusElement.hidden = true;
    detailElement.hidden = false;
}

/* 5. 로그인 상태와 작성자에 따라 버튼 표시 */
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

    const postWriterId =
        Number(post.memberId);

    if (loginMemberId === postWriterId) {
        writerActions.hidden = false;

        editLink.href =
            tierList?.templateId
                ? `/pages/tier/maker.html?id=${encodeURIComponent(tierList.templateId)}`
                : worldcupResult?.runId
                    ? `/pages/worldcup/result.html?id=${encodeURIComponent(worldcupResult.runId)}`
                    : `/pages/post/update.html?postId=${post.postId}`;

        return;
    }

    memberActions.hidden = false;

    likeButton.textContent =
        liked
            ? "좋아요 취소"
            : "좋아요";
}

/* 6. 댓글 작성 영역 표시 */
function renderCommentForm(auth) {
    commentForm.hidden = false;
}

function requireCommentLogin() {
    if (currentMember?.loggedIn) return true;
    window.BookMateLoginPrompt.show();
    return false;
}

/* 7. 댓글 목록 조회 */
async function loadComments() {
    if (!currentPost) {
        return;
    }

    commentStatusElement.hidden = false;
    commentStatusElement.textContent =
        "댓글을 불러오는 중입니다.";

    commentListElement.replaceChildren();

    try {
        const response = await fetch(
            `/api/posts/comments?postId=${encodeURIComponent(currentPost.postId)}`
        );

        const result =
            await response.json();

        if (!response.ok || !result.success) {
            throw new Error(
                result.message ||
                "댓글을 불러오지 못했습니다."
            );
        }

        const comments =
            Array.isArray(result.comments)
                ? result.comments
                : [];

        renderComments(comments);
    } catch (error) {
        console.error(error);

        commentStatusElement.hidden = false;
        commentStatusElement.textContent =
            error.message ||
            "댓글을 불러오지 못했습니다.";
    }
}

/* 8. 댓글 목록 출력 */
function renderComments(comments) {
    commentListElement.replaceChildren();

    commentCountElement.textContent =
        comments.length;

    if (comments.length === 0) {
        commentStatusElement.hidden = true;
        commentStatusElement.textContent = "";
        return;
    }

    commentStatusElement.hidden = true;

    const fragment =
        document.createDocumentFragment();

    const loginMemberId = Number(
        currentMember?.memberId ??
        currentMember?.loginMemberId
    );

    comments.forEach(comment => {
        const item =
            document.createElement("article");

        item.className =
            "post-comment-item";

        const header =
            document.createElement("div");

        header.className =
            "post-comment-item-header";

        const writer = document.createElement("span");
        writer.className = "post-comment-member";
        window.BookMateBookshelfVisit.render(
            writer,
            comment.memberNickname || "알 수 없음",
            comment.memberId
        );

        const headerRight =
            document.createElement("div");

        headerRight.className =
            "post-comment-item-header-right";

        const date =
            document.createElement("time");

        date.textContent =
            formatDateTime(comment.createdAt);

        date.dateTime =
            toDateTimeAttribute(comment.createdAt);

        headerRight.append(date);

        const commentWriterId =
            Number(comment.memberId);

        if (currentMember?.loggedIn) {
            if (loginMemberId === commentWriterId) {
                item.classList.add("is-my-comment");
                const editButton =
                    document.createElement("button");

                editButton.type = "button";
                editButton.className =
                    "post-comment-action-button";
                editButton.textContent = "수정";

                editButton.addEventListener(
                    "click",
                    () => {
                        startCommentEdit(
                            item,
                            comment
                        );
                    }
                );

                const deleteCommentButton =
                    document.createElement("button");

                deleteCommentButton.type = "button";
                deleteCommentButton.className =
                    "post-comment-action-button";
                deleteCommentButton.textContent = "삭제";

                deleteCommentButton.addEventListener(
                    "click",
                    () => {
                        deleteComment(
                            comment.commentId
                        );
                    }
                );

                headerRight.append(
                    editButton,
                    deleteCommentButton
                );
            } else {
                const reportCommentButton =
                    document.createElement("button");

                reportCommentButton.type =
                    "button";

                reportCommentButton.className =
                    "post-comment-action-button";

                reportCommentButton.textContent =
                    "신고";

                reportCommentButton.addEventListener(
                    "click",
                    () => {
                        openReportModal(
                            "COMMENT",
                            comment.commentId
                        );
                    }
                );

                headerRight.append(
                    reportCommentButton
                );
            }
        }

        header.append(
            writer,
            headerRight
        );

        const content =
            document.createElement("p");

        content.className =
            "post-comment-content";

        content.textContent =
            comment.content || "";

        item.append(
            header,
            content
        );

        fragment.append(item);
    });

    commentListElement.append(fragment);
}

/* 9. 댓글 수정 화면 표시 */
function startCommentEdit(item, comment) {
    const existingEditor =
        document.querySelector(
            ".post-comment-edit-form"
        );

    if (existingEditor) {
        alert("수정 중인 댓글이 있습니다.");
        return;
    }

    const contentElement =
        item.querySelector(
            ".post-comment-content"
        );

    contentElement.hidden = true;

    const form =
        document.createElement("form");

    form.className =
        "post-comment-edit-form";

    const textarea =
        document.createElement("textarea");

    textarea.maxLength = 1000;
    textarea.value =
        comment.content || "";

    const footer =
        document.createElement("div");

    footer.className =
        "post-comment-edit-actions";

    const length =
        document.createElement("span");

    length.textContent =
        `${textarea.value.length} / 1000`;

    textarea.addEventListener(
        "input",
        () => {
            length.textContent =
                `${textarea.value.length} / 1000`;
        }
    );

    const buttonGroup =
        document.createElement("div");

    buttonGroup.className =
        "post-comment-edit-buttons";

    const cancelButton =
        document.createElement("button");

    cancelButton.type = "button";
    cancelButton.className =
        "button";

    cancelButton.textContent =
        "취소";

    cancelButton.addEventListener(
        "click",
        () => {
            form.remove();
            contentElement.hidden = false;
        }
    );

    const saveButton =
        document.createElement("button");

    saveButton.type = "submit";
    saveButton.className =
        "button button-primary";

    saveButton.textContent =
        "수정 완료";

    buttonGroup.append(
        cancelButton,
        saveButton
    );

    footer.append(
        length,
        buttonGroup
    );

    form.append(
        textarea,
        footer
    );

    item.append(form);

    textarea.focus();

    form.addEventListener(
        "submit",
        async event => {
            event.preventDefault();

            const content =
                textarea.value.trim();

            if (!content) {
                alert(
                    "댓글 내용을 입력해주세요."
                );

                textarea.focus();
                return;
            }

            saveButton.disabled = true;

            try {
                const response =
                    await fetch(
                        "/api/posts/comments/update",
                        {
                            method: "POST",
                            headers: {
                                "Content-Type":
                                    "application/json;charset=UTF-8"
                            },
                            body: JSON.stringify({
                                commentId:
                                comment.commentId,
                                content
                            })
                        }
                    );

                const result =
                    await response.json();

                if (
                    !response.ok ||
                    !result.success
                ) {
                    throw new Error(
                        result.message ||
                        "댓글 수정에 실패했습니다."
                    );
                }

                await loadComments();
            } catch (error) {
                console.error(error);

                alert(
                    error.message ||
                    "댓글 수정 중 오류가 발생했습니다."
                );

                saveButton.disabled = false;
            }
        }
    );
}

/* 10. 댓글 삭제 */
async function deleteComment(commentId) {
    const confirmed =
        confirm("댓글을 삭제하시겠습니까?");

    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch(
            "/api/posts/comments/delete",
            {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/json;charset=UTF-8"
                },
                body: JSON.stringify({
                    commentId
                })
            }
        );

        const result =
            await response.json();

        if (
            !response.ok ||
            !result.success
        ) {
            throw new Error(
                result.message ||
                "댓글 삭제에 실패했습니다."
            );
        }

        await loadComments();
    } catch (error) {
        console.error(error);

        alert(
            error.message ||
            "댓글 삭제 중 오류가 발생했습니다."
        );
    }
}

/* 11. 신고 모달 열기 */
function openReportModal(targetType, targetId) {
    currentReportTargetType =
        targetType;

    currentReportTargetId =
        targetId;

    reportReasonTypeElement.value = "";
    reportReasonDetailElement.value = "";

    reportDetailGroup.hidden = true;

    updateReportDetailLength();

    reportModal.hidden = false;

    document.body.classList.add(
        "report-modal-open"
    );

    reportReasonTypeElement.focus();
}

/* 12. 신고 모달 닫기 */
function closeReportModal() {
    reportModal.hidden = true;

    document.body.classList.remove(
        "report-modal-open"
    );

    currentReportTargetType = null;
    currentReportTargetId = null;

    reportForm.reset();
    reportDetailGroup.hidden = true;

    updateReportDetailLength();
}

/* 13. 신고 모달 닫기 버튼 */
reportCloseButtons.forEach(button => {
    button.addEventListener(
        "click",
        closeReportModal
    );
});

reportModalCloseButton.addEventListener(
    "click",
    closeReportModal
);

/* 14. 신고 사유 변경 */
reportReasonTypeElement.addEventListener(
    "change",
    () => {
        const isOther =
            reportReasonTypeElement.value ===
            "OTHER";

        reportDetailGroup.hidden =
            !isOther;

        if (!isOther) {
            reportReasonDetailElement.value =
                "";

            updateReportDetailLength();
        }
    }
);

/* 15. 신고 상세 글자 수 */
reportReasonDetailElement.addEventListener(
    "input",
    updateReportDetailLength
);

function updateReportDetailLength() {
    reportDetailLengthElement.textContent =
        `${reportReasonDetailElement.value.length} / 1000`;
}

/* 16. 신고 등록 */
reportForm.addEventListener(
    "submit",
    async event => {
        event.preventDefault();

        if (
            !currentReportTargetType ||
            !currentReportTargetId
        ) {
            return;
        }

        const reasonType =
            reportReasonTypeElement.value;

        const reasonDetail =
            reportReasonDetailElement.value.trim();

        if (!reasonType) {
            alert("신고 사유를 선택해주세요.");
            reportReasonTypeElement.focus();
            return;
        }

        if (
            reasonType === "OTHER" &&
            !reasonDetail
        ) {
            alert("기타 신고 사유를 입력해주세요.");
            reportReasonDetailElement.focus();
            return;
        }

        const confirmed =
            confirm("신고를 접수하시겠습니까?");

        if (!confirmed) {
            return;
        }

        reportSubmitButton.disabled = true;

        try {
            const response = await fetch(
                "/api/reports/create",
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json;charset=UTF-8"
                    },
                    body: JSON.stringify({
                        targetType:
                        currentReportTargetType,
                        targetId:
                        currentReportTargetId,
                        reasonType,
                        reasonDetail:
                            reasonDetail || null
                    })
                }
            );

            const result =
                await response.json();

            if (!response.ok || !result.success) {
                throw new Error(
                    result.message ||
                    "신고 접수에 실패했습니다."
                );
            }

            closeReportModal();

            alert("신고가 접수되었습니다.");
        } catch (error) {
            console.error(error);

            alert(
                error.message ||
                "신고 처리 중 오류가 발생했습니다."
            );
        } finally {
            reportSubmitButton.disabled =
                false;
        }
    }
);

/* 17. 댓글 등록 */
commentForm.addEventListener(
    "submit",
    async event => {
        event.preventDefault();

        if (!requireCommentLogin()) return;

        if (!currentPost) {
            return;
        }

        const content =
            commentContentElement.value.trim();

        if (!content) {
            alert("댓글 내용을 입력해주세요.");
            commentContentElement.focus();
            return;
        }

        commentSubmitButton.disabled = true;

        try {
            const response = await fetch(
                "/api/posts/comments/create",
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json;charset=UTF-8"
                    },
                    body: JSON.stringify({
                        postId:
                        currentPost.postId,
                        content
                    })
                }
            );

            const result =
                await response.json();

            if (!response.ok || !result.success) {
                throw new Error(
                    result.message ||
                    "댓글 등록에 실패했습니다."
                );
            }

            commentContentElement.value = "";

            updateCommentLength();

            await loadComments();
        } catch (error) {
            console.error(error);

            alert(
                error.message ||
                "댓글 등록 중 오류가 발생했습니다."
            );
        } finally {
            commentSubmitButton.disabled =
                false;
        }
    }
);

/* 18. 댓글 글자 수 표시 */
commentContentElement.addEventListener(
    "input",
    updateCommentLength
);

commentContentElement.addEventListener("focus", () => {
    if (requireCommentLogin()) return;
    commentContentElement.blur();
});

function updateCommentLength() {
    commentLengthElement.textContent =
        `${commentContentElement.value.length} / 1000`;
}

/* 19. 게시글 숨김 */
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
        const response = await fetch(
            "/api/posts/hide",
            {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/json;charset=UTF-8"
                },
                body: JSON.stringify({
                    postId:
                    currentPost.postId
                })
            }
        );

        const result =
            await response.json();

        if (!response.ok || !result.success) {
            throw new Error(
                result.message ||
                "게시글 숨김 처리에 실패했습니다."
            );
        }

        window.location.replace(
            "/pages/post/list.html"
        );
    } catch (error) {
        console.error(error);

        alert(
            error.message ||
            "게시글 숨김 처리 중 오류가 발생했습니다."
        );
    }
});

/* 20. 게시글 삭제 */
deleteButton.addEventListener("click", async () => {
    if (!currentPost) {
        return;
    }

    const confirmed = confirm(
        "게시글을 삭제하시겠습니까?\n삭제한 게시글은 목록에서 표시되지 않습니다."
    );

    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch(
            "/api/posts/delete",
            {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/json;charset=UTF-8"
                },
                body: JSON.stringify({
                    postId:
                    currentPost.postId
                })
            }
        );

        const result =
            await response.json();

        if (!response.ok || !result.success) {
            throw new Error(
                result.message ||
                "게시글 삭제에 실패했습니다."
            );
        }

        window.location.replace(
            "/pages/post/list.html"
        );
    } catch (error) {
        console.error(error);

        alert(
            error.message ||
            "게시글 삭제 중 오류가 발생했습니다."
        );
    }
});

/* 21. 게시글 좋아요 */
likeButton.addEventListener("click", async () => {
    if (!currentPost) {
        return;
    }

    try {
        const response = await fetch(
            "/api/posts/like",
            {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/json;charset=UTF-8"
                },
                body: JSON.stringify({
                    postId:
                    currentPost.postId
                })
            }
        );

        const result =
            await response.json();

        if (!response.ok || !result.success) {
            throw new Error(
                result.message ||
                "좋아요 처리에 실패했습니다."
            );
        }

        likeCountElement.textContent =
            result.likeCount;

        likeButton.textContent =
            result.liked
                ? "좋아요 취소"
                : "좋아요";
    } catch (error) {
        console.error(error);

        alert(
            error.message ||
            "좋아요 처리 중 오류가 발생했습니다."
        );
    }
});

/* 22. 게시글 신고 */
reportButton.addEventListener("click", () => {
    if (!currentPost) {
        return;
    }

    openReportModal(
        "POST",
        currentPost.postId
    );
});

/* 23. 오류 출력 */
function showError(message) {
    detailElement.hidden = true;
    statusElement.hidden = false;
    statusElement.textContent = message;
}

/* 24. 날짜 출력 형식 */
function formatDateTime(value) {
    if (!value) {
        return "-";
    }

    return String(value)
        .substring(0, 19);
}

/* 25. time 태그 날짜 형식 */
function toDateTimeAttribute(value) {
    if (!value) {
        return "";
    }

    return String(value)
        .substring(0, 19)
        .replace(" ", "T");
}

/* 26. 게시글 상세 초기 실행 */
updateCommentLength();
updateReportDetailLength();
loadPostDetail();
