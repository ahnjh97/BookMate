const searchForm = document.querySelector("#book-search-form");
const keywordInput = document.querySelector("#book-keyword");
const genreSelect = document.querySelector("#book-genre");
const statusElement = document.querySelector("#book-status");
const listElement = document.querySelector("#book-list");
const suggestionsElement = document.querySelector("#search-suggestions");
const suggestionCache = new Map();
let suggestionTimer;
let suggestionRequest;

keywordInput.addEventListener("input", () => {
  clearTimeout(suggestionTimer);
  suggestionRequest?.abort();

  const keyword = keywordInput.value.trim();
  if (!keyword) {
    closeSuggestions();
    return;
  }

  suggestionTimer = setTimeout(() => loadSuggestions(keyword), 250);
});

keywordInput.addEventListener("keydown", (event) => {
  if (event.key === "Escape") closeSuggestions();
});

document.addEventListener("click", (event) => {
  if (!searchForm.contains(event.target)) closeSuggestions();
});

async function loadSuggestions(keyword) {
  if (suggestionCache.has(keyword)) {
    renderSuggestions(suggestionCache.get(keyword));
    return;
  }

  suggestionRequest = new AbortController();

  try {
    const response = await fetch(`/bookmate/api/search/suggestions?q=${encodeURIComponent(keyword)}`, {
      signal: suggestionRequest.signal
    });
    const result = await response.json();
    if (!response.ok || !result.success) throw new Error(result.message);

    suggestionCache.set(keyword, result.data);
    if (suggestionCache.size > 50) {
      suggestionCache.delete(suggestionCache.keys().next().value);
    }

    if (keywordInput.value.trim() === keyword) renderSuggestions(result.data);
  } catch (error) {
    if (error.name !== "AbortError") closeSuggestions();
  }
}

function renderSuggestions(suggestions) {
  suggestionsElement.replaceChildren();

  suggestions.forEach((suggestion) => {
    const option = document.createElement("button");
    option.type = "button";
    option.setAttribute("role", "option");

    const type = document.createElement("span");
    type.textContent = suggestion.type === "BOOK" ? "책" : "작가";

    const name = document.createElement("strong");
    name.textContent = suggestion.name;

    const detail = document.createElement("small");
    detail.textContent = suggestion.detail;

    option.append(type, name, detail);
    option.addEventListener("click", () => {
      keywordInput.value = suggestion.name;
      closeSuggestions();
      loadBooks();
    });
    suggestionsElement.append(option);
  });

  suggestionsElement.hidden = suggestions.length === 0;
}

function closeSuggestions() {
  suggestionsElement.hidden = true;
  suggestionsElement.replaceChildren();
}

async function loadBooks() {
  const params = new URLSearchParams();
  if (keywordInput.value.trim()) params.set("keyword", keywordInput.value.trim());
  if (genreSelect.value) params.set("genre", genreSelect.value);
  params.set("page", 1);
  params.set("size", 100);

  statusElement.textContent = "책 목록을 불러오는 중입니다.";
  listElement.replaceChildren();

  try {
    const query = params.toString();
    const response = await fetch(`/bookmate/api/books${query ? `?${query}` : ""}`);
    const result = await response.json();
    if (!response.ok || !result.success) throw new Error(result.message);
    renderBooks(result.data.books);
  } catch (error) {
    statusElement.textContent = "책 목록을 불러오지 못했습니다. DB와 Tomcat 실행 상태를 확인해 주세요.";
  }
}

function renderBooks(books) {
  if (books.length === 0) {
    statusElement.textContent = "검색 조건에 맞는 책이 없습니다.";
    return;
  }

  books.forEach((book) => {
    const card = document.createElement("article");
    card.className = "book-card";

    const cover = document.createElement("figure");

    const image = document.createElement("img");
    image.src = book.imageUrl || "";
    image.alt = `${book.title} 표지`;
    image.loading = "lazy";
    image.decoding = "async";
    image.addEventListener("error", () => {
      cover.textContent = book.title;
      image.remove();
    }, { once: true });
    cover.append(image);

    const content = document.createElement("div");

    const heading = document.createElement("h2");
    heading.textContent = book.title;

    const author = document.createElement("p");
    author.textContent = book.authorName;

    const meta = document.createElement("p");
    meta.textContent = `${book.genre} · ${book.publisher || "출판사 미정"}`;

    const description = document.createElement("p");
    description.textContent = book.description || "등록된 책 소개가 없습니다.";

    const rating = document.createElement("strong");
    rating.textContent = `★ ${Number(book.averageRating).toFixed(1)} (${book.ratingCount}명)`;

    content.append(heading, author, meta, description, rating);
    card.append(cover, content);
    listElement.append(card);
  });

  statusElement.textContent = `현재 ${listElement.childElementCount}권을 표시하고 있습니다.`;
}

searchForm.addEventListener("submit", (event) => {
  event.preventDefault();
  closeSuggestions();
  loadBooks();
});

genreSelect.addEventListener("change", loadBooks);
loadBooks();
