(function initializeFlashToast() {
  const storageKey = "bookmate:flash-toast";

  function show(message, state = "success") {
    if (!message) return;
    document.querySelector(".flash-toast[data-bookmate-toast]")?.remove();

    const toast = document.createElement("p");
    toast.className = "flash-toast";
    toast.dataset.bookmateToast = "true";
    toast.dataset.state = state;
    toast.setAttribute("role", state === "warning" ? "alert" : "status");
    toast.setAttribute("aria-live", state === "warning" ? "assertive" : "polite");
    toast.textContent = message;
    document.body.append(toast);

    requestAnimationFrame(() => {
      toast.dataset.visible = "true";
    });
    window.setTimeout(() => {
      toast.dataset.visible = "false";
      window.setTimeout(() => toast.remove(), 250);
    }, 3600);
  }

  window.BookMateToast = { show };

  let flash;
  try {
    flash = JSON.parse(sessionStorage.getItem(storageKey));
    sessionStorage.removeItem(storageKey);
  } catch (error) {
    sessionStorage.removeItem(storageKey);
    return;
  }
  if (!flash?.message) return;
  show(flash.message, flash.state || "success");
})();
