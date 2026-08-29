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
let books = [], activeKeyword = "";
const selected = new Set();
async function loadBooks() {
  try {
    const response = await fetch("/api/books?size=100&sort=title"), data = await response.json();
    books = data.data?.books || [];
    updateTypeFilter();
  } catch (error) {
    message.textContent = "책 목록을 불러오지 못했습니다.";
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
  authorError.hidden = true;
  authorFilter.removeAttribute("aria-invalid");
  if (type === "장르") {
    filterLabel.firstChild.textContent = "장르 선택";
    [...new Set(books.map(book => book.genre))].sort().forEach(value => filter.add(new Option(value, value)));
  } else if (type === "작가") filterLabel.firstChild.textContent = "작가 선택";
  renderBooks();
}
function renderAuthorOptions() {
  const query = authorFilter.value.trim().toLocaleLowerCase("ko"),
    names = [...new Set(books.map(book => book.authorName).filter(Boolean))].filter(name =>
      name.toLocaleLowerCase("ko").startsWith(query)
    ).sort((a, b) => a.localeCompare(b, "ko"));
  authorOptions.replaceChildren();
  names.forEach(name => {
    const option = document.createElement("button");
    option.type = "button";
    option.setAttribute("role", "option");
    option.textContent = name;
    option.addEventListener("mousedown", event => {
      event.preventDefault();
      authorFilter.value = name;
      validateAuthor();
      closeAuthorOptions();
      renderBooks();
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
  const value = authorFilter.value.trim(),
    isValid = [...new Set(books.map(book => book.authorName).filter(Boolean))].some(name => name === value);
  authorError.hidden = isValid;
  authorFilter.toggleAttribute("aria-invalid", !isValid);
  return isValid;
}
function filteredBooks() {
  const q = activeKeyword.toLowerCase(), authorQuery = authorFilter.value.trim().toLocaleLowerCase("ko");
  return books.filter(book => {
    const typeMatch = category.value === "자유"
      || (category.value === "장르"
        ? book.genre === filter.value
        : book.authorName.toLocaleLowerCase("ko").startsWith(authorQuery));
    return typeMatch && (!q || book.title.toLowerCase().includes(q));
  });
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
      event.target.checked ? selected.add(book.bookId) : selected.delete(book.bookId);
      updateCount();
    });
    picker.append(label);
  });
  if (!picker.children.length) picker.innerHTML = "<p class=\"tier-status\">조건에 맞는 책이 없습니다.</p>";
}
function showSuggestions() {
  const q = search.value.trim().toLowerCase();
  suggestions.replaceChildren();
  if (!q) {
    closeSuggestions();
    return;
  }
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
function applySearch() {
  activeKeyword = search.value.trim();
  closeSuggestions();
  renderBooks();
}
function updateCount() {
  document.getElementById("selection-count").textContent = `선택된 책 수 : ${selected.size}권`;
}
category.addEventListener("change", updateTypeFilter);
filter.addEventListener("change", renderBooks);
authorFilter.addEventListener("focus", renderAuthorOptions);
authorFilter.addEventListener("click", renderAuthorOptions);
authorFilter.addEventListener("input", () => {
  validateAuthor();
  renderAuthorOptions();
  renderBooks();
});
authorFilter.addEventListener("blur", validateAuthor);
authorFilter.addEventListener("keydown", event => {
  if (event.key === "Escape") closeAuthorOptions();
});
search.addEventListener("input", showSuggestions);
search.addEventListener("keydown", event => {
  if (event.key === "Enter") {
    event.preventDefault();
    applySearch();
  }
  if (event.key === "Escape") closeSuggestions();
});
document.getElementById("book-search-button").addEventListener("click", applySearch);
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
  if (!validateAuthor()) {
    authorFilter.focus();
    renderAuthorOptions();
    return;
  }
  if (selected.size < 3) {
    message.textContent = "책을 3권 이상 선택해 주세요.";
    return;
  }
  const button = event.currentTarget.querySelector("button[type=submit]");
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
    message.textContent = data.message;
    event.currentTarget.reset();
    selected.clear();
    activeKeyword = "";
    updateCount();
    updateTypeFilter();
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
