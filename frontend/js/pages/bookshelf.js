const statusElement = document.querySelector("#bookshelf-status");
const contentElement = document.querySelector("#bookshelf-content");
let viewingOwnBookshelf = false;

window.BookMateBookshelf = { renderActivity: renderBookshelfActivity };

if (statusElement && contentElement) initializeBookshelf();

async function initializeBookshelf() {
  try {
    const requestedValue = new URLSearchParams(window.location.search).get("memberId");
    const authResponse = await fetch("/api/auth", { cache: "no-store" });
    const auth = authResponse.ok ? await authResponse.json() : { loggedIn: false };
    let memberId = Number(requestedValue);

    if (!requestedValue) {
      if (!auth.loggedIn) {
        window.location.replace("/pages/auth/login.html");
        return;
      }
      memberId = Number(auth.memberId);
      viewingOwnBookshelf = true;
    } else {
      if (!Number.isInteger(memberId) || memberId <= 0) {
        throw new Error("방문할 회원을 찾을 수 없습니다.");
      }
      viewingOwnBookshelf = auth.loggedIn && Number(auth.memberId) === memberId;
    }

    const response = await fetch(`/api/members/bookshelf?memberId=${memberId}`, { cache: "no-store" });
    const result = await response.json();
    if (!response.ok || !result.success) throw new Error(result.message);
    renderBookshelf(result.bookshelf);
    document.querySelector(".bookshelf-back").hidden = viewingOwnBookshelf;
    if (viewingOwnBookshelf) {
      const similarSection = document.querySelector("#similar-members");
      similarSection.hidden = false;
      await window.BookMateMemberPage?.loadSimilarMembers();
    } else {
      await loadViewerSimilarity(memberId);
    }
    statusElement.hidden = true;
    contentElement.hidden = false;
  } catch (error) {
    statusElement.dataset.state = "error";
    statusElement.textContent = error.message || "회원 책장을 불러오지 못했습니다.";
  }
}

async function loadViewerSimilarity(targetMemberId) {
  const section = document.querySelector("#bookshelf-similarity");
  const container = document.querySelector("#bookshelf-similarity-content");
  if (!section || !container) return;

  try {
    const response = await fetch(`/api/preferences/similar?memberId=${targetMemberId}`, {
      credentials: "include",
      cache: "no-store",
    });
    if (response.status === 401) return;
    const result = await response.json();
    if (!response.ok || !result.success) throw new Error(result.message);
    section.hidden = false;
    if (!result.user) {
      container.innerHTML = emptyMessage("두 회원이 함께 참여한 취향 데이터가 아직 부족합니다.");
      return;
    }
    container.innerHTML = similarityMarkup(result.user);
  } catch (error) {
    section.hidden = false;
    container.innerHTML = emptyMessage(error.message || "취향 일치율을 불러오지 못했습니다.");
  }
}

function similarityMarkup(user) {
  const components = [
    ["평점", user.ratingSimilarity],
    ["티어리스트", user.tierSimilarity],
    ["이상형월드컵", user.worldcupSimilarity],
  ];
  return `
    <div class="bookshelf-similarity-card">
      <div class="bookshelf-similarity-total">
        <span>종합 취향일치율</span>
        <strong>${formatSimilarity(user.similarityScore)}%</strong>
      </div>
      <dl class="bookshelf-similarity-components">
        ${components.map(([label, score]) => `
          <div><dt>${label}</dt><dd>${score == null ? "데이터 부족" : `${formatSimilarity(score)}%`}</dd></div>
        `).join("")}
      </dl>
      <p>
        공통 평점 ${Number(user.commonRatingCount || 0)}권 ·
        티어 도서 ${Number(user.commonTierBookCount || 0)}권 ·
        월드컵 도서 ${Number(user.commonWorldcupBookCount || 0)}권
      </p>
    </div>`;
}

function formatSimilarity(value) {
  return Number(value || 0).toFixed(1);
}

function renderBookshelf(bookshelf) {
  const nickname = bookshelf.nickname || "회원";
  document.title = `${nickname}님의 책장 | BookMate`;
  const nicknameElement = document.querySelector("#bookshelf-nickname");
  nicknameElement.textContent = nickname;
  nicknameElement.title = nickname;
  renderBookshelfActivity(document, bookshelf);
}

function renderBookshelfActivity(root, bookshelf) {
  renderCounts(root, bookshelf.counts || {});
  renderBooks(root, bookshelf.favoriteBooks || [], bookshelf.counts?.ratingAverage || 0);
  renderTierLists(root, bookshelf.tierLists || []);
  renderWorldcups(root, bookshelf.worldcupResults || []);
}

function renderCounts(root, counts) {
  const container = root.querySelector("[data-bookshelf-counts]");
  if (!container) return;
  const values = [
    ["평가한 책", counts.ratings || 0, "권"],
    ["참여한 티어리스트", counts.tierLists || 0, "개"],
    ["참여한 이상형월드컵", counts.worldcups || 0, "개"],
  ];
  container.innerHTML = values.map(([label, value, unit]) => `
    <div><span>${label}</span><strong>${Number(value).toLocaleString()}${unit}</strong></div>
  `).join("");
}

