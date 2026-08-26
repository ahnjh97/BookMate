(async function loadNavbar() {
  const navbarHosts = document.querySelectorAll("[data-navbar]");
  if (navbarHosts.length === 0) return;

  try {
    const response = await fetch("/bookmate/components/navbar.html");
    if (!response.ok) {
      throw new Error(`공통 navbar를 불러오지 못했습니다. (${response.status})`);
    }

    const navbarMarkup = await response.text();
    const currentPath = normalizePath(window.location.pathname);

    navbarHosts.forEach((host) => {
      host.innerHTML = navbarMarkup;
      markCurrentPage(host, currentPath);
    });

    await Promise.all(Array.from(navbarHosts, configureDevAuth));

    document.dispatchEvent(new CustomEvent("bookmate:navbar-ready"));
  } catch (error) {
    console.error(error);
    navbarHosts.forEach((host) => {
      host.innerHTML = `
        <nav class="navbar" aria-label="주요 메뉴">
          <a class="logo" href="/bookmate/">BookMate</a>
        </nav>
      `;
    });
  }
})();

function markCurrentPage(navbarHost, currentPath) {
  navbarHost.querySelectorAll("a[href]").forEach((link) => {
    const linkPath = normalizePath(new URL(link.href, window.location.origin).pathname);
    if (linkPath === currentPath) {
      link.setAttribute("aria-current", "page");
    }
  });
}

function normalizePath(path) {
  return path.length > 1 ? path.replace(/\/+$/, "") : path;
}

async function configureDevAuth(navbarHost) {
  // TODO: 실제 로그인 UI와 세션 확인 API가 연결되면 개발용 인증 처리 전체를 삭제합니다.
  const loginLink = navbarHost.querySelector('.auth-menu a[href="/bookmate/pages/auth/login.html"]');
  if (!loginLink) return;

  try {
    const response = await fetch("/bookmate/api/dev/auth");
    if (!response.ok) return;

    const result = await response.json();
    if (result.loggedIn) {
      renderDevLogout(navbarHost, result.nickname);
      return;
    }

    loginLink.addEventListener("click", async (event) => {
      event.preventDefault();
      loginLink.textContent = "로그인 중...";

      try {
        const loginResponse = await fetch("/bookmate/api/dev/auth", { method: "POST" });
        const loginResult = await loginResponse.json();
        if (!loginResponse.ok || !loginResult.success) {
          throw new Error(loginResult.message || "개발용 로그인에 실패했습니다.");
        }
        renderDevLogout(navbarHost, loginResult.nickname);
        document.dispatchEvent(new CustomEvent("bookmate:auth-changed", {
          detail: { loggedIn: true }
        }));
      } catch (error) {
        window.location.href = loginLink.href;
      }
    }, { once: true });
  } catch (error) {
    // 일반 실행 모드에서는 기존 로그인 링크를 그대로 사용합니다.
  }
}

function renderDevLogout(navbarHost, nickname = "개발회원") {
  const authMenu = navbarHost.querySelector(".auth-menu");
  authMenu.replaceChildren();

  const memberLabel = document.createElement("span");
  memberLabel.className = "dev-member-label";
  memberLabel.textContent = `${nickname} 로그인 중`;

  const logoutButton = document.createElement("button");
  logoutButton.className = "button";
  logoutButton.type = "button";
  logoutButton.textContent = "로그아웃";
  logoutButton.addEventListener("click", async () => {
    await fetch("/bookmate/api/dev/auth", { method: "DELETE" });
    window.location.reload();
  });

  authMenu.append(memberLabel, logoutButton);
}
