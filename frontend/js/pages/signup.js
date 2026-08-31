// ============================================
// 파일: signup.js
// ============================================

import { memberApi } from "/js/api/memberApi.js";

const signupForm = document.querySelector("#signup-form");
const idInput = document.querySelector("#login-id");
const checkButton = document.querySelector("#check-id");
const signupMessage = document.querySelector("#form-message");
let checkedLoginId = "";

idInput.addEventListener("input", () => { checkedLoginId = ""; });

checkButton.addEventListener("click", async () => {
  if (!idInput.reportValidity()) return;
  setMessage("확인 중...", "");

  try {
    const result = await memberApi.checkLoginId(idInput.value);
    if (!result.available) throw new Error("이미 사용 중인 아이디입니다.");
    checkedLoginId = idInput.value;
    setMessage("사용할 수 있는 아이디입니다.", "success");
  } catch (error) {
    checkedLoginId = "";
    setMessage(error.message || "아이디를 확인하지 못했습니다.", "error");
  }
});

signupForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (checkedLoginId !== idInput.value) {
    setMessage("아이디 중복 확인을 먼저 해 주세요.", "error");
    return;
  }
  const submit = signupForm.querySelector('[type="submit"]');
  submit.disabled = true;

  try {
    const data = Object.fromEntries(new FormData(signupForm));
    await memberApi.signup(data);
    window.location.href = "/pages/auth/login.html";
  } catch (error) {
    setMessage(error.message, "error");
    submit.disabled = false;
  }
});

function setMessage(message, type) {
  signupMessage.textContent = message;
  signupMessage.className = `form-message ${type}`.trim();
}
