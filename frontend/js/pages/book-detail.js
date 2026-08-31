const detailElement = document.querySelector("#book-detail");
const statusElement = document.querySelector("#book-detail-status");
const coverElement = document.querySelector("#book-cover");
const titleElement = document.querySelector("#book-title");
const authorElement = document.querySelector("#book-author");
const genreElement = document.querySelector("#book-genre");
const publisherElement = document.querySelector("#book-publisher");
const publishedDateElement = document.querySelector("#book-published-date");
const averageRatingElement = document.querySelector("#book-average-rating");
const ratingDistributionElement = document.querySelector("#book-rating-distribution");
const ratingDistributionTriggerElement = document.querySelector(".book-rating-distribution-trigger");
const ratingCountElement = document.querySelector("#book-rating-count");
const descriptionElement = document.querySelector("#book-description");
const sourceLinkElement = document.querySelector("#book-source-link");
const detailContentElement = document.querySelector(".book-detail-content");
const ratingBookContextElement = document.querySelector("#rating-book-context");
const ratingBookTitleElement = document.querySelector("#rating-book-title");
const ratingBookAuthorElement = document.querySelector("#rating-book-author");
const ratingBookDescriptionElement = document.querySelector("#rating-book-description");
const ratingEditorElement = document.querySelector("#rating-editor");
const openRatingFormElement = document.querySelector("#open-rating-form");
const myRatingSummaryElement = document.querySelector("#my-rating-summary");
const ratingToastElement = document.querySelector("#rating-toast");
const closeRatingFormElement = document.querySelector("#close-rating-form");
const ratingForm = document.querySelector("#rating-form");
const ratingCommentElement = document.querySelector("#rating-comment");
const ratingCommentCountElement = document.querySelector("#rating-comment-count");
const ratingSubmitElement = document.querySelector("#rating-submit");
const ratingMessageElement = document.querySelector("#rating-message");
const ratingScoreOptionsElement = document.querySelector(".rating-score-options");
const ratingStarLabels = [...ratingScoreOptionsElement.querySelectorAll("label")];
const readerRatingsElement = document.querySelector("#reader-ratings");
const readerRatingsSummaryElement = document.querySelector("#reader-ratings-summary");
const readerRatingsStatusElement = document.querySelector("#reader-ratings-status");
const readerRatingsListElement = document.querySelector("#reader-ratings-list");
const readerRatingsPaginationElement = document.querySelector("#reader-ratings-pagination");
const readerRatingFilterButtons = document.querySelectorAll("[data-reader-score]");
const readerRatingsPagination = window.BookMateListPagination.create({
  root: readerRatingsPaginationElement,
  pageSize: 4,
  onRender: renderReaderRatingPage,
  onPageChange: async (page) => {
    const bookId = getBookId();
    if (bookId) await loadPublicRatings(bookId, page);
  },
});
const bookCommunityElement = document.querySelector("#book-community");
const bookTemplatesHeadingElement = document.querySelector("#book-tier-templates-heading");
const bookTierTemplatesStatusElement = document.querySelector("#book-tier-templates-status");
const bookTierTemplatesListElement = document.querySelector("#book-tier-templates-list");
const bookTemplateTypeButtons = document.querySelectorAll("[data-template-type]");
const bookTierTemplatesPagination = window.BookMateListPagination.create({
  root: document.querySelector("#book-tier-templates-pagination"),
  pageSize: 4,
  onRender: renderBookTemplatePage,
});
const bookTemplates = { tier: [], worldcup: [] };
let selectedBookTemplateType = "tier";
let currentRating = null;
let isLoggedIn = false;
let currentRatingsPage = 1;
let currentReaderScore = null;
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
  image.src = book.detailImageUrl || book.imageUrl;
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
  const genreGroup = window.BookMateGenre.groupOf(book.genre);
  genreElement.textContent = genreGroup;
  genreElement.dataset.genre = genreGroup;
  publisherElement.textContent = book.publisher || "출판사 미정";
  publishedDateElement.textContent = formatPublishedDate(book.publishedDate);
  averageRatingElement.textContent = `★ ${Number(book.averageRating).toFixed(2)}`;
  renderRatingDistribution(book.ratingDistribution || {}, Number(book.ratingCount || 0));
  ratingCountElement.textContent = `${book.ratingCount}명 참여`;
  descriptionElement.textContent = book.description || "등록된 책 소개가 없습니다.";
  sourceLinkElement.hidden = !book.sourceUrl;
  if (book.sourceUrl) sourceLinkElement.href = book.sourceUrl;
  ratingBookTitleElement.textContent = book.title;
  ratingBookAuthorElement.textContent = book.authorName;
  ratingBookDescriptionElement.textContent = book.description || "등록된 책 소개가 없습니다.";
  document.title = `${book.title} | BookMate`;

  statusElement.hidden = true;
  detailElement.hidden = false;
}

