(async function loadNavbar() {
  const navbarHosts = document.querySelectorAll("[data-navbar]");
  if (navbarHosts.length === 0) return;

  try {
    const response = await fetch("/components/navbar.html");
    if (!response.ok) throw new Error(`공통 navbar를 불러오지 못했습니다. (${response.status})`);
    const markup = await response.text();
    const currentPath = normalizePath(window.location.pathname);

    navbarHosts.forEach((host) => {
      host.innerHTML = markup;
      markCurrentPage(host, currentPath);
    });

    const authResponse = await fetch("/api/auth/session", { cache: "no-store" });
    const auth = authResponse.ok ? await authResponse.json() : { loggedIn: false };
    navbarHosts.forEach((host) => renderAuthMenu(host, auth));
    document.dispatchEvent(new CustomEvent("bookmate:navbar-ready", { detail: auth }));
  } catch (error) {
    console.error(error);
    navbarHosts.forEach((host) => {
      host.innerHTML = '<nav class="navbar"><a class="brand" href="/"><strong>BOOKMATE</strong></a></nav>';
    });
  }
})();

function renderAuthMenu(navbarHost, auth) {
  const menu = navbarHost.querySelector(".auth-menu");
  if (!menu) { return; }
  menu.replaceChildren();
  if (!auth.loggedIn) {
    menu.append(createLink("로그인", "/pages/auth/login.html", "login-link"));
    menu.append(createLink("회원가입", "/pages/auth/signup.html", "button button-small button-primary"));
    return;
  }
  // 관리자 / 일반회원 메뉴 분리
  if (auth.role === "ADMIN") {menu.append(createLink("관리자페이지", "/pages/admin/admin.html", "login-link admin-link"));
  } else { menu.append(createLink("마이페이지", "/pages/member/mypage.html", "login-link"));
  }
  menu.append(createLink("회원정보수정", "/pages/member/edit.html", "login-link account-edit-link"));
  const logout = document.createElement("button");
  logout.type = "button";
  logout.className = "button button-small button-primary";
  logout.textContent = "로그아웃";
  logout.addEventListener("click", async () => {
    logout.disabled = true;
    await fetch("/api/auth/session", { method: "DELETE" });
    window.location.href = "/";
  });
  menu.append(logout);
}

function createLink(label, href, className) {
  const link = document.createElement("a");
  link.textContent = label;
  link.href = href;
  link.className = className;
  return link;
}

function markCurrentPage(host, currentPath) {
  host.querySelectorAll("a[href]").forEach((link) => {
    const linkUrl = new URL(link.href, window.location.origin);
    const samePath = normalizePath(linkUrl.pathname) === currentPath;
    const sameSection = window.location.hash
      ? linkUrl.hash === window.location.hash
      : !linkUrl.hash;
    if (samePath && sameSection) link.setAttribute("aria-current", "page");
  });
}

function normalizePath(path) {
  return path.length > 1 ? path.replace(/\/+$/, "") : path;
}
