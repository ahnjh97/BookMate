(function initializeFlashToast() {
  const storageKey = "bookmate:flash-toast";
  let flash;
  try {
    flash = JSON.parse(sessionStorage.getItem(storageKey));
    sessionStorage.removeItem(storageKey);
  } catch (error) {
    sessionStorage.removeItem(storageKey);
    return;
  }
  if (!flash?.message) return;

  const toast = document.createElement("p");
  toast.className = "flash-toast";
  toast.dataset.state = flash.state || "success";
  toast.setAttribute("role", "status");
  toast.setAttribute("aria-live", "polite");
  toast.textContent = flash.message;
  document.body.append(toast);

  requestAnimationFrame(() => {
    toast.dataset.visible = "true";
  });
  window.setTimeout(() => {
    toast.dataset.visible = "false";
    window.setTimeout(() => toast.remove(), 250);
  }, 3600);
})();