function renderRatingDistribution(distribution, totalCount) {
  ratingDistributionElement.replaceChildren();
  const heading = document.createElement("strong");
  heading.textContent = `별점 분포 | 총 ${totalCount}명`;
  ratingDistributionElement.append(heading);
  for (let score = 5; score >= 1; score--) {
    const count = Number(distribution[score] ?? distribution[String(score)] ?? 0);
    const ratio = totalCount > 0 ? Math.min(100, Math.max(0, count / totalCount * 100)) : 0;
    const row = document.createElement("div");
    row.className = "book-rating-distribution-row";
    row.innerHTML = `<span>${score}점</span><i><b style="width:${ratio.toFixed(1)}%"></b></i><em>${count}명 (${ratio.toFixed(1)}%)</em>`;
    ratingDistributionElement.append(row);
  }
}

function setRatingDistributionOpen(isOpen) {
  ratingDistributionTriggerElement.classList.toggle("is-open", isOpen);
  ratingDistributionElement.setAttribute("aria-hidden", String(!isOpen));
}

ratingDistributionTriggerElement.addEventListener("mouseenter", () => {
  setRatingDistributionOpen(true);
});

ratingDistributionTriggerElement.addEventListener("mouseleave", () => {
  setRatingDistributionOpen(false);
});

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
    const response = await fetch(`/api/books/${bookId}`);
    const result = await response.json();
    if (!response.ok || !result.success) {
      throw new Error(result.message || "책 정보를 불러오지 못했습니다.");
    }
    renderBook(result.data);
    await loadBookTemplates(bookId);
    await loadMyRating(bookId);
    await loadPublicRatings(bookId, currentRatingsPage);
  } catch (error) {
    const fallbackMessage = "책 정보를 불러오지 못했습니다. DB와 Tomcat 실행 상태를 확인해 주세요.";
    const message = error instanceof TypeError || error instanceof SyntaxError
      ? fallbackMessage
      : error.message || fallbackMessage;
    showError(message);
  }
}

async function loadBookTemplates(bookId) {
  try {
    const [tierResponse, worldcupResponse] = await Promise.all([
      fetch(`/api/tier-templates?bookId=${bookId}`),
      fetch(`/api/worldcup/templates?bookId=${bookId}`),
    ]);
    const [tierResult, worldcupResult] = await Promise.all([
      tierResponse.json(),
      worldcupResponse.json(),
    ]);
    if (!tierResponse.ok || !tierResult.success) {
      throw new Error(tierResult.message || "티어리스트를 불러오지 못했습니다.");
    }
    if (!worldcupResponse.ok || !worldcupResult.success) {
      throw new Error(worldcupResult.message || "이상형월드컵을 불러오지 못했습니다.");
    }
    bookTemplates.tier = tierResult.templates || [];
    bookTemplates.worldcup = worldcupResult.templates || [];
    renderSelectedBookTemplates();
  } catch (error) {
    bookTemplates.tier = [];
    bookTemplates.worldcup = [];
    bookTierTemplatesPagination.setItems([]);
    bookTierTemplatesStatusElement.textContent = error.message || "템플릿을 불러오지 못했습니다.";
  }
}

function renderSelectedBookTemplates() {
  const templates = bookTemplates[selectedBookTemplateType];
  bookTemplatesHeadingElement.textContent = selectedBookTemplateType === "tier"
    ? "이 책이 포함된 티어리스트 템플릿"
    : "이 책이 포함된 이상형월드컵 템플릿";
  bookTierTemplatesStatusElement.textContent = `총 ${templates.length}개`;
  bookTierTemplatesPagination.setItems(templates);
}

function renderBookTemplatePage(templates) {
  bookTierTemplatesListElement.replaceChildren();
  templates.forEach((template) => {
      const link = document.createElement("a");
      link.className = "book-tier-template-link";
      link.href = selectedBookTemplateType === "tier"
        ? `/pages/tier/maker.html?id=${template.templateId}`
        : `/pages/worldcup/play.html?id=${template.templateId}`;
      const categoryClass = getTierCategoryClass(template.category);
      link.innerHTML = `<strong>${escapeHtml(template.title)}</strong><span class="category-chip ${categoryClass}">${escapeHtml(template.category)}</span>`;
      bookTierTemplatesListElement.append(link);
  });
}

