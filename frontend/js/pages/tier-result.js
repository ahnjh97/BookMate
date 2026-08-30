const tierListId = Number(new URLSearchParams(window.location.search).get("id"));
const statusElement = document.querySelector("#result-status");
const boardElement = document.querySelector("#result-board");
const grades = ["S", "A", "B", "C", "D"];

loadTierResult();

async function loadTierResult() {
  if (!Number.isInteger(tierListId) || tierListId <= 0) {
    statusElement.textContent = "티어리스트 번호가 올바르지 않습니다.";
    return;
  }

  try {
    const response = await fetch(`/api/tier-lists?tierListId=${tierListId}`, { cache: "no-store" });
    const result = await response.json();
    if (!response.ok || !result.success) throw new Error(result.message);
    renderTierResult(result.tierList);
    statusElement.hidden = true;
    boardElement.hidden = false;
  } catch (error) {
    statusElement.textContent = error.message || "티어리스트를 불러오지 못했습니다.";
  }
}

function renderTierResult(tierList) {
  document.title = `${tierList.title} | BookMate`;
  document.querySelector("#result-title").textContent = tierList.title;
  document.querySelector("#result-description").textContent = tierList.templateDescription || "";
  document.querySelector("#result-owner").textContent = `${tierList.memberNickname}님의 티어리스트`;
  boardElement.replaceChildren();

  grades.forEach((grade) => {
    const row = document.createElement("div");
    row.className = "tier-row";
    row.innerHTML = `<div class="tier-label">${grade}</div>`;
    const zone = document.createElement("div");
    zone.className = "tier-dropzone";
    tierList.items
      .filter((item) => item.grade === grade)
      .forEach((item) => zone.append(createBook(item)));
    row.append(zone);
    boardElement.append(row);
  });
}

function createBook(book) {
  const link = document.createElement("a");
  link.className = "tier-book";
  link.href = `/pages/book/detail.html?id=${book.bookId}`;
  link.dataset.tooltip = `${book.title} - ${book.authorName}`;
  link.draggable = false;
  link.innerHTML = `
    <img src="${escapeHtml(book.imageUrl || "")}" alt="${escapeHtml(book.title)}" draggable="false">
    <span>${escapeHtml(book.title)}</span>`;
  return link;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
