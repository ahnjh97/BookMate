// ============================================
// 파일: js/pages/login.js
//
// ── 뭐가 어디로 이동했는지 ──
//   headers 설정          → http.js의 request() 안으로 이동
//   JSON.stringify(body)  → http.js의 request() 안으로 이동
//   response.json() 파싱  → http.js의 request() 안으로 이동
//   !response.ok 판단 후 throw → http.js의 request() 안으로 이동
//   ("로그인에 실패했습니다" 기본 메시지 → http.js에서
//     "요청 처리 중 오류가 발생했습니다"로 일괄 대체됨, 문구만 다르고 동작은 동일)
//
// ── 기능 충분성 검토 ──
//   원본이 하던 일(요청 조립, 실패 판단, 에러 던지기) 전부 http.js가 그대로 수행
//   → 빠진 기능 없음. login.js 쪽엔 폼 처리·리다이렉트 로직만 남음(원본과 동일)
// ============================================
import { authApi } from "../api/authApi.js";

const loginForm = document.querySelector("#login-form");
const loginMessage = document.querySelector("#form-message");

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const submit = loginForm.querySelector('[type="submit"]');
  submit.disabled = true;
  loginMessage.textContent = "";
  
  try {
    const data = Object.fromEntries(new FormData(loginForm));

    // const response = await fetch("/api/auth/login", {
    //   method: "POST",
    //   headers: { "Content-Type": "application/json" },
    //   body: JSON.stringify(data)
    // });
    // authApi.login(data) 한 줄이 아래 4개 역할을 전부 대체
    // URL(/api/auth), method(POST)      → endpoints.js + authApi.js
    // headers, JSON.stringify(body)     → http.js
    // 응답 실패 판단 + throw            → http.js
    await authApi.login(data);

    const redirect = new URLSearchParams(window.location.search).get("redirect");
    let safeRedirect = "/";
    if (redirect?.startsWith("/")) {
      const target = new URL(redirect, window.location.origin);
      if (target.origin === window.location.origin) {
        safeRedirect = `${target.pathname}${target.search}${target.hash}`;
      }
    }
    window.location.href = safeRedirect;
  } catch (error) {
    loginMessage.textContent = error.message;
    loginMessage.className = "form-message error";
    submit.disabled = false;
  }
});
