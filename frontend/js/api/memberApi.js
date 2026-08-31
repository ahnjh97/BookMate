// ============================================
// 파일: js/api/memberApi.js (신규 생성)
// 목적: 회원가입/정보수정/마이페이지 관련 API 모음 (authApi와 역할 분리)
// ============================================

import { httpClient } from "./http.js";
import { ENDPOINTS } from "./endpoints.js";

export const memberApi = {
    getMe: () => httpClient.get(ENDPOINTS.MEMBER_ME),
    updateMe: (data) => httpClient.put(ENDPOINTS.MEMBER_ME, data),
    checkNickname: (nickname) => httpClient.get(`${ENDPOINTS.MEMBER_CHECK_NICKNAME}?nickname=${encodeURIComponent(nickname)}`),
    checkEmail: (email) => httpClient.get(`${ENDPOINTS.MEMBER_CHECK_EMAIL}?email=${encodeURIComponent(email)}`),
    changePassword: (data) => httpClient.put(ENDPOINTS.MEMBER_PASSWORD, data),
    checkLoginId: (loginId) => httpClient.get(`${ENDPOINTS.AUTH_SIGNUP}?loginId=${encodeURIComponent(loginId)}`),
    signup: (data) => httpClient.post(ENDPOINTS.AUTH_SIGNUP, data),
};