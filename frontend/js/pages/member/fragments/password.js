// ============================================
// 파일: pages/member/fragments/password.js
//
// [목적] 비밀번호 변경 폼 — 현재비번 재인증 + 새비번/확인비번 대조
//
// [검증 순서]
//   1. 새비번과 확인비번이 서로 같은지(프론트에서만 확인, 사용자 오타 방지용)
//   2. 서버에 현재비번+새비번 전송 → 서버가 현재비번을 실제로 검증(재인증)
//      → 여기서 틀리면 401 에러가 오고, 그대로 화면에 메시지 표시
// ============================================

import { memberApi } from "/js/api/memberApi.js";

export function init() {
    const currentInput = document.querySelector("#password-current");
    const newInput = document.querySelector("#password-new");
    const confirmInput = document.querySelector("#password-confirm");
    const message = document.querySelector("#password-message");

    document.querySelector("#password-save").addEventListener("click", async () => {
        if (newInput.value !== confirmInput.value) {
            message.textContent = "새 비밀번호가 일치하지 않습니다.";
            message.className = "status-msg error show";
            return;
        }

        try {
            await memberApi.changePassword({
                currentPassword: currentInput.value,
                newPassword: newInput.value,
            });
            message.textContent = "비밀번호가 변경되었습니다.";
            message.className = "status-msg success show";
            currentInput.value = "";
            newInput.value = "";
            confirmInput.value = "";
        } catch (error) {
            message.textContent = error.message;
            message.className = "status-msg error show";
        }
    });
}