const grid = document.querySelector("#grid"),
  statusEl = document.querySelector("#status"),
  form = document.querySelector("#ideal-search"),
  keyword = document.querySelector("#keyword");
form.addEventListener("submit", e => {
  e.preventDefault();
  load();
});
async function load() {
  grid.replaceChildren();
  statusEl.textContent = "템플릿을 불러오는 중입니다.";
  try {
    const r = await fetch(
        `/api/ideal/templates${keyword.value.trim() ? `?keyword=${encodeURIComponent(keyword.value.trim())}` : ""}`,
      ),
      d = await r.json();
    if (!r.ok) throw new Error(d.message);
    statusEl.textContent = d.templates.length ? `${d.templates.length}개의 월드컵 템플릿` : "검색 결과가 없습니다.";
    d.templates.forEach(t => {
      const a = document.createElement("a");
      a.className = "ideal-card";
      a.href = `/pages/ideal/play.html?id=${t.templateId}`;
      a.innerHTML = `<div class="ideal-cover">${
        t.coverImage ? `<img src="${esc(t.coverImage)}" alt="">` : "<b>WORLD CUP</b>"
      }</div><div class="ideal-card-body"><div class="ideal-meta"><span>${t.itemCount}권</span><span>${
        esc(t.creatorNickname)
      }${t.participated ? " · 참여 완료" : ""}</span></div><h2>${esc(t.title)}</h2><p>${
        esc(t.description || "책 취향의 최종 우승자를 골라보세요.")
      }</p></div>`;
      grid.append(a);
    });
  } catch (e) {
    statusEl.textContent = e.message || "불러오지 못했습니다.";
  }
}
function esc(v) {
  return String(v ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
    "\"",
    "&quot;",
  );
}
load();
