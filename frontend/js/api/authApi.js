// ============================================
// 파일: js/api/authApi.js
// 목적: 인증(로그인/로그아웃/세션확인) 관련 API만 모음
//       회원가입/중복확인은 signupApi.js(또는 memberApi.js)로 별도 — 여기 안 섞음
// 지금 만드는 건 login.js가 실제로 쓰는 login()뿐,
// logout/checkSession은 나중에 그 화면 구현 시 추가
// ============================================

import { httpClient } from "./http.js";
import { ENDPOINTS } from "./endpoints.js";

export const authApi = {
    login: (data) => httpClient.post(ENDPOINTS.AUTH_SESSION, data),
    logout: () => httpClient.del(ENDPOINTS.AUTH_SESSION),
    checkSession: () => httpClient.get(ENDPOINTS.AUTH_SESSION),
};