bookTemplateTypeButtons.forEach((button) => {
  button.addEventListener("click", () => {
    selectedBookTemplateType = button.dataset.templateType;
    bookTemplateTypeButtons.forEach((item) => {
      const selected = item === button;
      item.classList.toggle("is-active", selected);
      item.setAttribute("aria-selected", String(selected));
    });
    renderSelectedBookTemplates();
  });
});

function getTierCategoryClass(category) {
  return {
    "장르": "category-genre",
    "시리즈": "category-series",
    "작가": "category-author",
    "테마": "category-theme",
  }[category] || "category-default";
}

function escapeHtml(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
}

async function loadPublicRatings(bookId, page = 1) {
  readerRatingsStatusElement.textContent = "평가를 불러오는 중입니다.";
  try {
    const query = new URLSearchParams({ bookId: String(bookId), page: String(page) });
    if (currentReaderScore != null) query.set("score", String(currentReaderScore));
    const response = await fetch(`/api/ratings/public?${query}`);
    const result = await response.json();
    if (!response.ok || !result.success) throw new Error(result.message || "독자 평가를 불러오지 못했습니다.");
    currentRatingsPage = result.data.page;
    renderPublicRatings(result.data);
  } catch (error) {
    readerRatingsListElement.replaceChildren();
    readerRatingsPagination.setPage([], 1, 0);
    readerRatingsStatusElement.textContent = error.message || "독자 평가를 불러오지 못했습니다.";
    readerRatingsStatusElement.dataset.state = "error";
  }
}

function renderPublicRatings(data) {
  readerRatingsSummaryElement.textContent = currentReaderScore == null
    ? `총 ${data.totalCount}개의 평가`
    : `${currentReaderScore}점 평가 ${data.totalCount}개`;
  readerRatingsStatusElement.textContent = "";
  delete readerRatingsStatusElement.dataset.state;
  readerRatingsPagination.setPage(data.ratings, data.page, data.totalCount);
}

function renderReaderRatingPage(ratings) {
  readerRatingsListElement.replaceChildren();
  ratings.forEach((rating) => readerRatingsListElement.append(createRatingCard(rating)));
}

readerRatingFilterButtons.forEach(button => {
  button.addEventListener("click", async () => {
    currentReaderScore = button.dataset.readerScore === "all"
      ? null
      : Number(button.dataset.readerScore);
    currentRatingsPage = 1;
    readerRatingFilterButtons.forEach(item => {
      const active = item === button;
      item.classList.toggle("is-active", active);
      item.setAttribute("aria-pressed", String(active));
    });
    const bookId = getBookId();
    if (bookId) await loadPublicRatings(bookId, 1);
  });
});

function createRatingCard(rating) {
  const article = document.createElement("article");
  article.className = "reader-rating-card";
  const heading = document.createElement("div");
  heading.className = "reader-rating-card-heading";
  const nickname = document.createElement("strong");
  nickname.textContent = rating.nickname;
  const score = document.createElement("span");
  score.className = "reader-rating-score";
  score.setAttribute("aria-label", `5점 만점에 ${rating.score}점`);
  score.textContent = `${"★".repeat(rating.score)}${"☆".repeat(5 - rating.score)}`;
  const comment = document.createElement("p");
  comment.textContent = rating.commentText || "한줄평이 없습니다.";
  if (!rating.commentText) comment.className = "reader-rating-empty-comment";
  const headingMeta = document.createElement("div");
  headingMeta.className = "reader-rating-card-meta";
  headingMeta.append(score);

  if (currentRating?.ratingId === rating.ratingId) {
    headingMeta.append(createRatingDeleteControl(rating, article));
    article.classList.add("is-my-rating");
  }

  heading.append(nickname, headingMeta);
  article.append(heading, comment);
  return article;
}

