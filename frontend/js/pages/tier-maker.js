const params = new URLSearchParams(location.search),
  templateId = Number(params.get("id")),
  board = document.getElementById("tier-board"),
  pool = document.getElementById("book-pool"),
  message = document.getElementById("maker-message"),
  grades = ["S", "A", "B", "C", "D"];
let template = null, dragged = null, placeholder = null;
async function init() {
  if (!templateId) {
    message.textContent = "템플릿 번호가 필요합니다.";
    return;
  }
  try {
    const templateResponse = await fetch(`/api/tier-templates?id=${templateId}`),
      data = await templateResponse.json();
    if (!templateResponse.ok) throw new Error(data.message);
    template = data.template;
    document.getElementById("maker-title").textContent = template.title;
    document.getElementById("maker-description").textContent = template.description
      || `${template.creatorNickname}님이 만든 템플릿`;
    document.getElementById("stats-button").href = `/pages/tier/stats.html?id=${templateId}`;
    buildBoard();
    resetBooks();
    await restoreSavedTierList();
  } catch (error) {
    message.textContent = error.message;
  }
}
async function restoreSavedTierList() {
  const response = await fetch(`/api/tier-lists?templateId=${templateId}`, { credentials: "include" }),
    contentType = response.headers.get("content-type") || "";
  if (!contentType.includes("application/json")) {
    throw new Error(
      response.status === 405
        ? "백엔드 서버를 다시 시작한 뒤 새로고침해 주세요."
        : "저장된 티어리스트 응답 형식이 올바르지 않습니다.",
    );
  }
  const data = await response.json();
  if (!response.ok) throw new Error(data.message);
  const saved = data.tierList;
  if (!saved) return;
  document.getElementById("list-description").value = saved.description || "";
  document.getElementById("list-community").checked = Boolean(saved.publishToCommunity);
  const booksById = new Map([...pool.querySelectorAll(".tier-book")].map(book => [Number(book.dataset.bookId), book]));
  (saved.placements || []).forEach(placement => {
    const book = booksById.get(Number(placement.bookId)),
      zone = board.querySelector(`.tier-dropzone[data-grade="${placement.grade}"]`);
    if (book && zone) zone.append(book);
  });
  updatePool();
  message.textContent = "마지막으로 저장한 티어리스트를 불러왔습니다.";
}
function buildBoard() {
  board.replaceChildren();
  grades.forEach(grade => {
    const row = document.createElement("div");
    row.className = "tier-row";
    row.innerHTML = `<div class="tier-label">${grade}</div>`;
    const zone = document.createElement("div");
    zone.className = "tier-dropzone";
    zone.dataset.grade = grade;
    wireZone(zone);
    row.append(zone);
    board.append(row);
  });
  wireZone(pool);
}
function resetBooks() {
  pool.replaceChildren();
  template.items.forEach(item => pool.append(bookNode(item)));
  updatePool();
}
function bookNode(item) {
  const el = document.createElement("div");
  el.className = "tier-book";
  el.draggable = true;
  el.dataset.bookId = item.bookId;
  el.dataset.tooltip = `${item.title} - ${item.authorName}`;
  el.innerHTML = `<img src="${item.imageUrl || ""}" alt="${escapeHtml(item.title)}" draggable="false"><span>${
    escapeHtml(item.title)
  }</span>`;
  el.addEventListener("dragstart", event => {
    dragged = el;
    placeholder = document.createElement("div");
    placeholder.className = "tier-book drop-preview";
    placeholder.style.width = `${el.offsetWidth}px`;
    placeholder.style.height = `${el.offsetHeight}px`;
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", String(item.bookId));
    const ghost = document.createElement("div");
    ghost.className = "tier-drag-ghost";
    const ghostImage = el.querySelector("img").cloneNode();
    ghost.append(ghostImage);
    document.body.append(ghost);
    event.dataTransfer.setDragImage(ghost, ghost.offsetWidth / 2, ghost.offsetHeight / 2);
    setTimeout(() => ghost.remove(), 0);
    requestAnimationFrame(() => {
      el.after(placeholder);
      el.classList.add("is-dragging");
    });
  });
  el.addEventListener("dragend", () => {
    el.classList.remove("is-dragging");
    placeholder?.remove();
    placeholder = null;
    dragged = null;
    document.querySelectorAll(".drag-over").forEach(zone => zone.classList.remove("drag-over"));
    updatePool();
  });
  return el;
}

document.addEventListener("dragover", event => {
  if (!dragged) return;
  event.preventDefault();
  if (!event.target.closest(".tier-dropzone")) event.dataTransfer.dropEffect = "none";
}, { capture: true });

document.addEventListener("drop", event => {
  if (dragged && !event.target.closest(".tier-dropzone")) event.preventDefault();
}, { capture: true });

function wireZone(zone) {
  zone.addEventListener("dragover", event => {
    event.preventDefault();
    if (!dragged || !placeholder) return;
    zone.classList.add("drag-over");
    const target = event.target.closest(".tier-book:not(.is-dragging):not(.drop-preview)");
    if (target && target.parentElement === zone) {
      const rect = target.getBoundingClientRect();
      zone.insertBefore(placeholder, event.clientX < rect.left + rect.width / 2 ? target : target.nextSibling);
    } else if (placeholder.parentElement !== zone) zone.append(placeholder);
  });
  zone.addEventListener("dragleave", event => {
    if (!zone.contains(event.relatedTarget)) zone.classList.remove("drag-over");
  });
  zone.addEventListener("drop", event => {
    event.preventDefault();
    zone.classList.remove("drag-over");
    if (dragged && placeholder) {
      zone.insertBefore(dragged, placeholder);
      placeholder.remove();
      placeholder = null;
      updatePool();
    }
  });
}
function updatePool() {
  document.getElementById("pool-count").textContent = `${
    pool.querySelectorAll(".tier-book:not(.drop-preview)").length
  }권 남음`;
}
document.getElementById("reset-button").addEventListener("click", () => {
  if (confirm("모든 책을 처음 위치로 되돌릴까요?")) resetBooks();
});
const dialog = document.getElementById("save-dialog");
document.getElementById("save-open-button").addEventListener("click", () => dialog.showModal());
document.getElementById("save-cancel").addEventListener("click", () => dialog.close());
document.getElementById("save-form").addEventListener("submit", async event => {
  event.preventDefault();
  const placements = [];
  grades.forEach(grade =>
    document.querySelectorAll(`.tier-dropzone[data-grade="${grade}"] .tier-book:not(.drop-preview)`).forEach(el =>
      placements.push({ bookId: Number(el.dataset.bookId), grade })
    )
  );
  if (pool.querySelectorAll(".tier-book:not(.drop-preview)").length) {
    message.textContent = "모든 책을 티어에 배치한 뒤 저장해 주세요.";
    dialog.close();
    return;
  }
  try {
    const response = await fetch("/api/tier-lists", {
        method: "POST",
        headers: { "Content-Type": "application/json;charset=UTF-8" },
        credentials: "include",
        body: JSON.stringify({
          templateId,
          description: document.getElementById("list-description").value,
          publishToCommunity: document.getElementById("list-community").checked,
          placements,
        }),
      }),
      data = await response.json();
    if (response.status === 401) {
      alert("저장하려면 로그인이 필요합니다.");
      location.href = "/pages/auth/login.html";
      return;
    }
    if (!response.ok) throw new Error(data.message);
    dialog.close();
    message.textContent = data.message;
  } catch (error) {
    message.textContent = error.message || "저장하지 못했습니다.";
    dialog.close();
  }
});
function escapeHtml(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
    "\"",
    "&quot;",
  );
}
init();
