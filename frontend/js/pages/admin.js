/*
 * =========================================
 * BookMate 관리자 페이지
 * =========================================
 */

import { authApi } from "/js/api/authApi.js";

let loginAdminMemberId = null;
const memberMap = new Map();
let allMembers = [];
let selectedMemberFilter = "active";
let selectedPostFilter = "public";
const deleteStates = new Map();
let memberPagination;
let postPagination;
let commentPagination;
let bookPagination;
let tierPagination;
let worldcupPagination;

document.addEventListener("DOMContentLoaded", async () => {
  const allowed = await checkAdminAccess();
  if (!allowed) return;

  initializePagination();
  initializeDeleteControls();
  initializeMemberFilters();
  initializePostFilters();
  await loadMembers();
  await loadPosts(1);
  await loadComments();
  await loadBookRequests();
  await loadTierTemplates();
  await loadWorldcupTemplates();

});

function initializePagination() {
  memberPagination = BookMateListPagination.create({
    root: document.getElementById("member-pagination"),
    pageSize: 10,
    onRender: renderMembers,
  });
  postPagination = BookMateListPagination.create({
    root: document.getElementById("post-pagination"),
    pageSize: 10,
    onRender: renderPosts,
    onPageChange: loadPosts,
  });
  commentPagination = BookMateListPagination.create({
    root: document.getElementById("comment-pagination"),
    pageSize: 5,
    onRender: renderComments,
  });
  bookPagination = BookMateListPagination.create({
    root: document.getElementById("book-pagination"),
    pageSize: 5,
    onRender: renderBookRequests,
  });
  tierPagination = BookMateListPagination.create({
    root: document.getElementById("tier-pagination"),
    pageSize: 5,
    onRender: renderTierTemplates,
  });
  worldcupPagination = BookMateListPagination.create({
    root: document.getElementById("worldcup-pagination"),
    pageSize: 5,
    onRender: renderWorldcupTemplates,
  });
}

function initializeMemberFilters() {
  document.querySelectorAll("[data-member-filter]").forEach(button => {
    button.addEventListener("click", () => {
      selectedMemberFilter = button.dataset.memberFilter;
      document.querySelectorAll("[data-member-filter]").forEach(item => {
        const active = item === button;
        item.classList.toggle("is-active", active);
        item.setAttribute("aria-pressed", String(active));
      });
      renderFilteredMembers();
    });
  });
}

function renderFilteredMembers() {
  const filtered = allMembers.filter(member => {
    if (selectedMemberFilter === "admin") return member.role === "ADMIN";
    if (selectedMemberFilter === "active") return member.role !== "ADMIN" && member.isLocked !== "Y";
    if (selectedMemberFilter === "restricted") return member.role !== "ADMIN" && member.isLocked === "Y";
    return false;
  });
  memberPagination.setItems(filtered);
}

function initializePostFilters() {
  document.querySelectorAll("[data-post-filter]").forEach(button => {
    button.addEventListener("click", async () => {
      selectedPostFilter = button.dataset.postFilter;
      document.querySelectorAll("[data-post-filter]").forEach(item => {
        const active = item === button;
        item.classList.toggle("is-active", active);
        item.setAttribute("aria-pressed", String(active));
      });
      cancelDeleteMode("post");
      await loadPosts(1);
    });
  });
}

function initializeDeleteControls() {
  document.querySelectorAll("[data-delete-controls]").forEach(controls => {
    const type = controls.dataset.deleteControls;
    const section = controls.closest(".admin-section");
    const deleteButton = controls.querySelector(".admin-delete-start");
    const cancelButton = controls.querySelector(".admin-delete-cancel");
    const state = { active: false, selected: new Set(), section, deleteButton, cancelButton };
    deleteStates.set(type, state);
    deleteButton.addEventListener("click", () => handleDeleteButton(type));
    cancelButton.addEventListener("click", () => cancelDeleteMode(type));
  });
}

function prepareSelectableRow(row, type, id, deletable = true) {
  row.dataset.itemId = String(id);
  row.dataset.deletable = String(deletable);
  if (!deletable) row.classList.add("is-not-deletable");
  const state = deleteStates.get(type);
  row.classList.toggle("is-selected", Boolean(state?.selected.has(Number(id))));
  row.setAttribute("aria-selected", String(Boolean(state?.selected.has(Number(id)))));
  row.addEventListener("click", event => {
    if (!state?.active || !deletable || event.target.closest("button, a, input, select")) return;
    const itemId = Number(row.dataset.itemId);
    if (state.selected.has(itemId)) state.selected.delete(itemId);
    else state.selected.add(itemId);
    row.classList.toggle("is-selected", state.selected.has(itemId));
    row.setAttribute("aria-selected", String(state.selected.has(itemId)));
    updateDeleteControls(type);
  });
}

