/*
 * =========================================
 * BookMate 관리자 페이지
 * =========================================
 */

let loginAdminMemberId = null;
const memberMap = new Map();

document.addEventListener("DOMContentLoaded", async () => {
  const allowed = await checkAdminAccess();
  if (!allowed) return;

  await loadMembers();
  await loadPosts();
  await loadBookRequests();
  await loadTierTemplates();
  await loadWorldcupTemplates();

  const memberRefreshButton = document.getElementById("member-refresh-button");
  const postRefreshButton = document.getElementById("post-refresh-button");
  const tierRefreshButton = document.getElementById("tier-refresh-button");
  const bookRequestRefreshButton = document.getElementById("book-request-refresh-button");
  const worldcupRefreshButton = document.getElementById("worldcup-refresh-button");

  if (memberRefreshButton) {
    memberRefreshButton.addEventListener("click", loadMembers);
  }

  if (postRefreshButton) {
    postRefreshButton.addEventListener("click", loadPosts);
  }
  if (tierRefreshButton) tierRefreshButton.addEventListener("click", loadTierTemplates);
  if (bookRequestRefreshButton) bookRequestRefreshButton.addEventListener("click", loadBookRequests);
  if (worldcupRefreshButton) worldcupRefreshButton.addEventListener("click", loadWorldcupTemplates);
});

async function loadBookRequests() {
  const tbody = document.getElementById("book-request-table-body");
  if (!tbody) return;
  try {
    const response = await fetch("/api/admin/book-requests", { credentials: "include" });
    const data = await response.json();
    if (!response.ok) { handleApiError(response.status, data.message); return; }
    const requests = data.requests || [];
    tbody.innerHTML = requests.length ? "" : '<tr><td colspan="8" class="admin-empty">책 등록 신청이 없습니다.</td></tr>';
    requests.forEach(request => {
      const row = document.createElement("tr");
      const pending = request.status === "PENDING";
      row.innerHTML = `<td>${request.requestId}</td><td>${escapeHtml(request.requesterNickname)}</td>`
        + `<td>${escapeHtml(request.isbn)}</td><td><a href="${escapeHtml(request.imageUrl)}" target="_blank" rel="noopener">${escapeHtml(request.title)}</a><br><small>${escapeHtml(request.authorName)}</small></td>`
        + `<td>${escapeHtml(request.genre)}</td><td>${formatValue(request.publishedDate)}</td><td><span class="admin-badge">${escapeHtml(request.status)}</span></td>`
        + `<td>${pending ? `<button class="admin-action-button" onclick="reviewBookRequest(${request.requestId},true)">승인</button> <button class="admin-action-button admin-action-danger" onclick="reviewBookRequest(${request.requestId},false)">반려</button>` : "처리 완료"}</td>`;
      tbody.append(row);
    });
  } catch (error) { console.error(error); showMessage("책 등록 신청을 불러오지 못했습니다."); }
}

async function reviewBookRequest(requestId, approved) {
  const reason = approved ? "" : prompt("반려 사유를 입력해 주세요.");
  if (!approved && !reason) return;
  if (approved && !confirm(`${requestId}번 책을 등록할까요?`)) return;
  try {
    const response = await fetch("/api/admin/book-requests", { method: "POST", headers: { "Content-Type": "application/json" }, credentials: "include", body: JSON.stringify({ requestId, approved, reason }) });
    const data = await response.json();
    if (!response.ok) { handleApiError(response.status, data.message); return; }
    showMessage(data.message);
    await loadBookRequests();
  } catch (error) { console.error(error); showMessage("책 등록 신청을 처리하지 못했습니다."); }
}

