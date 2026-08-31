const searchForm = document.querySelector("#book-search-form");
const queryInput = document.querySelector("#book-catalog-query");
const searchMessage = document.querySelector("#book-search-message");
const searchResults = document.querySelector("#book-search-results");
const selectedBookSection = document.querySelector("#selected-book");
const selectedBookContent = document.querySelector("#selected-book-content");
const requestMessage = document.querySelector("#book-request-message");
const clearSelectedButton = document.querySelector("#clear-selected-book");
const submitRequestButton = document.querySelector("#submit-book-request");

let selectedBook = null;
let searchController = null;
const searchPagination = window.BookMateListPagination.create({
  root: document.querySelector("#book-search-pagination"),
  pageSize: 4,
  onRender: renderResultPage,
});

function show(element, message, state = "") {
  element.textContent = message;
  if (state) element.dataset.state = state;
  else delete element.dataset.state;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function bookMarkup(book, selectable) {
  return `
    <img src="${escapeHtml(book.imageUrl)}" alt="${escapeHtml(book.title)} 표지"
         loading="lazy" decoding="async" referrerpolicy="no-referrer">
    <div class="book-search-card-body">
      <span class="book-search-genre">${escapeHtml(book.genre)}</span>
      <h3>${escapeHtml(book.title)}</h3>
      <p>${escapeHtml(book.authorName)} | ${escapeHtml(book.publisher)}</p>
      <p>${escapeHtml(book.publishedDate)} | ISBN ${escapeHtml(book.isbn)}</p>
      <p class="book-search-description">${escapeHtml(book.description)}</p>
      <div class="book-search-card-actions">
        <a href="${escapeHtml(book.sourceUrl)}" target="_blank" rel="noopener noreferrer">알라딘에서 도서 정보 보기</a>
        ${selectable ? '<button class="button button-outline" type="button">이 책 선택</button>' : ""}
      </div>
    </div>`;
}

function renderResults(books) {
  searchPagination.setItems(books);
}

function renderResultPage(books) {
  searchResults.replaceChildren();
  books.forEach(book => {
    const card = document.createElement("article");
    card.className = "book-search-card";
    card.innerHTML = bookMarkup(book, true);
    card.querySelector("button").addEventListener("click", () => selectBook(book));
    searchResults.append(card);
  });
}

async function selectBook(book) {
  show(searchMessage, "ISBN 중복 여부를 확인하는 중입니다.");
  try {
    const response = await fetch(`/api/books/isbn?value=${encodeURIComponent(book.isbn)}`);
    const result = await response.json();
    if (!response.ok || !result.success) throw new Error(result.message || "ISBN을 확인하지 못했습니다.");
    if (!result.available) throw new Error("이미 등록되었거나 검토 중인 책입니다.");

    selectedBook = book;
    selectedBookContent.innerHTML = bookMarkup(book, false);
    selectedBookSection.hidden = false;
    searchPagination.setItems([]);
    show(searchMessage, "");
    selectedBookSection.scrollIntoView({ block: "nearest" });
  } catch (error) {
    show(searchMessage, error.message, "error");
  }
}

searchForm.addEventListener("submit", async event => {
  event.preventDefault();
  const query = queryInput.value.trim();
  if (query.length < 2) {
    show(searchMessage, "검색어를 두 글자 이상 입력해 주세요.", "error");
    return;
  }
  if (searchController) searchController.abort();
  searchController = new AbortController();
  const submit = searchForm.querySelector("button");
  submit.disabled = true;
  selectedBook = null;
  selectedBookSection.hidden = true;
  searchPagination.setItems([]);
  show(searchMessage, "알라딘에서 도서를 검색하는 중입니다.");
  try {
    const response = await fetch(`/api/books/catalog?query=${encodeURIComponent(query)}`, {
      credentials: "include",
      signal: searchController.signal
    });
    const result = await response.json();
    if (!response.ok || !result.success) throw new Error(result.message || "도서를 검색하지 못했습니다.");
    const books = Array.isArray(result.books) ? result.books : [];
    renderResults(books);
    show(searchMessage, books.length
      ? `필수 정보가 모두 확인된 도서 ${books.length}권입니다.`
      : "필수 정보가 모두 등록된 검색 결과가 없습니다.");
  } catch (error) {
    if (error.name !== "AbortError") show(searchMessage, error.message, "error");
  } finally {
    submit.disabled = false;
  }
});

clearSelectedButton.addEventListener("click", () => {
  selectedBook = null;
  selectedBookSection.hidden = true;
  queryInput.focus();
});

submitRequestButton.addEventListener("click", async () => {
  if (!selectedBook) return;
  submitRequestButton.disabled = true;
  show(requestMessage, "알라딘 도서 정보를 다시 확인하고 있습니다.");
  try {
    const response = await fetch("/api/books/requests", {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ isbn: selectedBook.isbn })
    });
    const result = await response.json();
    if (!response.ok || !result.success) throw new Error(result.message || "책 등록에 실패했습니다.");
    sessionStorage.setItem("bookmate:flash-toast", JSON.stringify({ message: result.message, state: "success" }));
    location.assign("/pages/book/list.html");
  } catch (error) {
    show(requestMessage, error.message, "error");
    submitRequestButton.disabled = false;
  }
});