async function handleDeleteButton(type) {
  const state = deleteStates.get(type);
  if (!state.active) {
    deleteStates.forEach((otherState, otherType) => {
      if (otherType !== type && otherState.active) cancelDeleteMode(otherType);
    });
    state.active = true;
    state.section.classList.add("is-selection-mode");
    state.cancelButton.hidden = false;
    updateDeleteControls(type);
    showAdminToast("삭제할 항목들을 선택하세요.", "delete");
    return;
  }
  if (state.selected.size === 0) {
    showAdminToast("삭제할 항목들을 선택하세요.", "delete");
    return;
  }
  if (!confirm(`선택한 ${state.selected.size}개 항목을 삭제하시겠습니까?`)) return;

  state.deleteButton.disabled = true;
  try {
    let deletedCount = 0;
    for (const postId of state.selected) {
      const response = await fetch("/api/admin/posts/delete", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ postId }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || "게시글을 삭제하지 못했습니다.");
      deletedCount += 1;
    }
    cancelDeleteMode(type);
    showAdminToast(`${deletedCount}개 게시글을 삭제했습니다.`, "success");
    await loadPosts(1);
  } catch (error) {
    console.error("관리자 선택 삭제 실패:", error);
    showAdminToast(error.message || "선택한 게시글을 삭제하지 못했습니다.", "error");
    await loadPosts(1);
  } finally {
    state.deleteButton.disabled = false;
  }
}

function cancelDeleteMode(type) {
  const state = deleteStates.get(type);
  state.active = false;
  state.selected.clear();
  state.section.classList.remove("is-selection-mode");
  state.section.querySelectorAll("tbody tr").forEach(row => {
    row.classList.remove("is-selected");
    row.setAttribute("aria-selected", "false");
  });
  state.cancelButton.hidden = true;
  updateDeleteControls(type);
}

function updateDeleteControls(type) {
  const state = deleteStates.get(type);
  state.deleteButton.textContent = state.active && state.selected.size
    ? `선택 항목 삭제 (${state.selected.size})`
    : state.active ? "선택 항목 삭제" : "삭제 모드";
}

function showAdminToast(message, state = "success") {
  document.querySelector(".admin-live-toast")?.remove();
  const toast = document.createElement("p");
  toast.className = "flash-toast admin-live-toast";
  toast.dataset.state = state;
  toast.setAttribute("role", "status");
  toast.setAttribute("aria-live", "polite");
  toast.textContent = message;
  document.body.append(toast);
  requestAnimationFrame(() => { toast.dataset.visible = "true"; });
  window.setTimeout(() => {
    toast.dataset.visible = "false";
    window.setTimeout(() => toast.remove(), 250);
  }, 2400);
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
    tierPagination.setItems(data.templates || []);
  } catch (error) {
    console.error(error);
    showMessage("티어 템플릿 신청을 불러오지 못했습니다.");
  }
}

