// Figma 책 목록 UI: 검색, 장르 필터, 카드 렌더링을 페이지 구조에 맞게 연결합니다.
const searchForm = document.querySelector("#book-search-form");
const keywordInput = document.querySelector("#book-keyword");
const genreSelect = document.querySelector("#book-genre");
const statusElement = document.querySelector("#book-status");
const listElement = document.querySelector("#book-list");
const suggestionsElement = document.querySelector("#search-suggestions");
const categoryButtons = document.querySelectorAll(".book-category-tabs [data-genre]");
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
        const response = await fetch(`/api/search/suggestions?q=${encodeURIComponent(keyword)}`, {
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

// 장르 칩과 기존 select의 선택 상태를 동기화합니다.
function updateActiveCategory() {
    categoryButtons.forEach((button) => {
        const isActive = button.dataset.genre === genreSelect.value;
        button.classList.toggle("is-active", isActive);
        button.setAttribute("aria-pressed", String(isActive));
    });
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
        const response = await fetch(`/api/books${query ? `?${query}` : ""}`);
        const result = await response.json();
        if (!response.ok || !result.success) throw new Error(result.message);
        renderBooks(result.data.books);
    } catch (error) {
        statusElement.textContent = "책 목록을 불러오지 못했습니다. DB와 Tomcat 실행 상태를 확인해 주세요.";
    }
}

// API 응답을 Figma의 표지 중심 카드(순위·장르·별점)로 구성합니다.
function renderBooks(books) {
    if (!Array.isArray(books) || books.length === 0) {
        statusElement.textContent = "검색 조건에 맞는 책이 없습니다.";
        return;
    }

    books.forEach((book, index) => {
        const detailUrl = `/pages/book/detail.html?id=${encodeURIComponent(book.bookId)}`;
        const card = document.createElement("a");
        card.className = "book-card";
        card.href = detailUrl;
        card.setAttribute("aria-label", `${book.title} 상세 보기`);

        const cover = document.createElement("figure");
        cover.className = "book-card-cover";

        const rank = document.createElement("span");
        rank.className = "book-rank";
        rank.textContent = index + 1;

        const image = document.createElement("img");
        image.src = book.imageUrl || "";
        image.alt = `${book.title} 표지`;
        image.loading = "lazy";
        image.decoding = "async";
        image.addEventListener("error", () => {
            cover.textContent = book.title;
            cover.prepend(rank);
            image.remove();
        }, {once: true});

        cover.append(rank, image);

        const content = document.createElement("div");
        content.className = "book-card-content";

        const genre = document.createElement("span");
        genre.className = "book-genre";
        genre.textContent = book.genre || "도서";

        const heading = document.createElement("h2");
        heading.className = "book-title";
        heading.textContent = book.title;

        const author = document.createElement("p");
        author.className = "book-author";
        author.textContent = book.authorName;

        const rating = document.createElement("p");
        rating.className = "book-rating";

        const star = document.createElement("span");
        star.className = "book-rating-star";
        star.textContent = "★";

        const score = document.createElement("span");
        score.className = "book-rating-score";
        score.textContent = Number(book.averageRating || 0).toFixed(1);

        const count = document.createElement("span");
        count.className = "book-rating-count";
        count.textContent = `(${book.ratingCount || 0}명)`;

        rating.append(star, score, count);
        content.append(genre, heading, author, rating);
        card.append(cover, content);
        listElement.append(card);
    });

    statusElement.textContent = `현재 ${listElement.childElementCount}권을 표시하고 있습니다.`;
}

// 새로 추가한 장르 칩으로 기존 책 목록 API를 다시 조회합니다.
categoryButtons.forEach((button) => {
    button.addEventListener("click", () => {
        genreSelect.value = button.dataset.genre;
        updateActiveCategory();
        loadBooks();
    });
});

searchForm.addEventListener("submit", (event) => {
    event.preventDefault();
    closeSuggestions();
    loadBooks();
});

genreSelect.addEventListener("change", () => {
    updateActiveCategory();
    loadBooks();
});

updateActiveCategory();
loadBooks();
