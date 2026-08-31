(function initializeLoginPrompt(global) {
  function show(options = {}) {
    const message = options.message || "로그인이 필요한 기능입니다.";
    const goLogin = global.confirm(`${message}\n로그인 페이지로 이동하시겠습니까?`);

    if (!goLogin) return false;

    const returnUrl = options.returnUrl ||
      `${global.location.pathname}${global.location.search}${global.location.hash}`;
    global.location.href = `/pages/auth/login.html?redirect=${encodeURIComponent(returnUrl)}`;
    return true;
  }

  global.BookMateLoginPrompt = { show };
})(window);
