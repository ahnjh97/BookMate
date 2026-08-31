// 파일: js/api/http.js
// 공통 fetch 래퍼. 응답 형식: {success, message, ...값들이 최상위에 그대로}
// (data로 안 감쌈 — 팀원 컨트롤러들 형식에 맞춤, 2026-08-25)
//
// 사용법: 직접 안 쓰고 authApi.js 등에서 감싸서 씀
//   const result = await httpClient.post("/api/auth", {loginId, password});
//   result.memberId  // 바로 꺼냄
//
// 실패 시 throw됨 → try/catch로 e.message 사용, 필요시 e.status로 분기

const BASE_URL = "";

async function request(method, path, body = null) {
    const options = {
        method,
        headers: { "Content-Type": "application/json" },
        credentials: "include",
    };
    if (body) options.body = JSON.stringify(body);

    const res = await fetch(BASE_URL + path, options);
    const json = await res.json();

    if (!json.success) {
        const error = new Error(json.message || "요청 처리에 실패했습니다.");
        error.status = res.status;
        throw error;
    }

    return json;
}

export const httpClient = {
    get: (path) => request("GET", path),
    post: (path, body) => request("POST", path, body),
    put: (path, body) => request("PUT", path, body),
    del: (path) => request("DELETE", path),
};