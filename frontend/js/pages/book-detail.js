const detailElement = document.querySelector("#book-detail");
const statusElement = document.querySelector("#book-detail-status");
const coverElement = document.querySelector("#book-cover");
const titleElement = document.querySelector("#book-title");
const authorElement = document.querySelector("#book-author");
const genreElement = document.querySelector("#book-genre");
const publisherElement = document.querySelector("#book-publisher");
const publishedDateElement = document.querySelector("#book-published-date");
const averageRatingElement = document.querySelector("#book-average-rating");
const ratingCountElement = document.querySelector("#book-rating-count");
const descriptionElement = document.querySelector("#book-description");
const detailContentElement = document.querySelector(".book-detail-content");
const ratingBookContextElement = document.querySelector("#rating-book-context");
const ratingBookTitleElement = document.querySelector("#rating-book-title");
const ratingBookAuthorElement = document.querySelector("#rating-book-author");
const ratingBookDescriptionElement = document.querySelector("#rating-book-description");
const ratingEditorElement = document.querySelector("#rating-editor");
const openRatingFormElement = document.querySelector("#open-rating-form");
const ratingToastElement = document.querySelector("#rating-toast");
const closeRatingFormElement = document.querySelector("#close-rating-form");
const ratingForm = document.querySelector("#rating-form");
const ratingCommentElement = document.querySelector("#rating-comment");
const ratingCommentCountElement = document.querySelector("#rating-comment-count");
const ratingSubmitElement = document.querySelector("#rating-submit");
const ratingMessageElement = document.querySelector("#rating-message");
let currentRating = null;
let isLoggedIn = false;
let ratingToastTimer = null;
let ratingToastClearTimer = null;

function getBookId() {
  const value = new URLSearchParams(window.location.search).get("id");
  return value && /^\d+$/.test(value) && Number(value) > 0 ? value : null;
}

function formatPublishedDate(value) {
  if (!value) return "출간일 미정";

  const isoDate = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value));
  if (isoDate) {
    return `${Number(isoDate[1])}년 ${Number(isoDate[2])}월 ${Number(isoDate[3])}일`;
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) return value;
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    timeZone: "UTC"
  }).format(parsedDate);
}

function renderCover(book) {
  coverElement.replaceChildren();
  if (!book.imageUrl) {
    coverElement.textContent = book.title;
    return;
  }

  const image = document.createElement("img");
  image.src = book.imageUrl;
  image.alt = `${book.title} 표지`;
  image.decoding = "async";
  image.addEventListener("error", () => {
    coverElement.textContent = book.title;
    image.remove();
  }, { once: true });
  coverElement.append(image);
}

function renderBook(book) {
  renderCover(book);
  titleElement.textContent = book.title;
  authorElement.textContent = book.authorName;
  genreElement.textContent = book.genre;
  publisherElement.textContent = book.publisher || "출판사 미정";
  publishedDateElement.textContent = formatPublishedDate(book.publishedDate);
  averageRatingElement.textContent = `★ ${Number(book.averageRating).toFixed(1)}`;
  ratingCountElement.textContent = `${book.ratingCount}명 참여`;
  descriptionElement.textContent = book.description || "등록된 책 소개가 없습니다.";
  ratingBookTitleElement.textContent = book.title;
  ratingBookAuthorElement.textContent = book.authorName;
  ratingBookDescriptionElement.textContent = book.description || "등록된 책 소개가 없습니다.";
  document.title = `${book.title} | BookMate`;

  statusElement.hidden = true;
  detailElement.hidden = false;
}

function showError(message) {
  detailElement.hidden = true;
  statusElement.hidden = false;
  statusElement.dataset.state = "error";
  statusElement.textContent = message;
}

async function loadBook() {
  const bookId = getBookId();
  if (!bookId) {
    showError("올바른 책 번호가 필요합니다. 책 목록에서 다시 선택해 주세요.");
    return;
  }

  try {
    const response = await fetch(`/bookmate/api/books/${bookId}`);
    const result = await response.json();
    if (!response.ok || !result.success) {
      throw new Error(result.message || "책 정보를 불러오지 못했습니다.");
    }
    renderBook(result.data);
    await loadMyRating(bookId);
  } catch (error) {
    const fallbackMessage = "책 정보를 불러오지 못했습니다. DB와 Tomcat 실행 상태를 확인해 주세요.";
    const message = error instanceof TypeError || error instanceof SyntaxError
      ? fallbackMessage
      : error.message || fallbackMessage;
    showError(message);
  }
}

async function loadMyRating(bookId) {
  try {
    const response = await fetch(`/bookmate/api/ratings?bookId=${bookId}`);
    if (response.status === 401) {
      isLoggedIn = false;
      currentRating = null;
      renderRatingState();
      return;
    }

    const result = await response.json();
    if (!response.ok || !result.success) {
      throw new Error(result.message || "내 평점을 불러오지 못했습니다.");
    }

    isLoggedIn = true;
    currentRating = result.data;
    renderRatingState();
  } catch (error) {
    currentRating = null;
    renderRatingState();
    console.error(error);
  }
}

