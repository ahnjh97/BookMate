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
