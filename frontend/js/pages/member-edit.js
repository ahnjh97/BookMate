import { memberApi } from "/js/api/memberApi.js";

const accountForm = document.getElementById("account-edit-form");
const loginIdInput = document.getElementById("account-login-id");
const nicknameInput = document.getElementById("account-nickname");
const emailInput = document.getElementById("account-email");
const authorSection = document.getElementById("account-author-section");
const selfIntroInput = document.getElementById("account-self-intro");
const nicknameHint = document.getElementById("account-nickname-hint");
const emailHint = document.getElementById("account-email-hint");
const accountMessage = document.getElementById("account-message");
const passwordForm = document.getElementById("password-edit-form");
const currentPasswordInput = document.getElementById("password-current");
const newPasswordInput = document.getElementById("password-new");
const confirmPasswordInput = document.getElementById("password-confirm");
const passwordMessage = document.getElementById("password-message");

let member = null;
let checkedNickname = "";
let checkedEmail = "";

async function initialize() {
  try {
    member = await memberApi.getMe();
    loginIdInput.value = member.loginId || "";
    nicknameInput.value = member.nickname || "";
    emailInput.value = member.email || "";
    checkedNickname = nicknameInput.value;
    checkedEmail = emailInput.value;
    if (member.isAuthor) {
      authorSection.hidden = false;
      selfIntroInput.value = member.selfIntro || "";
    }
  } catch (error) {
    if (error.status === 401) {
      location.replace("/pages/auth/login.html");
      return;
    }
    showMessage(accountMessage, error.message || "회원정보를 불러오지 못했습니다.", "error");
    accountForm.querySelectorAll("input, textarea, button").forEach(element => { element.disabled = true; });
  }
}

nicknameInput.addEventListener("input", () => {
  checkedNickname = nicknameInput.value === member?.nickname ? nicknameInput.value : "";
  clearHint(nicknameHint);
});

emailInput.addEventListener("input", () => {
  checkedEmail = emailInput.value === member?.email ? emailInput.value : "";
  clearHint(emailHint);
});

document.getElementById("account-check-nickname").addEventListener("click", async () => {
  const nickname = nicknameInput.value.trim();
  if (nickname === member?.nickname) {
    checkedNickname = nickname;
    showHint(nicknameHint, "현재 사용 중인 닉네임입니다.", "success");
    return;
  }
  await checkAvailability(
    () => memberApi.checkNickname(nickname),
    nicknameHint,
    () => { checkedNickname = nickname; },
    "사용 가능한 닉네임입니다.",
  );
});

document.getElementById("account-check-email").addEventListener("click", async () => {
  const email = emailInput.value.trim();
  if (email.toLowerCase() === String(member?.email || "").toLowerCase()) {
    checkedEmail = email;
    showHint(emailHint, "현재 사용 중인 이메일입니다.", "success");
    return;
  }
  await checkAvailability(
    () => memberApi.checkEmail(email),
    emailHint,
    () => { checkedEmail = email; },
    "사용 가능한 이메일입니다.",
  );
});

accountForm.addEventListener("submit", async event => {
  event.preventDefault();
  const nickname = nicknameInput.value.trim();
  const email = emailInput.value.trim();
  if (checkedNickname !== nickname) {
    showMessage(accountMessage, "닉네임 중복 확인을 먼저 해주세요.", "error");
    return;
  }
  if (checkedEmail.toLowerCase() !== email.toLowerCase()) {
    showMessage(accountMessage, "이메일 중복 확인을 먼저 해주세요.", "error");
    return;
  }
  const submit = accountForm.querySelector("button[type=submit]");
  submit.disabled = true;
  try {
    const result = await memberApi.updateMe({
      nickname,
      email,
      selfIntro: member.isAuthor ? selfIntroInput.value : undefined,
    });
    member = { ...member, nickname, email };
    checkedNickname = nickname;
    checkedEmail = email;
    showMessage(accountMessage, result.message || "변경사항이 저장되었습니다.", "success");
  } catch (error) {
    showMessage(accountMessage, error.message || "회원정보를 수정하지 못했습니다.", "error");
  } finally {
    submit.disabled = false;
  }
});

passwordForm.addEventListener("submit", async event => {
  event.preventDefault();
  if (newPasswordInput.value !== confirmPasswordInput.value) {
    showMessage(passwordMessage, "새 비밀번호가 일치하지 않습니다.", "error");
    return;
  }
  const submit = passwordForm.querySelector("button[type=submit]");
  submit.disabled = true;
  try {
    const result = await memberApi.changePassword({
      currentPassword: currentPasswordInput.value,
      newPassword: newPasswordInput.value,
    });
    passwordForm.reset();
    showMessage(passwordMessage, result.message || "비밀번호가 변경되었습니다.", "success");
  } catch (error) {
    showMessage(passwordMessage, error.message || "비밀번호를 변경하지 못했습니다.", "error");
  } finally {
    submit.disabled = false;
  }
});

async function checkAvailability(request, hint, onAvailable, successMessage) {
  showHint(hint, "확인 중입니다.");
  try {
    const result = await request();
    if (!result.available) {
      showHint(hint, "이미 사용 중입니다.", "error");
      return;
    }
    onAvailable();
    showHint(hint, successMessage, "success");
  } catch (error) {
    showHint(hint, error.message || "중복 여부를 확인하지 못했습니다.", "error");
  }
}

function showHint(element, text, state = "") {
  element.textContent = text;
  if (state) element.dataset.state = state;
  else delete element.dataset.state;
}

function clearHint(element) {
  element.textContent = "";
  delete element.dataset.state;
}

function showMessage(element, text, state) {
  element.textContent = text;
  element.dataset.state = state;
}

initialize();
