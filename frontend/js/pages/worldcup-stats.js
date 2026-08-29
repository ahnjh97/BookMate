const id = new URLSearchParams(location.search).get("id"),
  statusEl = document.querySelector("#status"),
  table = document.querySelector("#table");
async function load() {
  try {
    const r = await fetch(`/api/worldcup/stats?templateId=${id}`), d = await r.json();
    if (!r.ok) throw new Error(d.message);
    const x = d.stats;
    document.querySelector("#title").textContent = `${x.title} 전체 통계`;
    document.querySelector("#summary").textContent = `총 ${x.totalRuns}회 완료된 월드컵의 선택을 합산했습니다.`;
    document.querySelector("#play").href = `/pages/worldcup/play.html?id=${id}`;
    table.innerHTML =
      "<table class=\"stats-table\"><thead><tr><th>순위</th><th>책</th><th>우승</th><th>우승률</th><th>결승 진출</th><th>대결 승리</th><th>승률</th></tr></thead><tbody>"
      + x.stats.map((b, i) =>
        `<tr><td>${i + 1}</td><td class="stats-book"><a href="/pages/book/detail.html?id=${b.bookId}">${
          b.imageUrl ? `<img src="${esc(b.imageUrl)}" alt="" loading="lazy" decoding="async">` : ""
        }<span><strong>${esc(b.title)}</strong><br><small>${
          esc(b.authorName)
        }</small></span></a></td><td>${b.championships}회</td><td>${
          Number(b.championshipRate).toFixed(1)
        }%</td><td>${b.finals}회</td><td>${b.wins} / ${b.matches}</td><td>${Number(b.winRate).toFixed(1)}%</td></tr>`
      ).join("") + "</tbody></table>";
    statusEl.hidden = true;
    table.hidden = false;
  } catch (e) {
    statusEl.textContent = e.message || "통계를 불러오지 못했습니다.";
  }
}
function esc(v) {
  return String(v ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
    "\"",
    "&quot;",
  );
}
load();
