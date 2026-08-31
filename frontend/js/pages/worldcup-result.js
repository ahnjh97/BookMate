const id = new URLSearchParams(location.search).get("id"),
  statusEl = document.querySelector("#status"),
  content = document.querySelector("#content"),
  shareButton = document.querySelector("#community-share"),
  shareDialog = document.querySelector("#community-share-dialog"),
  shareForm = document.querySelector("#community-share-form"),
  shareContent = document.querySelector("#community-share-content");
let currentResult = null;
async function load() {
  try {
    const r = await fetch(`/api/worldcup/runs?id=${id}`), d = await r.json();
    if (!r.ok) throw new Error(d.message);
    const x = d.result;
    currentResult = x;
    document.querySelector("#title").innerHTML = `<span class="page-title-icon page-title-icon-trophy" aria-hidden="true"></span>${esc(x.title)}`;
    document.querySelector("#stats-link").href = `/pages/worldcup/stats.html?id=${x.templateId}`;
    if (x.owner) {
      shareButton.hidden = false;
      shareButton.textContent = x.publishedToCommunity ? "커뮤니티 게시글 보기" : "커뮤니티에 공유";
    }
    const winner = x.matches.find(m => m.roundSize === 2).winner;
    document.querySelector("#winner").innerHTML = `<p>🏆 최종 우승</p><a href="/pages/book/detail.html?id=${winner.bookId}">${
      winner.imageUrl ? `<img src="${esc(winner.imageUrl.replace("-240.webp", "-520.webp"))}" alt="" decoding="async">` : ""
    }<h2>${esc(winner.title)}</h2></a>`;
    const sizes = [];
    for (let s = 2; s <= x.bracketSize; s *= 2) sizes.push(s);
    const bracket = document.querySelector("#bracket");
    sizes.forEach((s, roundIndex) => {
      const matches = x.matches.filter(m => m.roundSize === s);
      if (roundIndex > 0) bracket.append(createConnector(matches.length / 2));
      const col = document.createElement("section");
      col.className = "bracket-round";
      col.innerHTML = `<h3>${s === 2 ? "결승" : s + "강"}</h3>`;
      const matchesElement = document.createElement("div");
      matchesElement.className = "bracket-round-matches";
      matchesElement.style.setProperty("--match-count", matches.length);
      matches.forEach(m => {
        const box = document.createElement("div");
        box.className = "bracket-match";
        box.innerHTML = entry(m.left, m.winner.bookId) + entry(m.right, m.winner.bookId);
        matchesElement.append(box);
      });
      col.append(matchesElement);
      bracket.append(col);
    });
    statusEl.hidden = true;
    content.hidden = false;
  } catch (e) {
    statusEl.textContent = e.message || "결과를 불러오지 못했습니다.";
  }
}
shareButton.addEventListener("click", async () => {
  if (!currentResult) return;
  if (currentResult.postId) {
    location.href = `/pages/post/detail.html?postId=${currentResult.postId}`;
    return;
  }
  shareDialog.showModal();
  shareContent.focus();
});
document.querySelector("#community-share-cancel").addEventListener("click", () => shareDialog.close());
shareForm.addEventListener("submit", async event => {
  event.preventDefault();
  shareButton.disabled = true;
  shareButton.textContent = "공유하는 중...";
  const submitButton = shareForm.querySelector('button[type="submit"]');
  submitButton.disabled = true;
  try {
    const response = await fetch("/api/worldcup/share", {
      method: "POST",
      headers: { "Content-Type": "application/json;charset=UTF-8" },
      credentials: "include",
      body: JSON.stringify({ runId: Number(id), content: shareContent.value }),
    });
    const data = await response.json();
    if (!response.ok || !data.success) throw new Error(data.message || "공유하지 못했습니다.");
    sessionStorage.setItem("bookmate:flash-toast", JSON.stringify({
      state: "success",
      message: "이상형월드컵 결과가 커뮤니티에 공유되었습니다.",
    }));
    location.href = `/pages/post/detail.html?postId=${data.postId}`;
  } catch (error) {
    shareButton.disabled = false;
    shareButton.textContent = "커뮤니티에 공유";
    submitButton.disabled = false;
    alert(error.message || "월드컵 결과를 공유하지 못했습니다.");
  }
});
function createConnector(branchCount) {
  const connector = document.createElement("div");
  connector.className = "bracket-connectors";
  connector.style.setProperty("--branch-count", branchCount);
  for (let index = 0; index < branchCount; index += 1) {
    const branch = document.createElement("span");
    branch.innerHTML = "<i></i><i></i><i></i>";
    connector.append(branch);
  }
  return connector;
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
