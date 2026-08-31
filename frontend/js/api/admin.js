// ============================================
// 파일: js/api/adminApi.js
// 목적: 관리자 전용 API 모음 — 회원잠금, 게시글관리, 템플릿승인
// 무시해도 무방 공수가 적게들어 임시로 만들어둠 백엔드컨트롤러에서 한번 재확인 필요
// ============================================

import { httpClient } from "./http.js";
import { ENDPOINTS } from "./endpoints.js";

export const adminApi = {
    getMembers: () => httpClient.get(ENDPOINTS.ADMIN_MEMBERS),
    setMemberLock: (memberId, locked) => httpClient.post(ENDPOINTS.ADMIN_MEMBERS_LOCK, { memberId, locked }),

    getPosts: () => httpClient.get(ENDPOINTS.ADMIN_POSTS),
    setPostPin: (postId, pinned) => httpClient.post(ENDPOINTS.ADMIN_POSTS_PIN, { postId, pinned }),
    deletePost: (postId) => httpClient.post(ENDPOINTS.ADMIN_POSTS_DELETE, { postId }),

    getTierTemplates: () => httpClient.get(ENDPOINTS.ADMIN_TIER_TEMPLATES),
    reviewTierTemplate: (templateId, approved, reason) =>
        httpClient.post(ENDPOINTS.ADMIN_TIER_TEMPLATES, { templateId, approved, reason }),

    getWorldcupTemplates: () => httpClient.get(ENDPOINTS.ADMIN_WORLDCUP_TEMPLATES),
    reviewWorldcupTemplate: (templateId, approved, reason) =>
        httpClient.post(ENDPOINTS.ADMIN_WORLDCUP_TEMPLATES, { templateId, approved, reason }),
};