function showRatingToast(message, state = "error") {
  window.clearTimeout(ratingToastTimer);
  window.clearTimeout(ratingToastClearTimer);
  ratingToastElement.textContent = message;
  ratingToastElement.dataset.state = state;
  ratingToastElement.dataset.visible = "true";

  ratingToastTimer = window.setTimeout(() => {
    delete ratingToastElement.dataset.visible;
    ratingToastClearTimer = window.setTimeout(() => {
      ratingToastElement.textContent = "";
      delete ratingToastElement.dataset.state;
    }, 300);
  }, 2200);
}

function renderRatingState() {
  const isUpdate = Boolean(currentRating);
  openRatingFormElement.textContent = isUpdate ? "평점 수정하기" : "평점 등록하기";
  ratingSubmitElement.textContent = isUpdate ? "평점 수정 완료" : "평점 기록 완료";
}

function prepareRatingForm() {
  ratingForm.reset();
  showRatingMessage("", "");

  if (currentRating) {
    const scoreInput = ratingForm.querySelector(`input[name="score"][value="${currentRating.score}"]`);
    if (scoreInput) scoreInput.checked = true;
    ratingCommentElement.value = currentRating.commentText || "";
  }

  ratingCommentCountElement.textContent = `${ratingCommentElement.value.length} / 500`;
}

function showRatingMessage(message, state) {
  ratingMessageElement.textContent = message;
  ratingMessageElement.dataset.state = state;
}

function setRatingMode(isRatingMode) {
  detailElement.classList.toggle("is-rating-mode", isRatingMode);
  detailContentElement.hidden = isRatingMode;
  ratingBookContextElement.hidden = !isRatingMode;
  ratingEditorElement.hidden = !isRatingMode;

  if (isRatingMode) {
    prepareRatingForm();
    ratingEditorElement.querySelector("h2").focus({ preventScroll: true });
    ratingEditorElement.scrollIntoView({ behavior: "smooth", block: "start" });
  } else {
    openRatingFormElement.focus({ preventScroll: true });
  }
}

async function submitRating(event) {
  event.preventDefault();

  const bookId = getBookId();
  const formData = new FormData(ratingForm);
  const score = Number(formData.get("score"));
  const commentText = String(formData.get("commentText") || "").trim();

  if (!bookId || !Number.isInteger(score) || score < 1 || score > 5) {
    showRatingMessage("1점부터 5점까지 평점을 선택해 주세요.", "error");
    return;
  }

  ratingSubmitElement.disabled = true;
  const isUpdate = Boolean(currentRating);
  showRatingMessage(isUpdate ? "평점을 수정하는 중입니다." : "평점을 등록하는 중입니다.", "loading");

  try {
    const response = await fetch("/bookmate/api/ratings", {
      method: isUpdate ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        ratingId: currentRating?.ratingId,
        bookId: Number(bookId),
        score,
        commentText
      })
    });
    const result = await response.json();

    if (!response.ok || !result.success) {
      throw new Error(result.message || `평점을 ${isUpdate ? "수정" : "등록"}하지 못했습니다.`);
    }

    await loadBook();
    setRatingMode(false);
    showRatingToast(`평점 ${isUpdate ? "수정" : "등록"}이 완료되었습니다.`, "success");
  } catch (error) {
    const fallbackMessage = `평점을 ${isUpdate ? "수정" : "등록"}하지 못했습니다. 잠시 후 다시 시도해 주세요.`;
    const message = error instanceof TypeError || error instanceof SyntaxError
      ? fallbackMessage
      : error.message || fallbackMessage;
    showRatingMessage(message, "error");
  } finally {
    ratingSubmitElement.disabled = false;
  }
}

ratingCommentElement.addEventListener("input", () => {
  ratingCommentCountElement.textContent = `${ratingCommentElement.value.length} / 500`;
});
openRatingFormElement.addEventListener("click", () => {
  if (!isLoggedIn) {
    showRatingToast("로그인이 필요한 기능입니다.");
    return;
  }
  setRatingMode(true);
});
closeRatingFormElement.addEventListener("click", () => setRatingMode(false));
ratingForm.addEventListener("submit", submitRating);
document.addEventListener("bookmate:auth-changed", () => {
  isLoggedIn = true;
  window.clearTimeout(ratingToastTimer);
  window.clearTimeout(ratingToastClearTimer);
  ratingToastElement.textContent = "";
  delete ratingToastElement.dataset.state;
  delete ratingToastElement.dataset.visible;
  const bookId = getBookId();
  if (bookId) loadMyRating(bookId);
});
loadBook();
