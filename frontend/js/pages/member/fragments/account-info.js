// ============================================
// 파일: pages/member/fragments/account-info.js
//
// [목적]
//   마이페이지 "내정보" 탭 — 닉네임/이메일/작가소개 조회 및 수정
//
// [진입 조건]
//   mypage.js가 사이드바 "내정보" 클릭 시 이 fragment(HTML+JS)를 로드하고
//   init()을 호출함. 로그인 안 되어 있으면 애초에 mypage 진입 자체가
//   auth-guard에서 막히므로, 이 파일은 로그인된 상태만 가정하고 짜여 있음.
//
// [데이터 흐름]
//   1. window.__memberAuth : 로그인 시점에 세션에서 받은 값(로그인여부 확인용)
//   2. memberApi.getMe()   : email, selfIntro 등 세션에 없는 상세정보를 추가 조회
//      → 화면에 실제로 표시하는 값은 전부 2번(getMe) 결과 기준
//
// [중복확인 UX 패턴]
//   닉네임/이메일 입력값이 바뀔 때마다 checkedNickname/checkedEmail을 비움
//   → "중복확인" 버튼을 눌러 통과해야만 그 값이 checked 변수에 채워짐
//   → 저장 시 입력값과 checked 값이 다르면(=확인 안 하고 바꿈) 저장을 막음
//   회원가입 화면(signup.js)과 동일한 패턴을 재사용함
//
// [작가 소개 섹션]
//   detail.isAuthor가 true인 회원(AUTHOR_ACCOUNT 인증회원)에게만 노출됨
//
// [연동 API — 전부 완료 상태]
//   GET  /api/members/me             상세 조회
//   PUT  /api/members/me             저장
//   GET  /api/members/check-nickname 닉네임 중복확인(본인 제외)
//   GET  /api/members/check-email    이메일 중복확인(본인 제외)
// ============================================

import { memberApi } from "/js/api/memberApi.js";

export async function init() {
    const auth = window.__memberAuth;
    if (!auth) return;

    let detail;
    try {
        detail = await memberApi.getMe();
    } catch (error) {
        console.error("내정보를 불러오지 못했습니다.", error);
        return;
    }

    const loginIdInput = document.querySelector("#account-login-id");
    const nicknameInput = document.querySelector("#account-nickname");
    const emailInput = document.querySelector("#account-email");
    const authorSection = document.querySelector("#account-author-section");
    const selfIntroInput = document.querySelector("#account-self-intro");

    loginIdInput.value = detail.loginId ?? "";
    nicknameInput.value = detail.nickname ?? "";
    emailInput.value = detail.email ?? "";

    if (detail.isAuthor) {
        authorSection.hidden = false;
        selfIntroInput.value = detail.selfIntro ?? "";
    }

    // 현재값은 이미 서버에 저장된 본인 값이므로 확인된 상태로 시작
    let checkedNickname = nicknameInput.value;
    let checkedEmail = emailInput.value;

    // 값이 바뀌면 재확인 필요 — 확인 상태 초기화
    nicknameInput.addEventListener("input", () => { checkedNickname = ""; });
    emailInput.addEventListener("input", () => { checkedEmail = ""; });

    document.querySelector("#account-check-nickname").addEventListener("click", async () => {
        const hint = document.querySelector("#account-nickname-hint");

        if (nicknameInput.value === detail.nickname) {
            checkedNickname = nicknameInput.value;
            hint.textContent = "현재 닉네임과 동일합니다.";
            hint.className = "hint";
            return;
        }

        hint.textContent = "확인 중...";
        try {
            const result = await memberApi.checkNickname(nicknameInput.value);
            if (result.available) {
                checkedNickname = nicknameInput.value;
                hint.textContent = "사용 가능한 닉네임입니다.";
                hint.className = "hint ok";
            } else {
                hint.textContent = "이미 사용중인 닉네임입니다.";
                hint.className = "hint err";
            }
        } catch (error) {
            hint.textContent = error.message;
            hint.className = "hint err";
        }
    });

    document.querySelector("#account-check-email").addEventListener("click", async () => {
        const hint = document.querySelector("#account-email-hint");

        if (emailInput.value === detail.email) {
            checkedEmail = emailInput.value;
            hint.textContent = "현재 이메일과 동일합니다.";
            hint.className = "hint";
            return;
        }

        hint.textContent = "확인 중...";
        try {
            const result = await memberApi.checkEmail(emailInput.value);
            if (result.available) {
                checkedEmail = emailInput.value;
                hint.textContent = "사용 가능한 이메일입니다.";
                hint.className = "hint ok";
            } else {
                hint.textContent = "이미 사용중인 이메일입니다.";
                hint.className = "hint err";
            }
        } catch (error) {
            hint.textContent = error.message;
            hint.className = "hint err";
        }
    });

    document.querySelector("#account-save").addEventListener("click", async () => {
        const message = document.querySelector("#account-message");

        // 중복확인을 거치지 않은 값으로 저장 시도하면 차단
        if (checkedNickname !== nicknameInput.value) {
            message.textContent = "닉네임 중복확인을 먼저 해주세요.";
            message.className = "status-msg error show";
            return;
        }
        if (checkedEmail !== emailInput.value) {
            message.textContent = "이메일 중복확인을 먼저 해주세요.";
            message.className = "status-msg error show";
            return;
        }

        try {
            await memberApi.updateMe({
                nickname: nicknameInput.value,
                email: emailInput.value,
                selfIntro: detail.isAuthor ? selfIntroInput.value : undefined,
            });
            // 저장 성공 시 profile 탭(초기화면)으로 이동
            // mypage.js가 popstate 이벤트를 듣고 있으므로, URL을 바꾸면 자동으로 화면 전환됨
            history.pushState({ key: "profile" }, "", "/pages/member/mypage.html?tab=profile");
            window.dispatchEvent(new PopStateEvent("popstate", { state: { key: "profile" } }));
        } catch (error) {
            message.textContent = error.message;
            message.className = "status-msg error show";
        }
    });
}