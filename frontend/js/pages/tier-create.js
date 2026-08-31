const picker = document.getElementById("book-picker"),
  search = document.getElementById("book-search"),
  message = document.getElementById("form-message"),
  category = document.getElementById("template-category"),
  filter = document.getElementById("template-filter"),
  authorFilter = document.getElementById("template-author-filter"),
  authorOptions = document.getElementById("template-author-options"),
  authorError = document.getElementById("template-author-error"),
  filterLabel = document.getElementById("template-filter-label"),
  suggestions = document.getElementById("tier-search-suggestions");
let books = [], activeKeyword = "", searchSequence = 0, searchTimer;
let currentPage = 1, hasMoreBooks = false, loadingMoreBooks = false;
let selectedAuthorId = null;
const selected = new Set();
async function loadBooks(keyword = "", append = false) {
  if (append && (loadingMoreBooks || !hasMoreBooks)) return;
  if (append) loadingMoreBooks = true;
  const sequence = ++searchSequence;
  try {
    const params = new URLSearchParams({ size: "40", sort: "title" });
    if (keyword) params.set("keyword", keyword);
    if (category.value === "장르" && filter.value) params.set("genre", filter.value);
    if (category.value === "작가" && selectedAuthorId) params.set("authorId", selectedAuthorId);
    params.set("page", append ? String(currentPage + 1) : "1");
    const response = await fetch(`/api/books?${params}`), data = await response.json();
    if (!response.ok || !data.success) throw new Error(data.message);
    if (sequence !== searchSequence) return;
    const fetchedBooks = data.data?.books || [];
    books = append
      ? [...new Map([...books, ...fetchedBooks].map(book => [book.bookId, book])).values()]
      : fetchedBooks;
    currentPage = append ? currentPage + 1 : 1;
    hasMoreBooks = Boolean(data.data?.hasMore);
    if (append) {
      const previousScrollTop = picker.scrollTop;
      renderBooks();
      picker.scrollTop = previousScrollTop;
    } else renderBooks();
  } catch (error) {
    if (sequence !== searchSequence) return;
    message.textContent = "책 목록을 불러오지 못했습니다.";
  } finally {
    if (append) loadingMoreBooks = false;
  }
}
function updateTypeFilter() {
  const type = category.value;
  filter.replaceChildren();
  closeAuthorOptions();
  filterLabel.hidden = type === "자유";
  filter.hidden = type !== "장르";
  authorFilter.hidden = type !== "작가";
  authorFilter.value = "";
  selectedAuthorId = null;
  authorError.hidden = true;
  authorFilter.removeAttribute("aria-invalid");
  if (type === "장르") {
    filterLabel.firstChild.textContent = "장르 선택";
    ["소설", "판타지", "SF", "추리", "인문/사회", "자기계발", "과학/IT"]
      .forEach(value => filter.add(new Option(value, value)));
  } else if (type === "작가") filterLabel.firstChild.textContent = "작가 선택";
  loadBooks(activeKeyword);
}
async function renderAuthorOptions() {
  const query = authorFilter.value.trim();
  authorOptions.replaceChildren();
  if (!query) {
    closeAuthorOptions();
    return;
  }
  let authors;
  try {
    const response = await fetch(`/api/search/suggestions?q=${encodeURIComponent(query)}`);
    const data = await response.json();
    if (!response.ok || !data.success || authorFilter.value.trim() !== query) return;
    authors = data.data.filter(item => item.type === "AUTHOR");
  } catch (error) {
    closeAuthorOptions();
    return;
  }
  authors.forEach(author => {
    const option = document.createElement("button");
    option.type = "button";
    option.setAttribute("role", "option");
    option.textContent = author.name;
    option.addEventListener("mousedown", event => {
      event.preventDefault();
      authorFilter.value = author.name;
      selectedAuthorId = author.id;
      authorError.hidden = true;
      authorFilter.removeAttribute("aria-invalid");
      closeAuthorOptions();
      loadBooks(activeKeyword);
    });
    authorOptions.append(option);
  });
  authorOptions.hidden = !names.length;
  authorFilter.setAttribute("aria-expanded", String(!authorOptions.hidden));
}
function closeAuthorOptions() {
  authorOptions.hidden = true;
  authorOptions.replaceChildren();
  authorFilter.setAttribute("aria-expanded", "false");
}
function validateAuthor() {
  if (category.value !== "작가") {
    authorError.hidden = true;
    authorFilter.removeAttribute("aria-invalid");
    return true;
  }
  const isValid = Boolean(selectedAuthorId && authorFilter.value.trim());
  authorError.hidden = isValid;
  authorFilter.toggleAttribute("aria-invalid", !isValid);
  return isValid;
}
function filteredBooks() {
  return books;
}
function renderBooks() {
  picker.replaceChildren();
  filteredBooks().forEach(book => {
    const label = document.createElement("label");
    label.className = "book-option";
    label.innerHTML = `${
      book.imageUrl ? `<img src="${escapeHtml(book.imageUrl)}" alt="" loading="lazy" decoding="async">` : "<span></span>"
    }<span><strong>${escapeHtml(book.title)}</strong><small>${
      escapeHtml(book.authorName)
    }</small></span><input type="checkbox" value="${book.bookId}" ${selected.has(book.bookId) ? "checked" : ""}>`;
    label.querySelector("input").addEventListener("change", event => {
      if (event.target.checked && selected.size >= 100) {
        event.target.checked = false;
        message.textContent = "책은 최대 100권까지 선택할 수 있습니다.";
        return;
      }
      event.target.checked ? selected.add(book.bookId) : selected.delete(book.bookId);
      message.textContent = "";
      updateCount();
    });
    picker.append(label);
  });
  if (!picker.children.length) picker.innerHTML = "<p class=\"tier-status\">조건에 맞는 책이 없습니다.</p>";
}
async function showSuggestions() {
  const q = search.value.trim().toLowerCase();
  suggestions.replaceChildren();
  if (!q) {
    closeSuggestions();
    await loadBooks();
    return;
  }
  await loadBooks(q);
  books.filter(book => book.title.toLowerCase().includes(q)).slice(0, 8).forEach(book => {
    const button = document.createElement("button");
    button.type = "button";
    button.innerHTML = `<span class="suggestion-type is-book">책</span><strong>${
      escapeHtml(book.title)
    }</strong><small>${escapeHtml(book.authorName)}</small>`;
    button.addEventListener("click", () => {
      search.value = book.title;
      activeKeyword = book.title;
      closeSuggestions();
      renderBooks();
    });
    suggestions.append(button);
  });
  suggestions.hidden = !suggestions.children.length;
  search.setAttribute("aria-expanded", String(!suggestions.hidden));
}
function closeSuggestions() {
  suggestions.hidden = true;
  suggestions.replaceChildren();
  search.setAttribute("aria-expanded", "false");
}
async function applySearch() {
  activeKeyword = search.value.trim();
  closeSuggestions();
  await loadBooks(activeKeyword);
}
function updateCount() {
  document.getElementById("selection-count").textContent = `선택된 책 수 : ${selected.size}권`;
}
category.addEventListener("change", updateTypeFilter);
filter.addEventListener("change", () => loadBooks(activeKeyword));
authorFilter.addEventListener("focus", renderAuthorOptions);
authorFilter.addEventListener("click", renderAuthorOptions);
authorFilter.addEventListener("input", () => {
  selectedAuthorId = null;
  renderAuthorOptions();
});
authorFilter.addEventListener("blur", validateAuthor);
authorFilter.addEventListener("keydown", event => {
  if (event.key === "Escape") closeAuthorOptions();
});
search.addEventListener("input", () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(showSuggestions, 250);
});
search.addEventListener("keydown", event => {
  if (event.key === "Enter") {
    event.preventDefault();
    applySearch();
  }
  if (event.key === "Escape") closeSuggestions();
});
document.getElementById("book-search-button").addEventListener("click", applySearch);
picker.addEventListener("scroll", () => {
  if (picker.scrollTop + picker.clientHeight >= picker.scrollHeight - 80) loadBooks(activeKeyword, true);
});
document.getElementById("selection-reset-button").addEventListener("click", () => {
  selected.clear();
  updateCount();
  renderBooks();
});
document.addEventListener("click", event => {
  if (!event.target.closest(".author-combobox")) closeAuthorOptions();
  if (!event.target.closest(".tier-book-search")) closeSuggestions();
});
document.getElementById("template-form").addEventListener("submit", async event => {
  event.preventDefault();
  const form = event.currentTarget;
  if (!validateAuthor()) {
    authorFilter.focus();
    renderAuthorOptions();
    return;
  }
  if (selected.size < 3) {
    message.textContent = "책을 3권 이상 선택해 주세요.";
    return;
  }
  const button = form.querySelector("button[type=submit]");
  button.disabled = true;
  try {
    const response = await fetch("/api/tier-templates", {
        method: "POST",
        headers: { "Content-Type": "application/json;charset=UTF-8" },
        credentials: "include",
        body: JSON.stringify({
          title: document.getElementById("template-title").value,
          category: category.value,
          description: document.getElementById("template-description").value,
          bookIds: [...selected],
        }),
      }),
      data = await response.json();
    if (response.status === 401) {
      alert("로그인 후 신청할 수 있습니다.");
      location.href = "/pages/auth/login.html";
      return;
    }
    if (!response.ok) throw new Error(data.message);
    sessionStorage.setItem("bookmate:flash-toast", JSON.stringify({
      state: "success",
      message: "새 티어리스트 신청이 완료되었습니다. 관리자 승인 후 등록됩니다.",
    }));
    location.href = "/pages/tier/list.html";
  } catch (error) {
    message.textContent = error.message || "신청하지 못했습니다.";
  } finally {
    button.disabled = false;
  }
});
function escapeHtml(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
    "\"",
    "&quot;",
  );
}
loadBooks();
