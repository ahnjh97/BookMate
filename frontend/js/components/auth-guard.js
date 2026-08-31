// ============================================
// 파일: js/components/auth-guard.js
// 목적: 비로그인 시 페이지 접근 차단, 로그인 페이지로 리다이렉트
// ============================================
import { authApi } from "/js/api/authApi.js";

document.documentElement.style.visibility = "hidden";

(async function requireLogin() {
  try {
    const result = await authApi.checkSession();

    if (result.loggedIn) {
      document.documentElement.style.visibility = "";
      return;
    }
  } catch (error) {
    console.error("로그인 상태를 확인하지 못했습니다.", error);
  }

  const returnUrl = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  window.location.replace(`/pages/auth/login.html?redirect=${encodeURIComponent(returnUrl)}`);
})();