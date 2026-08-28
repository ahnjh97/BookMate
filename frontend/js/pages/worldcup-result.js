const id = new URLSearchParams(location.search).get("id"),
  statusEl = document.querySelector("#status"),
  content = document.querySelector("#content");
async function load() {
  try {
    const r = await fetch(`/api/worldcup/runs?id=${id}`), d = await r.json();
    if (!r.ok) throw new Error(d.message);
    const x = d.result;
    document.querySelector("#title").textContent = x.title;
    const replay = document.querySelector("#replay");
    replay.href = `/pages/worldcup/play.html?id=${x.templateId}`;
    document.querySelector("#stats-link").href = `/pages/worldcup/stats.html?id=${x.templateId}`;
    const winner = x.matches.find(m => m.roundSize === 2).winner;
    document.querySelector("#winner").innerHTML = `<p>🏆 최종 우승</p><a href="/pages/book/detail.html?id=${winner.bookId}">${
      winner.imageUrl ? `<img src="${esc(winner.imageUrl.replace("-240.webp", "-520.webp"))}" alt="" decoding="async">` : ""
    }<h2>${esc(winner.title)}</h2></a>`;
    const sizes = [];
    for (let s = x.bracketSize; s >= 2; s /= 2) sizes.push(s);
    const bracket = document.querySelector("#bracket");
    sizes.forEach(s => {
      const col = document.createElement("section");
      col.className = "bracket-round";
      col.innerHTML = `<h3>${s === 2 ? "결승" : s + "강"}</h3>`;
      x.matches.filter(m => m.roundSize === s).forEach(m => {
        const box = document.createElement("div");
        box.className = "bracket-match";
        box.innerHTML = entry(m.left, m.winner.bookId) + entry(m.right, m.winner.bookId);
        col.append(box);
      });
      bracket.append(col);
    });
    statusEl.hidden = true;
    content.hidden = false;
  } catch (e) {
    statusEl.textContent = e.message || "결과를 불러오지 못했습니다.";
  }
}
function entry(b, w) {
  return `<a class="bracket-entry ${b.bookId === w ? "is-winner" : ""}" href="/pages/book/detail.html?id=${b.bookId}">${
    b.imageUrl ? `<img src="${esc(b.imageUrl)}" alt="" loading="lazy" decoding="async">` : ""
  }<span>${esc(b.title)}</span></a>`;
}
function esc(v) {
  return String(v ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
    "\"",
    "&quot;",
  );
}
load();
