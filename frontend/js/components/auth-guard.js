document.documentElement.style.visibility = "hidden";

(async function requireLogin() {
  try {
    const response = await fetch("/api/auth", {
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

  document.documentElement.style.visibility = "";
  const goLogin = window.confirm(
    "로그인이 필요한 기능입니다.\n로그인 페이지로 이동하시겠습니까?",
  );

  if (goLogin) {
    const returnUrl = `${window.location.pathname}${window.location.search}${window.location.hash}`;
    window.location.replace(`/pages/auth/login.html?redirect=${encodeURIComponent(returnUrl)}`);
    return;
  }

  window.location.replace(resolvePublicFallback());
})();

function resolvePublicFallback() {
  const path = window.location.pathname;
  if (path.startsWith("/pages/tier/")) return "/pages/tier/list.html";
  if (path.startsWith("/pages/worldcup/")) return "/pages/worldcup/list.html";
  return "/";
}
