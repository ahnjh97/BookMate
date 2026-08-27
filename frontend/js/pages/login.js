const loginForm = document.querySelector("#login-form");
const loginMessage = document.querySelector("#form-message");

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const submit = loginForm.querySelector('[type="submit"]');
  submit.disabled = true;
  loginMessage.textContent = "";
  try {
    const data = Object.fromEntries(new FormData(loginForm));
    const response = await fetch("/api/auth", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.message || "로그인에 실패했습니다.");
    window.location.href = "/";
  } catch (error) {
    loginMessage.textContent = error.message;
    loginMessage.className = "form-message error";
    submit.disabled = false;
  }
});
