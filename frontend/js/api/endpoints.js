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

    // member
    MEMBER_ME: "/api/members/me",
    MEMBER_CHECK_NICKNAME: "/api/members/check-nickname",
    MEMBER_CHECK_EMAIL: "/api/members/check-email",
    MEMBER_PASSWORD: "/api/members/password",

    // admin
    ADMIN_MEMBERS: "/api/admin/members",
    ADMIN_MEMBERS_LOCK: "/api/admin/members/lock",
    ADMIN_POSTS: "/api/posts",
    ADMIN_POSTS_PIN: "/api/admin/posts/pin",
    ADMIN_POSTS_DELETE: "/api/admin/posts/delete",
    ADMIN_TIER_TEMPLATES: "/api/admin/tier-templates",
    ADMIN_WORLDCUP_TEMPLATES: "/api/admin/worldcup-templates",
};