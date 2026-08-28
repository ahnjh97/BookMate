document.documentElement.style.visibility = "hidden";

(async function requireLogin() {
  try {
    const response = await fetch("/api/auth/session", {
      cache: "no-store",
      credentials: "same-origin"
    });
    const auth = response.ok ? await response.json() : { loggedIn: false };

    if (auth.loggedIn) {
      document.documentElement.style.visibility = "";
      return;
    }
  } catch (error) {
    console.error("로그인 상태를 확인하지 못했습니다.", error);
  }

  const returnUrl = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  window.location.replace(`/pages/auth/login.html?redirect=${encodeURIComponent(returnUrl)}`);
})();
