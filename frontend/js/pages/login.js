const loginForm = document.querySelector("#login-form");
const loginMessage = document.querySelector("#form-message");

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const submit = loginForm.querySelector('[type="submit"]');
  submit.disabled = true;
  loginMessage.textContent = "";
  try {
    const data = Object.fromEntries(new FormData(loginForm));
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.message || "로그인에 실패했습니다.");
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