async function loadTierTemplates() {
  const tbody = document.getElementById("tier-table-body");
  if (!tbody) return;
  try {
    const response = await fetch("/api/admin/tier-templates", { credentials: "include" });
    const data = await response.json();
    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }
    const templates = data.templates || [];
    tbody.innerHTML = templates.length
      ? ""
      : "<tr><td colspan=\"8\" class=\"admin-empty\">신청된 템플릿이 없습니다.</td></tr>";
    templates.forEach(template => {
      const row = document.createElement("tr");
      const pending = template.status === "PENDING";
      row.innerHTML = `<td>${template.templateId}</td><td>${escapeHtml(template.creatorNickname)}</td><td>${
        escapeHtml(template.category)
      }</td><td>${escapeHtml(template.title)}</td><td>${template.itemCount}권</td><td><span class="admin-badge">${
        escapeHtml(template.status)
      }</span></td><td>${formatValue(template.requestedAt)}</td><td>${
        pending
          ? `<button class="admin-action-button"
                     onclick="reviewTierTemplate(${template.templateId}, true)">승인</button>
             <button class="admin-action-button admin-action-danger"
                     onclick="reviewTierTemplate(${template.templateId}, false)">반려</button>`
          : "처리 완료"
      }</td>`;
      tbody.append(row);
    });
  } catch (error) {
    console.error(error);
    showMessage("티어 템플릿 신청을 불러오지 못했습니다.");
  }
}

async function reviewTierTemplate(templateId, approved) {
  const reason = approved ? "" : prompt("반려 사유를 입력해 주세요.");
  if (!approved && !reason) return;
  if (approved && !confirm(`${templateId}번 템플릿을 공개 승인할까요?`)) return;
  try {
    const response = await fetch("/api/admin/tier-templates", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ templateId, approved, reason }),
    });
    const data = await response.json();
    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }
    showMessage(data.message);
    await loadTierTemplates();
  } catch (error) {
    console.error(error);
    showMessage("검토 결과를 저장하지 못했습니다.");
  }
}

async function loadWorldcupTemplates() {
  const tbody = document.getElementById("worldcup-table-body");
  if (!tbody) return;
  try {
    const response = await fetch("/api/admin/worldcup-templates", { credentials: "include" });
    const data = await response.json();
    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }
    const templates = data.templates || [];
    tbody.innerHTML = templates.length
      ? ""
      : '<tr><td colspan="8" class="admin-empty">신청된 월드컵 템플릿이 없습니다.</td></tr>';
    templates.forEach(template => {
      const row = document.createElement("tr");
      const pending = template.status === "PENDING";
      row.innerHTML = `<td>${template.templateId}</td><td>${escapeHtml(template.creatorNickname)}</td>`
        + `<td>${escapeHtml(template.category)}</td><td>${escapeHtml(template.title)}</td>`
        + `<td>${template.itemCount}권</td><td><span class="admin-badge">${escapeHtml(template.status)}</span></td>`
        + `<td>${formatValue(template.requestedAt)}</td><td>${pending
          ? `<button class="admin-action-button" onclick="reviewWorldcupTemplate(${template.templateId}, true)">승인</button>
             <button class="admin-action-button admin-action-danger" onclick="reviewWorldcupTemplate(${template.templateId}, false)">반려</button>`
          : "처리 완료"}</td>`;
      tbody.append(row);
    });
  } catch (error) {
    console.error(error);
    showMessage("월드컵 템플릿 신청을 불러오지 못했습니다.");
  }
}

async function reviewWorldcupTemplate(templateId, approved) {
  const reason = approved ? "" : prompt("반려 사유를 입력해 주세요.");
  if (!approved && !reason) return;
  if (approved && !confirm(`${templateId}번 월드컵 템플릿을 공개 승인할까요?`)) return;
  try {
    const response = await fetch("/api/admin/worldcup-templates", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ templateId, approved, reason }),
    });
    const data = await response.json();
    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }
    showMessage(data.message);
    await loadWorldcupTemplates();
  } catch (error) {
    console.error(error);
    showMessage("월드컵 템플릿 검토 결과를 저장하지 못했습니다.");
  }
}

/*
 * =========================================
 * 관리자 페이지 접근 권한 확인
 * GET /api/auth
 * =========================================
 */
async function checkAdminAccess() {
  try {
    const response = await fetch("/api/auth", {
      method: "GET",
      cache: "no-store",
      credentials: "include",
    });

    if (!response.ok) {
      window.location.href = "/pages/auth/login.html";
      return false;
    }

    const auth = await response.json();

    if (!auth.loggedIn) {
      alert("로그인이 필요합니다.");
      window.location.href = "/pages/auth/login.html";
      return false;
    }

    if (auth.role !== "ADMIN") {
      alert("관리자만 접근할 수 있습니다.");
      window.location.href = "/";
      return false;
    }

    loginAdminMemberId = Number(auth.memberId);
    return true;
  } catch (error) {
    console.error("관리자 권한 확인 실패:", error);
    alert("관리자 권한을 확인하지 못했습니다.");
    return false;
  }
}