function createRatingDeleteControl(rating, cardElement) {
  const control = document.createElement("div");
  control.className = "reader-rating-delete-control";

  const deleteButton = document.createElement("button");
  deleteButton.type = "button";
  deleteButton.className = "reader-rating-delete-button";
  deleteButton.textContent = "삭제";
  deleteButton.setAttribute("aria-expanded", "false");

  const confirmation = document.createElement("div");
  confirmation.className = "reader-rating-delete-confirmation";
  confirmation.hidden = true;
  confirmation.setAttribute("role", "group");
  confirmation.setAttribute("aria-label", "평점 삭제 확인");

  const question = document.createElement("span");
  question.textContent = "삭제할까요?";
  const confirmButton = document.createElement("button");
  confirmButton.type = "button";
  confirmButton.className = "reader-rating-confirm-button";
  confirmButton.textContent = "확인";
  const cancelButton = document.createElement("button");
  cancelButton.type = "button";
  cancelButton.className = "reader-rating-cancel-button";
  cancelButton.textContent = "취소";
  confirmation.append(question, confirmButton, cancelButton);

  const toast = document.createElement("p");
  toast.className = "reader-rating-delete-toast";
  toast.setAttribute("role", "status");
  toast.setAttribute("aria-live", "polite");

  deleteButton.addEventListener("click", () => {
    confirmation.hidden = false;
    deleteButton.hidden = true;
    deleteButton.setAttribute("aria-expanded", "true");
    confirmButton.focus();
  });

  cancelButton.addEventListener("click", () => {
    confirmation.hidden = true;
    deleteButton.hidden = false;
    deleteButton.setAttribute("aria-expanded", "false");
    showCardToast(toast, "삭제를 취소했습니다.", "neutral");
    deleteButton.focus();
  });

  confirmButton.addEventListener("click", async () => {
    await deleteMyRating(rating, cardElement, control, toast);
  });

  control.append(deleteButton, confirmation, toast);
  return control;
}

function showCardToast(element, message, state) {
  window.clearTimeout(Number(element.dataset.timerId));
  element.textContent = message;
  element.dataset.state = state;
  element.dataset.visible = "true";
  const timerId = window.setTimeout(() => {
    delete element.dataset.visible;
  }, 2200);
  element.dataset.timerId = String(timerId);
}

async function deleteMyRating(rating, cardElement, controlElement, toastElement) {
  const bookId = getBookId();
  const buttons = controlElement.querySelectorAll("button");
  buttons.forEach((button) => { button.disabled = true; });
  showCardToast(toastElement, "삭제하는 중입니다.", "loading");

  try {
    const params = new URLSearchParams({
      ratingId: String(rating.ratingId),
      bookId: String(bookId)
    });
    const response = await fetch(`/api/ratings?${params}`, { method: "DELETE" });
    const result = await response.json();
    if (!response.ok || !result.success) {
      throw new Error(result.message || "평점을 삭제하지 못했습니다.");
    }

    currentRating = null;
    cardElement.classList.add("is-deleted");
    showCardToast(toastElement, "평점이 삭제되었습니다.", "success");
    await new Promise((resolve) => window.setTimeout(resolve, 700));
    await loadBook();
  } catch (error) {
    const fallbackMessage = "평점을 삭제하지 못했습니다. 잠시 후 다시 시도해 주세요.";
    const message = error instanceof TypeError || error instanceof SyntaxError
      ? fallbackMessage
      : error.message || fallbackMessage;
    showCardToast(toastElement, message, "error");
    buttons.forEach((button) => { button.disabled = false; });
  }
}

async function loadMyRating(bookId) {
  try {
    const response = await fetch(`/api/ratings?bookId=${bookId}`);
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
  myRatingSummaryElement.hidden = !isUpdate;
  myRatingSummaryElement.textContent = isUpdate ? `내 평점 ★ ${Number(currentRating.score).toFixed(1)}` : "";
}

function prepareRatingForm() {
  ratingForm.reset();
  showRatingMessage("", "");

  if (currentRating) {
    const scoreInput = ratingForm.querySelector(`input[name="score"][value="${currentRating.score}"]`);
    if (scoreInput) scoreInput.checked = true;
    ratingCommentElement.value = currentRating.commentText || "";
  }

  paintRatingStars(Number(ratingForm.querySelector('input[name="score"]:checked')?.value || 0));

  ratingCommentCountElement.textContent = `${ratingCommentElement.value.length} / 500`;
}

function paintRatingStars(score) {
  ratingStarLabels.forEach((label, index) => label.classList.toggle("is-filled", index < score));
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
  bookCommunityElement.hidden = isRatingMode;

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
    const response = await fetch("/api/ratings", {
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

    currentRatingsPage = 1;
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
ratingStarLabels.forEach((label) => {
  const input = label.querySelector('input[name="score"]');
  label.addEventListener("mouseenter", () => paintRatingStars(Number(input.value)));
  input.addEventListener("change", () => {
    paintRatingStars(Number(input.value));
    label.classList.remove("is-clicked");
    void label.offsetWidth;
    label.classList.add("is-clicked");
    window.setTimeout(() => label.classList.remove("is-clicked"), 450);
  });
});
ratingScoreOptionsElement.addEventListener("mouseleave", () => {
  paintRatingStars(Number(ratingForm.querySelector('input[name="score"]:checked')?.value || 0));
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
loadBook();
