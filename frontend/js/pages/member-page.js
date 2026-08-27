(async function protectMemberPage() {
  try {
    const response = await fetch("/api/auth", { cache: "no-store" });
    const auth = await response.json();
    if (!response.ok || !auth.loggedIn) {
      window.location.replace("/pages/auth/login.html");
      return;
    }
    document.querySelector("[data-member-page]")?.removeAttribute("hidden");
    document.querySelectorAll("[data-member-nickname]").forEach((element) => {
      element.textContent = auth.nickname || auth.loginId;
    });
  } catch (error) {
    window.location.replace("/pages/auth/login.html");
  }
})();