/*
 * =========================================
 * 회원 목록 조회
 * GET /api/admin/members
 * =========================================
 */
async function loadMembers() {
  try {
    const response = await fetch("/api/admin/members", {
      method: "GET",
      credentials: "include",
    });

    const data = await response.json();

    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }

    const members = data.members || [];

    memberMap.clear();

    members.forEach(member => {
      memberMap.set(Number(member.memberId), member);
    });

    renderMembers(members);
  } catch (error) {
    console.error("회원 목록 조회 실패:", error);
    showMessage("회원 목록을 불러오는 중 오류가 발생했습니다.");
  }
}

/*
 * =========================================
 * 회원 목록 화면 출력
 * =========================================
 */
function renderMembers(members) {
  const tbody = document.getElementById("member-table-body");

  if (!tbody) {
    console.error("member-table-body 요소를 찾을 수 없습니다.");
    return;
  }

  tbody.innerHTML = "";

  if (members.length === 0) {
    const row = document.createElement("tr");

    row.innerHTML = `
            <td colspan="9" class="admin-empty">
                등록된 회원이 없습니다.
            </td>
        `;

    tbody.appendChild(row);
    return;
  }

  members.forEach(member => {
    const row = document.createElement("tr");

    const locked = member.isLocked === "Y";
    const isAdmin = member.role === "ADMIN";
    const isSelf = Number(member.memberId) === loginAdminMemberId;

    let managementHtml;

    if (isAdmin) {
      managementHtml = `
                <span class="admin-self-label">
                    ${isSelf ? "현재 관리자" : "관리자 계정"}
                </span>
            `;
    } else {
      managementHtml = `
                <button
                    type="button"
                    class="admin-action-button"
                    onclick="changeMemberLock(${member.memberId}, ${!locked})"
                >
                    ${locked ? "잠금 해제" : "회원 잠금"}
                </button>
            `;
    }

    row.innerHTML = `
            <td>${member.memberId}</td>
            <td>${escapeHtml(member.loginId)}</td>
            <td>${escapeHtml(member.nickname)}</td>
            <td>${escapeHtml(member.email)}</td>
            <td>
                <span class="admin-badge">
                    ${escapeHtml(member.role)}
                </span>
            </td>
            <td>${formatValue(member.failCount)}</td>
            <td>${locked ? "잠금" : "정상"}</td>
            <td>${formatValue(member.createdAt)}</td>
            <td>${managementHtml}</td>
        `;

    tbody.appendChild(row);
  });
}

/*
 * =========================================
 * 회원 잠금 / 잠금 해제
 * POST /api/admin/members/lock
 * =========================================
 */