function renderBooks(root, books, ratingAverage) {
  const container = root.querySelector("[data-bookshelf-books]");
  if (!container) return;
  const filterButtons = root.querySelectorAll("[data-rating-filter]");
  const countElement = root.querySelector("[data-rating-count]");
  const sortedBooks = [...books].sort((left, right) => {
    const scoreDifference = Number(right.score) - Number(left.score);
    if (scoreDifference !== 0) return scoreDifference;
    return String(left.title || "").localeCompare(String(right.title || ""), "ko");
  });

  function applyRatingFilter(selectedValue = "all") {
    const filteredBooks = selectedValue === "all"
      ? sortedBooks
      : sortedBooks.filter(book => Number(book.score) === Number(selectedValue));
    const emptyText = selectedValue === "all"
      ? "후기를 남긴 책이 아직 없습니다."
      : `${selectedValue}점을 준 책이 없습니다.`;
    if (countElement) {
      const averageLabel = `평균 ${Number(ratingAverage).toFixed(2)}`;
      countElement.textContent = selectedValue === "all"
        ? `${filteredBooks.length.toLocaleString()}권 | ${averageLabel}`
        : `${filteredBooks.length.toLocaleString()}권 | 전체 ${averageLabel}`;
    }

    renderPaginated(container, filteredBooks, 10, "후기 도서", (pageBooks) => pageBooks.map((book) => `
      <a class="bookshelf-book" href="/pages/book/detail.html?id=${book.bookId}">
        <img src="${escapeHtml(book.imageUrl || "")}" alt="${escapeHtml(book.title)} 표지" loading="lazy">
        <strong>${escapeHtml(book.title)}</strong>
        <span>${escapeHtml(book.authorName)}</span>
        <small>★ ${Number(book.score).toFixed(1)}</small>
      </a>
    `).join(""), emptyText, "bookshelf-books-page");
  }

  filterButtons.forEach(button => {
    button.addEventListener("click", () => {
      filterButtons.forEach(item => {
        const active = item === button;
        item.classList.toggle("is-active", active);
        item.setAttribute("aria-pressed", String(active));
      });
      applyRatingFilter(button.dataset.ratingFilter);
    });
  });
  applyRatingFilter();
}

function renderTierLists(root, lists) {
  const container = root.querySelector("[data-bookshelf-tiers]");
  if (!container) return;
  renderPaginated(container, lists, 3, "참여한 티어리스트", (pageLists) => pageLists.map((list) => `
    <a class="bookshelf-result-card" href="${
      viewingOwnBookshelf
        ? `/pages/tier/maker.html?id=${list.templateId}`
        : `/pages/tier/result.html?id=${list.tierListId}`
    }">
      <span>티어리스트</span>
      <strong>${escapeHtml(list.title || list.templateTitle)}</strong>
      <small>${escapeHtml(list.templateTitle)}</small>
    </a>
  `).join(""), "참여한 티어리스트가 없습니다.", "bookshelf-result-page");
}

function renderWorldcups(root, results) {
  const container = root.querySelector("[data-bookshelf-worldcups]");
  if (!container) return;
  renderPaginated(container, results, 3, "참여한 이상형월드컵", (pageResults) => pageResults.map((result) => `
    <a class="bookshelf-result-card bookshelf-worldcup-card" href="/pages/worldcup/result.html?id=${result.runId}">
      <img src="${escapeHtml(result.winnerImageUrl || "")}" alt="" loading="lazy">
      <span>최종 우승</span>
      <strong>${escapeHtml(result.winnerTitle)}</strong>
      <small>${escapeHtml(result.templateTitle)}</small>
    </a>
  `).join(""), "참여한 이상형월드컵이 없습니다.", "bookshelf-result-page");
}

function renderPaginated(container, items, pageSize, label, renderPage, emptyText, pageClass) {
  if (!items.length) {
    container.innerHTML = emptyMessage(emptyText);
    return;
  }

  container.innerHTML = `
    <div class="${pageClass}" data-page-items></div>
    <nav class="list-pagination bookshelf-list-pagination" aria-label="${label} 페이지 이동">
      <button class="button button-outline" type="button" data-pagination-prev>이전</button>
      <div class="list-page-numbers" data-pagination-numbers aria-live="polite"></div>
      <button class="button button-outline" type="button" data-pagination-next>다음</button>
    </nav>`;

  const pageItems = container.querySelector("[data-page-items]");
  const pagination = window.BookMateListPagination.create({
    root: container.querySelector(".bookshelf-list-pagination"),
    pageSize,
    onRender: pageItemsList => {
      pageItems.innerHTML = renderPage(pageItemsList);
    },
  });
  pagination.setItems(items);
}

function emptyMessage(message) {
  return `<p class="bookshelf-empty">${escapeHtml(message)}</p>`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
