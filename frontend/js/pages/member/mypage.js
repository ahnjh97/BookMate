// ============================================
// 파일: pages/member/mypage.js
//
// [목적]
//   마이페이지 진입점. 로그인 확인 → 사이드바 메뉴 클릭에 따라
//   #mypage-content 안의 내용만 fragment(부분 HTML+JS)로 교체하는
//   단일 페이지 방식(SPA 없이 fetch로 부분 로드)
//
// [진입 흐름]
//   1. 세션 확인(authApi.checkSession()) → 비로그인이면 로그인 페이지로 이동
//   2. 로그인 확인되면 페이지 표시([data-member-page]의 hidden 제거)
//   3. 세션 정보를 window.__memberAuth에 전역 저장
//      → 모든 fragment.js가 재조회 없이 이 값을 바로 씀
//   4. URL의 ?tab= 값(없으면 profile)에 해당하는 fragment 로드
//
// [사이드바 메뉴 클릭 시 동작 — loadSection()]
//   1. ROUTES에서 해당 키의 fragment HTML 경로를 찾아 fetch, #mypage-content에 삽입
//   2. 브라우저 주소창 URL을 ?tab=키값으로 갱신(새로고침해도 같은 탭 유지,
//      뒤로가기 버튼도 정상 동작하도록 history.pushState 사용)
//   3. 사이드바에서 클릭된 메뉴에 active 스타일 표시
//   4. SECTION_SCRIPTS에 그 fragment 전용 JS가 등록되어 있으면 불러와서
//      init() 함수를 실행(각 fragment.js는 반드시 init()을 export해야 함)
//      → init()은 async여도 무방(비동기 API 호출 등), await로 완료를 기다림
//
// [새 탭(fragment) 추가하는 법]
//   1. ROUTES에 "키": "html 경로" 한 줄 추가
//   2. 그 fragment에 조회/저장 등 동작이 필요하면
//      SECTION_SCRIPTS에 "키": () => import("js 경로") 한 줄 추가
//      (동작이 필요 없는 정적 화면이면 SECTION_SCRIPTS 등록 생략 가능)
// ============================================

import { authApi } from "/js/api/authApi.js";

const contentArea = document.querySelector("#mypage-content");
const menuLinks = document.querySelectorAll("[data-mypage-menu]");
const page = document.querySelector("[data-member-page]");

const ROUTES = {
    "profile": "/pages/member/fragments/profile.html",
    "account-info": "/pages/member/fragments/account-info.html",
    "password": "/pages/member/fragments/password.html",
    "my-posts": "/pages/member/fragments/coming-soon.html",
    "my-ratings": "/pages/member/fragments/coming-soon.html",
    "bookcase": "/pages/member/fragments/coming-soon.html",
    "tier": "/pages/member/fragments/coming-soon.html",
};

const SECTION_SCRIPTS = {
    "profile": () => import("/js/pages/member/fragments/profile.js"),
    "account-info": () => import("/js/pages/member/fragments/account-info.js"),
    "password": () => import("/js/pages/member/fragments/password.js"),
    // "my-posts": () => import("/js/pages/member/fragments/my-posts.js"),
    // "my-ratings": () => import("/js/pages/member/fragments/my-ratings.js"),
    // "bookcase": () => import("/js/pages/member/fragments/bookcase.js"),
    // "tier": () => import("/js/pages/member/fragments/tier.js"),
};

async function loadSection(key) {
    if (!ROUTES[key]) return;

    const res = await fetch(ROUTES[key]);
    contentArea.innerHTML = await res.text();

    history.pushState({ key }, "", `/pages/member/mypage.html?tab=${key}`);
    menuLinks.forEach(link => link.classList.toggle("active", link.dataset.mypageMenu === key));

    if (SECTION_SCRIPTS[key]) {
        const module = await SECTION_SCRIPTS[key]();
        await module.init?.();
    }
}

menuLinks.forEach(link => {
    link.addEventListener("click", (e) => {
        e.preventDefault();
        if (link.classList.contains("disabled")) return;
        loadSection(link.dataset.mypageMenu);
    });
});

// 브라우저 뒤로가기/앞으로가기 시 그 시점의 탭으로 다시 전환
window.addEventListener("popstate", (e) => {
    const key = e.state?.key || new URLSearchParams(location.search).get("tab") || "profile";
    loadSection(key);
});

(async function init() {
    try {
        const auth = await authApi.checkSession();
        if (!auth.loggedIn) {
            window.location.replace("/pages/auth/login.html");
            return;
        }

        page.removeAttribute("hidden");
        renderUserBanner(auth);
        window.__memberAuth = auth;

        loadSection(new URLSearchParams(location.search).get("tab") || "profile");
    } catch {
        window.location.replace("/pages/auth/login.html");
    }
})();

window.addEventListener("bookmate:navigate-tab", (e) => {
    loadSection(e.detail);
});

function renderUserBanner(auth) {
    const nickname = auth.nickname || auth.loginId || "회원";
    const avatar = document.querySelector("#mypage-user-avatar");
    const nicknameEl = document.querySelector("#mypage-user-nickname");

    avatar.textContent = nickname.charAt(0).toUpperCase();
    avatar.style.background = pickColor(nickname); // 닉네임 기준 고정색(매번 안 바뀌게)
    nicknameEl.textContent = nickname;

    document.querySelector("#mypage-user-banner").addEventListener("click", () => {
        history.pushState({ key: "profile" }, "", "/pages/member/mypage.html?tab=profile");
        window.dispatchEvent(new PopStateEvent("popstate", { state: { key: "profile" } }));
    });
}

function pickColor(text) {
    const palette = ["#8B3A3A", "#4B6455", "#2F6F8F", "#7A5C3E", "#5B4B8A"];
    let hash = 0;
    for (const ch of text) hash = ch.charCodeAt(0) + ((hash << 5) - hash);
    return palette[Math.abs(hash) % palette.length];
}