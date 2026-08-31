// ============================================
// 목적: profile fragment 전용 — 닉네임 표시 + 유사회원(취향매칭) 로드
// 원본: js/pages/member-page.js에서 로그인체크 로직(protectMemberPage)만 빠지고 나머지 그대로
// 호출: mypage.js가 profile 탭 로드 시 이 파일의 init()을 호출
// ============================================

export function init() {
    const auth = window.__memberAuth; // mypage.js에서 미리 저장해둔 로그인 정보 재사용
    document.querySelectorAll("[data-member-nickname]").forEach((element) => {
        element.textContent = auth.nickname || auth.loginId;
    });
    if (document.querySelector("#similar-members-list")) loadSimilarMembers();
}

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
      <span class="taste-match-rank">TOP ${rank}</span>
      <h3>${escapeHtml(user.nickname)}</h3>
      <p class="taste-match-score">${formatScore(user.similarityScore)}% 일치</p>
      <dl class="taste-match-components">
        ${componentRow("평점", user.ratingSimilarity)}
        ${componentRow("티어리스트", user.tierSimilarity)}
        ${componentRow("이상형 월드컵", user.worldcupSimilarity)}
      </dl>
      <p class="taste-match-evidence">
        공통 평점 ${user.commonRatingCount}권 · 티어 도서 ${user.commonTierBookCount}권<br>
        월드컵 도서 ${user.commonWorldcupBookCount}권 · 신뢰도 ${confidenceLabel(user.confidence)}
      </p>`;
    return card;
}

function componentRow(label, score) {
    return `<div><dt>${label}</dt><dd>${score == null ? "데이터 부족" : `${formatScore(score)}%`}</dd></div>`;
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