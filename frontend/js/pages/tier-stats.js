const templateId = Number(new URLSearchParams(location.search).get("id")),
  board = document.getElementById("stats-grid"),
  statusEl = document.getElementById("stats-status"),
  grades = ["S", "A", "B", "C", "D"];
async function loadStats() {
  if (!templateId) {
    statusEl.textContent = "템플릿 번호가 필요합니다.";
    return;
  }
  document.getElementById("back-to-maker").href = `/pages/tier/maker.html?id=${templateId}`;
  try {
    const response = await fetch(`/api/tier-stats?templateId=${templateId}`), data = await response.json();
    if (!response.ok) throw new Error(data.message);
    const result = data.stats;
    document.getElementById("stats-template-title").textContent = result.title;
    renderBoard(result.stats || [], result.communityListCount || 0);
  } catch (error) {
    statusEl.textContent = error.message || "통계를 불러오지 못했습니다.";
  }
}
function renderBoard(items, listCount) {
  board.replaceChildren();
  board.className = "tier-board community-stats-board";
  statusEl.innerHTML = listCount
    ? `총 <strong>${listCount}명</strong>이 완성한 티어리스트의 배치를 합산했습니다.`
    : "아직 저장된 티어리스트가 없어 모든 책을 미집계 영역에 표시합니다.";
  grades.forEach(grade => {
    const row = document.createElement("div");
    row.className = "tier-row";
    row.innerHTML = `<div class="tier-label">${grade}</div>`;
    const zone = document.createElement("div");
    zone.className = "tier-dropzone stats-dropzone";
    items.filter(item => item.dominantGrade === grade).sort((a, b) =>
      (b.counts?.[grade] || 0) - (a.counts?.[grade] || 0)
    ).forEach(item => zone.append(statBook(item, grade, listCount)));
    row.append(zone);
    board.append(row);
  });
  const unranked = items.filter(item => item.dominantGrade === "-");
  if (unranked.length) {
    const row = document.createElement("div");
    row.className = "tier-row stats-unranked-row";
    row.innerHTML = "<div class=\"tier-label\">-</div>";
    const zone = document.createElement("div");
    zone.className = "tier-dropzone stats-dropzone";
    unranked.forEach(item => zone.append(statBook(item, "-", listCount)));
    row.append(zone);
    board.append(row);
  }
}
function statBook(item, grade, listCount) {
  const counts = item.counts || {},
    count = grade === "-" ? 0 : counts[grade] || 0,
    percentage = listCount ? Math.round(count / listCount * 100) : 0,
    maxCount = Math.max(...grades.map(value => counts[value] || 0)),
    leaders = maxCount ? grades.filter(value => (counts[value] || 0) === maxCount) : [],
    leaderLabel = leaders.length > 1 ? `${leaders.join("·")} 공동 1위` : grade,
    summary = listCount ? `${leaderLabel} ${count}명 · ${percentage}%` : "미집계",
    book = document.createElement("a");
  book.className = "tier-book stats-tier-book";
  book.href = `/pages/book/detail.html?id=${item.bookId}`;
  book.draggable = false;
  book.addEventListener("dragstart", event => event.preventDefault());
  const rows = grades.map(value => {
    const valueCount = counts[value] || 0, valuePercentage = listCount ? Math.round(valueCount / listCount * 100) : 0;
    return `<div class="stats-breakdown-row"><b>${value}</b><span><i style="width:${valuePercentage}%"></i></span><small>${valueCount}명 · ${valuePercentage}%</small></div>`;
  }).join("");
  book.innerHTML = `<img src="${escapeHtml(statsThumbnailUrl(item.imageUrl))}" alt="${
    escapeHtml(item.title)
  }" loading="eager" decoding="async" fetchpriority="low" draggable="false"><span>${escapeHtml(item.title)}</span><small class="stats-summary">${
    escapeHtml(summary)
  }</small><div class="stats-breakdown" role="tooltip"><strong>${escapeHtml(item.title)}</strong><p>${
    escapeHtml(item.authorName)
  }</p>${rows}</div>`;
  return book;
}
function statsThumbnailUrl(imageUrl) {
  const url = String(imageUrl || "");
  return url.replace("/fit-in/600x0/", "/fit-in/160x0/");
}
function escapeHtml(value) {
  return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
    "\"",
    "&quot;",
  );
}
loadStats();
