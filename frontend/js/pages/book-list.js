// Figma 책 목록 UI: 검색, 장르 필터, 카드 렌더링을 페이지 구조에 맞게 연결합니다.
const searchForm = document.querySelector("#book-search-form");
const keywordInput = document.querySelector("#book-keyword");
const sortSelect = document.querySelector("#book-sort");
const statusElement = document.querySelector("#book-status");
const listElement = document.querySelector("#book-list");
const suggestionsElement = document.querySelector("#search-suggestions");
const categoryButtons = document.querySelectorAll(".book-category-tabs [data-genre]");
const authorFilterHeading = document.querySelector("#author-filter-heading");
const selectedAuthorNameElement = document.querySelector("#selected-author-name");
const searchFilterHeading = document.querySelector("#search-filter-heading");
const selectedSearchKeywordElement = document.querySelector("#selected-search-keyword");
const pageParams = new URLSearchParams(window.location.search);
let selectedAuthorId = pageParams.get("authorId");
let selectedAuthorName = pageParams.get("authorName");
const suggestionCache = new Map();
let suggestionTimer;
let suggestionRequest;
let selectedGenre = "";

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

keywordInput.addEventListener("focus", () => {
    const keyword = keywordInput.value.trim();
    if (keyword) loadSuggestions(keyword);
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
    const keyword = keywordInput.value.trim();

    suggestions.forEach((suggestion) => {
        const option = document.createElement("button");
        option.type = "button";
        option.setAttribute("role", "option");

        const type = document.createElement("span");
        type.className = `suggestion-type ${suggestion.type === "BOOK" ? "is-book" : "is-author"}`;
        type.textContent = suggestion.type === "BOOK" ? "책" : "작가";

        const name = document.createElement("strong");
        appendHighlightedText(name, suggestion.name, keyword);

        const detail = document.createElement("small");
        appendHighlightedText(detail, suggestion.detail, keyword);

        option.append(type, name);
        if (suggestion.type === "BOOK") option.append(detail);
        option.addEventListener("click", () => {
            if (suggestion.type === "AUTHOR") {
                window.location.href = `/pages/book/list.html?authorId=${encodeURIComponent(suggestion.id)}&authorName=${encodeURIComponent(suggestion.name)}`;
                return;
            }
            window.location.href = `/pages/book/detail.html?id=${encodeURIComponent(suggestion.id)}`;
        });
        suggestionsElement.append(option);
    });

    const hasSuggestions = suggestions.length > 0;
    suggestionsElement.hidden = !hasSuggestions;
    keywordInput.setAttribute("aria-expanded", String(hasSuggestions));
}

function closeSuggestions() {
    suggestionsElement.hidden = true;
    suggestionsElement.replaceChildren();
    keywordInput.setAttribute("aria-expanded", "false");
}

function updateActiveCategory() {
    categoryButtons.forEach((button) => {
        const isActive = button.dataset.genre === selectedGenre;
        button.classList.toggle("is-active", isActive);
        button.setAttribute("aria-pressed", String(isActive));
    });
}

function showSearchResults(keyword) {
    authorFilterHeading.hidden = true;
    searchFilterHeading.hidden = false;
    selectedSearchKeywordElement.textContent = `'${keyword}'`;
}

async function loadBooks() {
    const params = new URLSearchParams();
    if (keywordInput.value.trim()) params.set("keyword", keywordInput.value.trim());
    if (selectedGenre) params.set("genre", selectedGenre);
    if (selectedAuthorId) params.set("authorId", selectedAuthorId);
    params.set("sort", sortSelect.value);
    params.set("page", 1);
    params.set("size", 100);

    statusElement.textContent = "책 목록을 불러오는 중입니다.";
    listElement.replaceChildren();

    try {
        const query = params.toString();
        const response = await fetch(`/api/books${query ? `?${query}` : ""}`);
        const result = await response.json();
        if (!response.ok || !result.success) throw new Error(result.message);
        const books = selectedAuthorId
            ? result.data.books.filter((book) => String(book.authorId) === String(selectedAuthorId))
            : result.data.books;
        renderBooks(books);
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

    books.forEach((book) => {
        const detailUrl = `/pages/book/detail.html?id=${encodeURIComponent(book.bookId)}`;
        const card = document.createElement("article");
        card.className = "book-card";

        const coverLink = document.createElement("a");
        coverLink.className = "book-cover-link";
        coverLink.href = detailUrl;
        coverLink.setAttribute("aria-label", `${book.title} 상세 보기`);

        const cover = document.createElement("figure");
        cover.className = "book-card-cover";

        const coverGenre = document.createElement("span");
        coverGenre.className = "book-cover-genre";
        coverGenre.textContent = book.genre || "도서";
        coverGenre.dataset.genre = book.genre || "도서";

        const image = document.createElement("img");
        image.src = book.imageUrl || "";
        image.alt = `${book.title} 표지`;
        image.loading = "lazy";
        image.decoding = "async";
        image.addEventListener("error", () => {
            cover.textContent = book.title;
            cover.prepend(coverGenre);
            image.remove();
        }, {once: true});

        cover.append(coverGenre, image);

        const content = document.createElement("div");
        content.className = "book-card-content";

        const heading = document.createElement("h2");
        heading.className = "book-title";

        const titleLink = document.createElement("a");
        titleLink.href = detailUrl;
        appendHighlightedText(titleLink, book.title, keywordInput.value.trim());
        titleLink.title = book.title;
        heading.append(titleLink);

        const author = document.createElement("a");
        author.className = "book-author";
        author.href = `/pages/book/list.html?authorId=${encodeURIComponent(book.authorId)}&authorName=${encodeURIComponent(book.authorName)}`;
        appendHighlightedText(author, book.authorName, keywordInput.value.trim());
        author.title = `${book.authorName}의 책 보기`;

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
        content.append(heading, author, rating);
        coverLink.append(cover);
        card.append(coverLink, content);
        listElement.append(card);
    });

    statusElement.textContent = `현재 ${listElement.childElementCount}권을 표시하고 있습니다.`;
}

categoryButtons.forEach((button) => {
    button.addEventListener("click", () => {
        selectedGenre = button.dataset.genre;
        updateActiveCategory();
        loadBooks();
    });
});

searchForm.addEventListener("submit", (event) => {
    event.preventDefault();
    closeSuggestions();
    const keyword = keywordInput.value.trim();
    if (keyword) showSearchResults(keyword);
    loadBooks();
});

sortSelect.addEventListener("change", () => {
    loadBooks();
});

document.querySelector("#book-reset-button")?.addEventListener("click", () => {
    selectedAuthorId = null;
    selectedAuthorName = null;
    selectedGenre = "";
    keywordInput.value = "";
    sortSelect.value = "rating";
    authorFilterHeading.hidden = true;
    searchFilterHeading.hidden = true;
    closeSuggestions();
    updateActiveCategory();
    window.history.replaceState({}, "", "/pages/book/list.html");
    loadBooks();
});

if (selectedAuthorId) {
    authorFilterHeading.hidden = false;
    selectedAuthorNameElement.textContent = selectedAuthorName || "선택한 작가";
} else {
    updateActiveCategory();
}
loadBooks();

function appendHighlightedText(element, text, keyword) {
    if (!keyword) {
        element.textContent = text;
        return;
    }

    const source = String(text || "");
    const lowerSource = source.toLocaleLowerCase("ko");
    const lowerKeyword = keyword.toLocaleLowerCase("ko");
    let cursor = 0;
    let matchIndex = lowerSource.indexOf(lowerKeyword, cursor);

    while (matchIndex !== -1) {
        element.append(document.createTextNode(source.slice(cursor, matchIndex)));
        const mark = document.createElement("mark");
        mark.className = "book-search-highlight";
        mark.textContent = source.slice(matchIndex, matchIndex + keyword.length);
        element.append(mark);
        cursor = matchIndex + keyword.length;
        matchIndex = lowerSource.indexOf(lowerKeyword, cursor);
    }
    element.append(document.createTextNode(source.slice(cursor)));
}