function renderTierTemplates(templates) {
  const tbody = document.getElementById("tier-table-body");
  tbody.innerHTML = templates.length
      ? ""
      : "<tr><td colspan=\"8\" class=\"admin-empty\">신청된 템플릿이 없습니다.</td></tr>";
    templates.forEach(template => {
      const row = document.createElement("tr");
      const pending = template.status === "PENDING";
      row.innerHTML = `<td>${template.templateId}</td><td>${escapeHtml(template.creatorNickname)}</td><td><span class="admin-category-badge ${getCategoryBadgeClass(template.category)}">${
        escapeHtml(getCategoryLabel(template.category))
      }</span></td><td>${escapeHtml(template.title)}</td><td>${template.itemCount}권</td><td><span class="admin-badge ${getStatusBadgeClass(template.status)}">${
        escapeHtml(getStatusLabel(template.status))
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
    worldcupPagination.setItems(data.templates || []);
  } catch (error) {
    console.error(error);
    showMessage("월드컵 템플릿 신청을 불러오지 못했습니다.");
  }
}

function renderWorldcupTemplates(templates) {
  const tbody = document.getElementById("worldcup-table-body");
  tbody.innerHTML = templates.length
      ? ""
      : '<tr><td colspan="8" class="admin-empty">신청된 월드컵 템플릿이 없습니다.</td></tr>';
    templates.forEach(template => {
      const row = document.createElement("tr");
      const pending = template.status === "PENDING";
      row.innerHTML = `<td>${template.templateId}</td><td>${escapeHtml(template.creatorNickname)}</td>`
        + `<td><span class="admin-category-badge ${getCategoryBadgeClass(template.category)}">${escapeHtml(getCategoryLabel(template.category))}</span></td><td>${escapeHtml(template.title)}</td>`
        + `<td>${template.itemCount}권</td><td><span class="admin-badge ${getStatusBadgeClass(template.status)}">${escapeHtml(getStatusLabel(template.status))}</span></td>`
        + `<td>${formatValue(template.requestedAt)}</td><td>${pending
          ? `<button class="admin-action-button" onclick="reviewWorldcupTemplate(${template.templateId}, true)">승인</button>
             <button class="admin-action-button admin-action-danger" onclick="reviewWorldcupTemplate(${template.templateId}, false)">반려</button>`
          : "처리 완료"}</td>`;
      tbody.append(row);
    });
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
 * GET /api/auth/session
 * =========================================
 */
async function checkAdminAccess() {
  try {
    // 수정: authApi.checkSession()이 credentials/헤더 등을 대신 처리
    const auth = await authApi.checkSession().catch(() => null);

    if (!auth || !auth.loggedIn) {
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

    allMembers = members;
    renderFilteredMembers();
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
            <td colspan="8" class="admin-empty">
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
                    class="admin-action-button ${locked ? "admin-state-release" : "admin-state-activate"}"
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
                <span class="admin-badge ${getRoleBadgeClass(member.role)}">
                    ${escapeHtml(getRoleLabel(member.role))}
                </span>
            </td>
            <td>${formatValue(member.failCount)}</td>
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
async function loadPosts(page = 1) {
  try {
    const params = new URLSearchParams({ page: String(page), size: "10", filter: selectedPostFilter });
    const response = await fetch(`/api/admin/posts?${params}`, {
      method: "GET",
      credentials: "include",
    });

    const data = await response.json();

    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }

    postPagination.setPage(data.posts || [], data.page || page, data.totalCount || 0);
  } catch (error) {
    console.error("게시글 목록 조회 실패:", error);
    showMessage("게시글 목록을 불러오는 중 오류가 발생했습니다.");
  }
}

async function loadBookRequests() {
  try {
    const response = await fetch("/api/admin/book-requests", { credentials: "include" });
    const data = await response.json();
    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }
    bookPagination.setItems(data.requests || []);
  } catch (error) {
    console.error("책 등록 신청 조회 실패:", error);
    showMessage("책 등록 신청을 불러오지 못했습니다.");
  }
}

function renderBookRequests(requests) {
  const tbody = document.getElementById("book-table-body");
  tbody.innerHTML = "";
  if (requests.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" class="admin-empty">신청된 책이 없습니다.</td></tr>';
    return;
  }
  requests.forEach(request => {
    const row = document.createElement("tr");
    const pending = request.status === "PENDING";
    const genreGroup = window.BookMateGenre.groupOf(request.genre);
    row.innerHTML = `<td>${request.requestId}</td><td>${escapeHtml(request.requesterNickname)}</td>`
      + `<td>${escapeHtml(request.title)}</td><td>${escapeHtml(request.authorName)}</td>`
      + `<td><span class="admin-category-badge genre-badge" data-genre="${escapeHtml(genreGroup)}">${escapeHtml(genreGroup)}</span></td>`
      + `<td><span class="admin-badge ${getStatusBadgeClass(request.status)}">${escapeHtml(getStatusLabel(request.status))}</span></td>`
      + `<td>${formatValue(request.requestedAt)}</td><td>${pending
        ? `<button class="admin-action-button" onclick="reviewBookRequest(${request.requestId}, true)">승인</button>
           <button class="admin-action-button admin-action-danger" onclick="reviewBookRequest(${request.requestId}, false)">반려</button>`
        : "처리 완료"}</td>`;
    tbody.append(row);
  });
}

async function reviewBookRequest(requestId, approved) {
  const reason = approved ? "" : prompt("반려 사유를 입력해 주세요.");
  if (!approved && !reason) return;
  if (approved && !confirm(`${requestId}번 책 등록 신청을 승인할까요?`)) return;
  try {
    const response = await fetch("/api/admin/book-requests", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ requestId, approved, reason }),
    });
    const data = await response.json();
    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }
    showMessage(data.message);
    await loadBookRequests();
  } catch (error) {
    console.error("책 등록 신청 검토 실패:", error);
    showMessage("책 등록 신청 검토 결과를 저장하지 못했습니다.");
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
            <td colspan="8" class="admin-empty">
                게시글이 없습니다.
            </td>
        `;

    tbody.appendChild(row);
    return;
  }

  posts.forEach(post => {
    const row = document.createElement("tr");
    const pinned = post.isPinned === "Y";
    const active = post.status === "ACTIVE";
    const hidden = post.status === "HIDDEN_BY_WRITER" || post.status === "HIDDEN";
    prepareSelectableRow(row, "post", post.postId, active || hidden);

    row.innerHTML = `
            <td>${post.postId}</td>
            <td>${escapeHtml(post.memberNickname)}</td>
            <td>${escapeHtml(getCategoryLabel(post.category))}</td>
            <td>${escapeHtml(post.title)}</td>
            <td>${formatValue(post.viewCount)}</td>
            <td><span class="admin-badge ${active ? "admin-post-public" : "admin-post-private"}">${active ? "공개" : "비공개"}</span></td>
            <td>${formatValue(post.createdAt)}</td>
            <td>
                ${active ? `<button
                    type="button"
                    class="admin-action-button ${pinned ? "admin-state-release" : "admin-state-activate"}"
                    onclick="changePostPin(${post.postId}, ${!pinned})"
                >
                    ${pinned ? "고정 해제" : "상단 고정"}
                </button>` : "처리 완료"}

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

async function loadComments() {
  const tbody = document.getElementById("comment-table-body");
  if (!tbody) return;

  try {
    const response = await fetch("/api/admin/comments", {
      credentials: "include",
    });
    const data = await response.json();
    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }
    commentPagination.setItems(data.comments || []);
  } catch (error) {
    console.error("관리자 댓글 목록 조회 실패:", error);
    showMessage("댓글 목록을 불러오지 못했습니다.");
  }
}

function renderComments(comments) {
  const tbody = document.getElementById("comment-table-body");
  if (!tbody) return;

  tbody.innerHTML = "";
  if (comments.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="admin-empty">등록된 댓글이 없습니다.</td></tr>`;
    return;
  }

  comments.forEach(comment => {
    const row = document.createElement("tr");
    const active = comment.status === "ACTIVE";
    row.innerHTML = `
      <td>${comment.commentId}</td>
      <td>${comment.postId}</td>
      <td>${escapeHtml(comment.memberNickname)}</td>
      <td class="admin-comment-content">${escapeHtml(comment.content)}</td>
      <td><span class="admin-badge ${getStatusBadgeClass(comment.status)}">${escapeHtml(getStatusLabel(comment.status))}</span></td>
      <td>${formatValue(comment.createdAt)}</td>
      <td>${active
        ? `<button type="button" class="admin-action-button admin-action-danger" data-comment-delete="${comment.commentId}">삭제</button>`
        : "처리 완료"}
      </td>
    `;
    row.querySelector("[data-comment-delete]")?.addEventListener("click", () => {
      deleteCommentByAdmin(comment.commentId);
    });
    tbody.appendChild(row);
  });
}

async function deleteCommentByAdmin(commentId) {
  if (!confirm(`댓글 ${commentId}번을 삭제하시겠습니까?`)) return;

  try {
    const response = await fetch("/api/admin/comments", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ commentId }),
    });
    const data = await response.json();
    if (!response.ok) {
      handleApiError(response.status, data.message);
      return;
    }
    showAdminToast(data.message || "댓글을 삭제했습니다.");
    await loadComments();
  } catch (error) {
    console.error("관리자 댓글 삭제 실패:", error);
    showMessage("댓글을 삭제하지 못했습니다.");
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

function getRoleBadgeClass(role) {
  return role === "ADMIN" ? "admin-role-admin" : "admin-role-user";
}

function getRoleLabel(role) {
  return {
    ADMIN: "관리자",
    USER: "일반 회원",
  }[role] || "일반 회원";
}

function getStatusBadgeClass(status) {
  return `admin-status-${String(status || "unknown").toLowerCase()}`;
}

function getStatusLabel(status) {
  return {
    PENDING: "미승인",
    APPROVED: "승인 완료",
    REJECTED: "반려",
    ACTIVE: "정상",
    DELETED: "삭제",
    HIDDEN: "숨김",
    INACTIVE: "비활성",
    HIDDEN_BY_WRITER: "작성자 숨김",
    DELETED_BY_WRITER: "작성자 삭제",
    DELETED_BY_ADMIN: "관리자 삭제",
  }[status] || "상태 미확인";
}

function getCategoryLabel(category) {
  return {
    FREE: "자유",
    RECOMMEND: "추천",
    REVIEW: "후기",
    NOTICE: "공지",
    GENRE: "장르",
    AUTHOR: "작가",
    SERIES: "시리즈",
    THEME: "테마",
    TIER: "티어리스트",
    IDEAL: "이상형월드컵",
    WORLDCUP: "이상형월드컵",
  }[category] || category;
}

function getCategoryBadgeClass(category) {
  const label = getCategoryLabel(category);
  return {
    "장르": "category-genre",
    "시리즈": "category-series",
    "작가": "category-author",
    "테마": "category-theme",
  }[label] || "category-default";
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

window.changeMemberLock = changeMemberLock;
window.changePostPin = changePostPin;
window.reviewBookRequest = reviewBookRequest;
window.reviewTierTemplate = reviewTierTemplate;
window.reviewWorldcupTemplate = reviewWorldcupTemplate;