async function changeMemberLock(memberId, locked) {
  const targetMember = memberMap.get(Number(memberId));

  if (targetMember && targetMember.role === "ADMIN") {
    showMessage("관리자 계정은 잠금 또는 잠금 해제할 수 없습니다.");
    return;
  }

  if (Number(memberId) === loginAdminMemberId) {
    showMessage("관리자는 자신의 계정을 잠글 수 없습니다.");
    return;
  }

  const actionName = locked ? "잠금" : "잠금 해제";
  const confirmed = confirm(`회원 ${memberId}번을 ${actionName}하시겠습니까?`);

  if (!confirmed) return;

  try {
    const response = await fetch("/api/admin/members/lock", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify({
        memberId: memberId,
        locked: locked,
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }

    showMessage(data.message || "회원 상태가 변경되었습니다.");
    await loadMembers();
  } catch (error) {
    console.error("회원 상태 변경 실패:", error);
    showMessage("회원 상태 변경 중 오류가 발생했습니다.");
  }
}

/*
 * =========================================
 * 게시글 목록 조회
 * GET /api/posts
 * =========================================
 */
async function loadPosts() {
  try {
    const response = await fetch("/api/posts", {
      method: "GET",
      credentials: "include",
    });

    const data = await response.json();

    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }

    renderPosts(data.posts || []);
  } catch (error) {
    console.error("게시글 목록 조회 실패:", error);
    showMessage("게시글 목록을 불러오는 중 오류가 발생했습니다.");
  }
}

/*
 * =========================================
 * 게시글 목록 화면 출력
 * =========================================
 */
function renderPosts(posts) {
  const tbody = document.getElementById("post-table-body");

  if (!tbody) {
    console.error("post-table-body 요소를 찾을 수 없습니다.");
    return;
  }

  tbody.innerHTML = "";

  if (posts.length === 0) {
    const row = document.createElement("tr");

    row.innerHTML = `
            <td colspan="9" class="admin-empty">
                게시글이 없습니다.
            </td>
        `;

    tbody.appendChild(row);
    return;
  }

  posts.forEach(post => {
    const row = document.createElement("tr");
    const pinned = post.isPinned === "Y";

    row.innerHTML = `
            <td>${post.postId}</td>
            <td>${escapeHtml(post.memberNickname)}</td>
            <td>${escapeHtml(post.category)}</td>
            <td>${escapeHtml(post.title)}</td>
            <td>${formatValue(post.viewCount)}</td>
            <td>${pinned ? "고정" : "-"}</td>
            <td>${escapeHtml(post.status)}</td>
            <td>${formatValue(post.createdAt)}</td>
            <td>
                <button
                    type="button"
                    class="admin-action-button"
                    onclick="changePostPin(${post.postId}, ${!pinned})"
                >
                    ${pinned ? "고정 해제" : "상단 고정"}
                </button>

                <button
                    type="button"
                    class="admin-action-button admin-action-danger"
                    onclick="deletePostByAdmin(${post.postId})"
                >
                    관리자 삭제
                </button>
            </td>
        `;

    tbody.appendChild(row);
  });
}

/*
 * =========================================
 * 게시글 상단 고정 / 해제
 * POST /api/admin/posts/pin
 * =========================================
 */
async function changePostPin(postId, pinned) {
  const actionName = pinned ? "상단 고정" : "고정 해제";
  const confirmed = confirm(`게시글 ${postId}번을 ${actionName}하시겠습니까?`);

  if (!confirmed) return;

  try {
    const response = await fetch("/api/admin/posts/pin", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify({
        postId: postId,
        pinned: pinned,
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }

    showMessage(data.message || "게시글 고정 상태가 변경되었습니다.");
    await loadPosts();
  } catch (error) {
    console.error("게시글 고정 상태 변경 실패:", error);
    showMessage("게시글 고정 상태 변경 중 오류가 발생했습니다.");
  }
}

/*
 * =========================================
 * 관리자 게시글 삭제
 * POST /api/admin/posts/delete
 * =========================================
 */
async function deletePostByAdmin(postId) {
  const confirmed = confirm(
    `게시글 ${postId}번을 삭제하시겠습니까?\n`
      + "삭제된 게시글은 일반 목록에서 보이지 않습니다.",
  );

  if (!confirmed) return;

  try {
    const response = await fetch("/api/admin/posts/delete", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify({
        postId: postId,
      }),
    });

    const data = await response.json();

    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }

    showMessage(data.message || "게시글이 삭제되었습니다.");
    await loadPosts();
  } catch (error) {
    console.error("관리자 게시글 삭제 실패:", error);
    showMessage("게시글 삭제 중 오류가 발생했습니다.");
  }
}

/*
 * =========================================
 * API 오류 공통 처리
 * =========================================
 */
function handleApiError(status, message) {
  if (status === 401) {
    showMessage(message || "로그인이 필요합니다.");
    return;
  }

  if (status === 403) {
    showMessage(message || "관리자 권한이 필요합니다.");
    return;
  }

  if (status === 404) {
    showMessage(message || "대상을 찾을 수 없습니다.");
    return;
  }

  if (status === 400) {
    showMessage(message || "잘못된 요청입니다.");
    return;
  }

  showMessage(message || "서버 오류가 발생했습니다.");
}

/*
 * =========================================
 * 메시지 표시
 * =========================================
 */
function showMessage(message) {
  const messageElement = document.getElementById("admin-message");

  if (!messageElement) {
    console.warn("admin-message 요소를 찾을 수 없습니다.", message);
    return;
  }

  messageElement.textContent = message || "";
}

/*
 * =========================================
 * null / undefined 표시 방지
 * =========================================
 */
function formatValue(value) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  return value;
}

/*
 * =========================================
 * HTML 삽입 방지
 * =========================================
 */
function escapeHtml(value) {
  if (value === null || value === undefined) {
    return "";
  }

  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");
}
