window.BookMateMemberPage = { loadSimilarMembers };

document.addEventListener("click", () => {
  document.querySelectorAll(".bookshelf-visit-prompt:not([hidden])").forEach((prompt) => {
    prompt.hidden = true;
  });
});

async function loadSimilarMembers() {
  const list = document.querySelector("#similar-members-list");
  const status = document.querySelector("#similar-members-status");
  try {
    const response = await fetch("/api/preferences/similar?limit=3", {
      credentials: "include",
      cache: "no-store",
    });
    const result = await response.json();
    if (!response.ok || !result.success) throw new Error(result.message);
    list.replaceChildren();
    if (!result.users.length) {
      status.textContent = "비교할 수 있는 취향 데이터가 아직 부족합니다.";
      return;
    }
    status.textContent = "";
    result.users.forEach((user, index) => list.append(createMatchCard(user, index + 1)));
  } catch (error) {
    status.dataset.state = "error";
    status.textContent = error.message || "취향 분석 결과를 불러오지 못했습니다.";
  }
}

function createMatchCard(user, rank) {
  const card = document.createElement("article");
  card.className = "taste-match-card";
  card.innerHTML = `
    <div class="taste-match-summary">
      ${medalMarkup(rank)}
      <div class="taste-match-user">
        <button class="taste-match-nickname" type="button">${escapeHtml(user.nickname)}</button>
        <div class="bookshelf-visit-prompt" hidden>
          <button type="button" data-bookshelf-member-id="${Number(user.memberId)}">책장 방문하기</button>
        </div>
      </div>
      <div class="taste-match-result">
        <small>취향일치율</small>
        <strong class="taste-match-score">${formatScore(user.similarityScore)}%</strong>
      </div>
    </div>
    <div class="taste-match-details">
      <dl class="taste-match-components">
        ${componentRow("책 평점", user.ratingSimilarity)}
        ${componentRow("티어리스트", user.tierSimilarity)}
        ${componentRow("이상형월드컵", user.worldcupSimilarity)}
      </dl>
      <p class="taste-match-evidence">
        공통 평점 ${user.commonRatingCount}권 · 티어 도서 ${user.commonTierBookCount}권<br>
        월드컵 도서 ${user.commonWorldcupBookCount}권 · 신뢰도 ${confidenceLabel(user.confidence)}
      </p>
    </div>`;

  const nicknameButton = card.querySelector(".taste-match-nickname");
  nicknameButton.title = String(user.nickname || "");
  const visitPrompt = card.querySelector(".bookshelf-visit-prompt");
  const visitButton = card.querySelector("[data-bookshelf-member-id]");
  visitPrompt.addEventListener("click", (event) => event.stopPropagation());
  visitButton.addEventListener("click", () => {
    window.location.href = `/pages/member/bookshelf.html?memberId=${Number(user.memberId)}`;
  });
  nicknameButton.addEventListener("click", (event) => {
    event.stopPropagation();
    document.querySelectorAll(".bookshelf-visit-prompt:not([hidden])").forEach((prompt) => {
      if (prompt !== visitPrompt) prompt.hidden = true;
    });
    visitPrompt.hidden = !visitPrompt.hidden;
  });
  return card;
}

function componentRow(label, score) {
  return `<div><dt>${label}</dt><dd>${score == null ? "데이터 부족" : `${formatScore(score)}%`}</dd></div>`;
}

function medalMarkup(rank) {
  const labels = ["금메달", "은메달", "동메달"];
  return `
    <span class="taste-match-rank taste-match-medal-${rank}" aria-label="${labels[rank - 1]}">
      <svg viewBox="0 0 36 42" aria-hidden="true">
        <path class="medal-ribbon" d="M8 1h8l5 15-7 4L8 1Zm20 0h-8l-5 15 7 4L28 1Z"/>
        <circle cx="18" cy="26" r="13"/>
        <path class="medal-star" d="m18 18 2.3 4.7 5.2.8-3.8 3.7.9 5.2-4.6-2.5-4.6 2.5.9-5.2-3.8-3.7 5.2-.8L18 18Z"/>
      </svg>
    </span>`;
}

function confidenceLabel(value) {
  if (value === "HIGH") return "높음";
  if (value === "MEDIUM") return "보통";
  return "낮음";
}

function formatScore(value) {
  return Number(value || 0).toFixed(1);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
