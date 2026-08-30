// 파일: js/api/endpoints.js
// URL 문자열을 코드 곳곳에 흩어두지 않고 상수로 보관
// → 나중에 "/api/members" 경로가 바뀌어도 이 파일 한 줄만 수정
export const ENDPOINTS = {
    MEMBER_SIGNUP: "/members",
    MEMBER_LOGIN: "/members/login",
    MEMBER_LOGOUT: "/members/logout",
    
    // auth
    AUTH_SESSION: "/api/auth",   // GET(세션확인)/POST(로그인)/DELETE(로그아웃) 공용 — AuthController
    AUTH_SIGNUP: "/api/auth/signup", // GET(중복확인)/POST(회원가입) — SignupController
    // ?? - 추가 예정
};