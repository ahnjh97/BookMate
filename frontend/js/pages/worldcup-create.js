const picker = document.querySelector("#worldcup-book-picker");
const searchInput = document.querySelector("#worldcup-book-search");
const message = document.querySelector("#worldcup-form-message");
const category = document.querySelector("#worldcup-template-category");
const filter = document.querySelector("#worldcup-template-filter");
const filterLabel = document.querySelector("#worldcup-filter-label");
const selectionCount = document.querySelector("#worldcup-selection-count");
const selected = new Set();
let books = [];
let activeKeyword = "";
let searchSequence = 0;
let currentPage = 1;
let hasMoreBooks = false;
let loadingMoreBooks = false;

async function loadBooks(keyword = "", append = false) {
  if (append && (loadingMoreBooks || !hasMoreBooks)) return;
  if (append) loadingMoreBooks = true;
  const sequence = ++searchSequence;
  try {
    const params = new URLSearchParams({ size: "40", sort: "title" });
    if (keyword) params.set("keyword", keyword);
    if (category.value === "장르" && filter.value) params.set("genre", filter.value);
    params.set("page", append ? String(currentPage + 1) : "1");
    const response = await fetch(`/api/books?${params}`);
    const data = await response.json();
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
    console.error(error);
    message.textContent = "책 목록을 불러오지 못했습니다.";
  } finally {
    if (append) loadingMoreBooks = false;
  }
}

function updateTypeFilter() {
  filter.replaceChildren();
  filterLabel.hidden = category.value !== "장르";
  if (category.value === "장르") {
    ["소설", "판타지", "SF", "추리", "인문/사회", "자기계발", "과학/IT"]
      .forEach(genre => filter.add(new Option(genre, genre)));
  }
  loadBooks(activeKeyword);
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
      book.imageUrl
        ? `<img src="${escapeHtml(book.imageUrl)}" alt="" loading="lazy" decoding="async">`
        : "<span></span>"
    }<span><strong>${escapeHtml(book.title)}</strong><small>${
      escapeHtml(book.authorName)
    }</small></span><input type="checkbox" value="${book.bookId}" ${
      selected.has(book.bookId) ? "checked" : ""
    }>`;
    label.querySelector("input").addEventListener("change", event => {
      if (event.target.checked && selected.size >= 64) {
        event.target.checked = false;
        message.textContent = "책은 최대 64권까지 선택할 수 있습니다.";
        return;
      }
      event.target.checked ? selected.add(book.bookId) : selected.delete(book.bookId);
      message.textContent = "";
      updateCount();
    });
    picker.append(label);
  });
}

async function applySearch() {
  activeKeyword = searchInput.value.trim();
  await loadBooks(activeKeyword);
}

function updateCount() {
  selectionCount.textContent = `선택된 책 수 : ${selected.size}권`;
}

category.addEventListener("change", updateTypeFilter);
filter.addEventListener("change", () => loadBooks(activeKeyword));
searchInput.addEventListener("keydown", event => {
  if (event.key !== "Enter") return;
  event.preventDefault();
  applySearch();
});
searchInput.addEventListener("input", () => {
  if (searchInput.value.trim()) return;
  activeKeyword = "";
  loadBooks();
});
document.querySelector("#worldcup-book-search-button").addEventListener("click", applySearch);
picker.addEventListener("scroll", () => {
  if (picker.scrollTop + picker.clientHeight >= picker.scrollHeight - 80) loadBooks(activeKeyword, true);
});
document.querySelector("#worldcup-selection-reset-button").addEventListener("click", () => {
  selected.clear();
  message.textContent = "";
  updateCount();
  renderBooks();
});

document.querySelector("#worldcup-template-form").addEventListener("submit", async event => {
  event.preventDefault();
  const form = event.currentTarget;
  if (selected.size < 16) {
    message.textContent = "책을 16권 이상 선택해 주세요.";
    return;
  }
  const button = form.querySelector("button[type=submit]");
  button.disabled = true;
  try {
    const response = await fetch("/api/worldcup/templates", {
      method: "POST",
      headers: {"Content-Type": "application/json;charset=UTF-8"},
      credentials: "include",
      body: JSON.stringify({
        title: document.querySelector("#worldcup-template-title").value,
        description: document.querySelector("#worldcup-template-description").value,
        category: category.value,
        bookIds: [...selected],
      }),
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.message);
    sessionStorage.setItem("bookmate:flash-toast", JSON.stringify({
      state: "success",
      message: "새 이상형월드컵 신청이 완료되었습니다. 관리자 승인 후 등록됩니다.",
    }));
    location.href = "/pages/worldcup/list.html";
  } catch (error) {
    message.textContent = error.message || "템플릿을 만들지 못했습니다.";
  } finally {
    button.disabled = false;
  }
});

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");
}

loadBooks();
