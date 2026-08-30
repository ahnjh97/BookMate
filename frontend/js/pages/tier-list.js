const grid = document.getElementById("template-grid");
const participationButtons = document.querySelectorAll(".book-category-tabs [data-participation]");
let templates = [], selectedParticipation = "all";
const pagination = window.BookMateListPagination.create({
  root: document.getElementById("tier-pagination"),
  pageSize: 8,
  scrollTarget: grid,
  onRender: renderTemplates,
});
document.getElementById("template-reset-button").addEventListener("click", () => {
  selectedParticipation = "all";
  participationButtons.forEach((button, index) => {
    button.classList.toggle("is-active", index === 0);
    button.setAttribute("aria-pressed", String(index === 0));
  });
  history.replaceState({}, "", location.pathname);
  renderFilteredTemplates();
});
async function loadTemplates() {
  grid.replaceChildren();
  try {
    const r = await fetch("/api/tier-templates", {
      cache: "no-store",
    });
    const d = await r.json();
    if (!r.ok) throw new Error(d.message);
    templates = d.templates || [];
    renderFilteredTemplates();
  } catch (e) {
    console.error(e);
  }
}
function renderFilteredTemplates() {
  const filtered = templates.filter(t =>
    selectedParticipation === "all" || (selectedParticipation === "participated" ? t.participated : !t.participated)
  );
  pagination.setItems(filtered);
}
function renderTemplates(items) {
  grid.replaceChildren();
  items.forEach(t => {
    const a = document.createElement("a");
    a.className = `template-card${t.participated ? " is-participated" : ""}`;
    a.href = `/pages/tier/maker.html?id=${t.templateId}`;
    const cover = createCoverCollage(t.coverImages);
    const badge = t.participated ? "<strong class=\"participation-badge\">참여 완료</strong>" : "";
    const categoryClass = getCategoryClass(t.category);
    a.innerHTML =
      `<div class="template-cover">${cover}</div><div class="template-body"><div class="template-meta"><span class="category-chip ${categoryClass}">${
        escapeHtml(t.category)
      }</span><span class="template-count">${t.itemCount}권</span><span>${
        escapeHtml(t.creatorNickname)
      }</span>${badge}</div><h3>${escapeHtml(t.title)}</h3><p>${
        escapeHtml(t.description || "등록된 설명이 없습니다.")
      }</p></div>`;
    grid.append(a);
  });
}
function createCoverCollage(images) {
  const safeImages = Array.isArray(images) ? images.filter(Boolean).slice(0, 2) : [];
  if (!safeImages.length) return `<span>BOOKMATE</span>`;
  return `<div class="template-collage collage-${safeImages.length}">${safeImages.map(image =>
    `<img src="${escapeHtml(image)}" alt="" loading="lazy" decoding="async">`
  ).join("")}</div>`;
}
participationButtons.forEach(button =>
  button.addEventListener("click", () => {
    selectedParticipation = button.dataset.participation;
    participationButtons.forEach(item => {
      const active = item === button;
      item.classList.toggle("is-active", active);
      item.setAttribute("aria-pressed", String(active));
    });
    renderFilteredTemplates();
  })
);
function getCategoryClass(category) {
  return {
    "장르": "category-genre",
    "시리즈": "category-series",
    "작가": "category-author",
    "테마": "category-theme",
  }[category] || "category-default";
}
function escapeHtml(v) {
  return String(v ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
    "\"",
    "&quot;",
  ).replaceAll("'", "&#039;");
}
loadTemplates();
