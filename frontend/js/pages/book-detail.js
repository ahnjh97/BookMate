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
  } catch (error) {
    const fallbackMessage = "책 정보를 불러오지 못했습니다. DB와 Tomcat 실행 상태를 확인해 주세요.";
    const message = error instanceof TypeError || error instanceof SyntaxError
      ? fallbackMessage
      : error.message || fallbackMessage;
    showError(message);
  }
}

loadBook();
