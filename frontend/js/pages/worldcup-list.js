const grid = document.querySelector("#grid");
const participationButtons = document.querySelectorAll("[data-participation]");
let templates = [];
let selectedParticipation = "all";
const pagination = window.BookMateListPagination.create({
  root: document.querySelector("#worldcup-pagination"),
  pageSize: 8,
  onRender: renderTemplatePage,
});

document.querySelector("#worldcup-reset-button").addEventListener("click", () => {
  selectedParticipation = "all";
  participationButtons.forEach((button, index) => {
    button.classList.toggle("is-active", index === 0);
    button.setAttribute("aria-pressed", String(index === 0));
  });
  history.replaceState({}, "", location.pathname);
  renderTemplates();
});

participationButtons.forEach(button => {
  button.addEventListener("click", () => {
    selectedParticipation = button.dataset.participation;
    participationButtons.forEach(item => {
      const active = item === button;
      item.classList.toggle("is-active", active);
      item.setAttribute("aria-pressed", String(active));
    });
    renderTemplates();
  });
});

async function loadTemplates() {
  grid.replaceChildren();
  try {
    const response = await fetch("/api/worldcup/templates", { cache: "no-store" });
    const data = await response.json();
    if (!response.ok) throw new Error(data.message);
    templates = data.templates || [];
    renderTemplates();
  } catch (error) {
    console.error(error);
  }
}

function renderTemplates() {
  const filtered = templates.filter(template =>
    selectedParticipation === "all"
      || (selectedParticipation === "participated" ? template.participated : !template.participated)
  );
  pagination.setItems(filtered);
}

function renderTemplatePage(filtered) {
  grid.replaceChildren();
  filtered.forEach(template => {
    const link = document.createElement("a");
    link.className = `worldcup-card${template.participated ? " is-participated" : ""}`;
    link.href = `/pages/worldcup/play.html?id=${template.templateId}`;
    const badge = template.participated
      ? '<strong class="participation-badge">참여 완료</strong>'
      : "";
    const categoryClass = template.category === "장르" ? "category-genre" : "category-default";
    link.innerHTML = `<div class="worldcup-cover">${createCoverCollage(template.coverImages)}</div>`
      + `<div class="worldcup-card-body"><div class="worldcup-meta">`
      + `<span class="worldcup-category-badge ${categoryClass}">${escapeHtml(template.category)}</span>`
      + `<span class="worldcup-count">${template.itemCount}권</span><span>${escapeHtml(template.creatorNickname)}</span>${badge}</div>`
      + `<h3>${escapeHtml(template.title || "이름 없는 템플릿")}</h3><p>${escapeHtml(
        template.description || "책 취향의 최종 우승자를 골라보세요."
      )}</p></div>`;
    grid.append(link);
  });
}

function createCoverCollage(images) {
  const safeImages = Array.isArray(images) ? images.filter(Boolean).slice(0, 2) : [];
  if (!safeImages.length) return "<b>WORLD CUP</b>";
  return `<div class="worldcup-collage collage-${safeImages.length}">${safeImages.map(image =>
    `<img src="${escapeHtml(image)}" alt="" loading="eager" decoding="async">`
  ).join("")}</div>`;
}

function escapeHtml(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;").replaceAll("'", "&#039;");
}

loadTemplates();
