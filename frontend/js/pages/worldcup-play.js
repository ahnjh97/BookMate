const id = new URLSearchParams(location.search).get("id"),
  title = document.querySelector("#title"),
  desc = document.querySelector("#description"),
  setup = document.querySelector("#setup"),
  game = document.querySelector("#game"),
  versus = document.querySelector("#versus");
let template, roundSize, current = [], next = [], matchIndex = 0, allMatches = [], totalMatches = 0, done = 0;
async function load() {
  try {
    const r = await fetch(`/api/worldcup/templates?id=${id}`), d = await r.json();
    if (!r.ok) throw new Error(d.message);
    template = d.template;
    if (template.participated && template.runId) {
      const previousResult = document.querySelector("#previous-result");
      previousResult.href = `/pages/worldcup/result.html?id=${template.runId}`;
      previousResult.hidden = false;
    }
    title.textContent = template.title;
    desc.textContent = template.description || "더 마음에 드는 책을 선택하세요.";
    document.querySelector("#stats-link").href = `/pages/worldcup/stats.html?id=${id}`;
    if (template.items.length < 16) document.querySelector("input[value=\"16\"]").disabled = true;
  } catch (e) {
    document.querySelector("#setup-message").textContent = e.message;
  }
}
document.querySelector("#start").onclick = () => {
  const size = Number(document.querySelector("input[name=\"size\"]:checked").value);
  if (template.items.length < size) return;
  roundSize = size;
  current = shuffle([...template.items]).slice(0, size);
  totalMatches = size - 1;
  setup.hidden = true;
  game.hidden = false;
  renderMatch();
};
function renderMatch() {
  document.querySelector("#round-label").textContent = roundSize === 2 ? "결승" : `${roundSize}강`;
  document.querySelector("#match-label").textContent = `${matchIndex + 1} / ${roundSize / 2} 경기`;
  document.querySelector("#progress").style.width = `${done / totalMatches * 100}%`;
  const pair = [current[matchIndex * 2], current[matchIndex * 2 + 1]];
  versus.innerHTML = `${card(pair[0], 0)}<div class="vs-mark">VS</div>${card(pair[1], 1)}`;
  versus.querySelectorAll(".versus-card").forEach((el, i) => el.onclick = () => choose(pair, i));
}
function choose(pair, index) {
  const winner = pair[index];
  allMatches.push({
    roundSize,
    matchOrder: matchIndex,
    leftBookId: pair[0].bookId,
    rightBookId: pair[1].bookId,
    winnerBookId: winner.bookId,
  });
  next.push(winner);
  done++;
  matchIndex++;
  if (matchIndex < roundSize / 2) {
    renderMatch();
    return;
  }
  if (roundSize === 2) {
    save();
    return;
  }
  current = next;
  next = [];
  roundSize /= 2;
  matchIndex = 0;
  renderMatch();
}
async function save() {
  versus.innerHTML = "<p class=\"worldcup-status\">결과를 저장하는 중입니다.</p>";
  try {
    const r = await fetch("/api/worldcup/runs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ templateId: Number(id), bracketSize: totalMatches + 1, matches: allMatches }),
      }),
      d = await r.json();
    if (!r.ok) throw new Error(d.message);
    location.href = `/pages/worldcup/result.html?id=${d.runId}`;
  } catch (e) {
    versus.innerHTML = `<p class="worldcup-status">${esc(e.message)}</p>`;
  }
}
function card(b, i) {
  const imageUrl = (b.imageUrl || "").replace("-240.webp", "-520.webp");
  return `<article class="versus-card" role="button" tabindex="0" data-index="${i}">${
    imageUrl ? `<img src="${esc(imageUrl)}" alt="" decoding="async">` : ""
  }<h2>${esc(b.title)}</h2><p>${esc(b.authorName)}</p></article>`;
}
function shuffle(a) {
  for (let i = a.length - 1; i; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}
function esc(v) {
  return String(v ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
    "\"",
    "&quot;",
  );
}
load();
