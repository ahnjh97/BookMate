(function initializeListPagination(global) {
  function create(options) {
    const root = options.root;
    const previousButton = root?.querySelector("[data-pagination-prev]");
    const nextButton = root?.querySelector("[data-pagination-next]");
    const numbersElement = root?.querySelector("[data-pagination-numbers]");
    const pageSize = Math.max(1, Number(options.pageSize) || 1);
    let items = [];
    let currentPage = 1;

    if (!root || !previousButton || !nextButton || !numbersElement) {
      throw new Error("페이지 이동 요소가 올바르게 구성되지 않았습니다.");
    }

    function render() {
      const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
      currentPage = Math.min(currentPage, totalPages);
      const startIndex = (currentPage - 1) * pageSize;

      previousButton.disabled = currentPage <= 1;
      nextButton.disabled = currentPage >= totalPages;
      numbersElement.replaceChildren();

      const groupStart = Math.floor((currentPage - 1) / 10) * 10 + 1;
      const groupEnd = Math.min(groupStart + 9, totalPages);
      for (let page = groupStart; page <= groupEnd; page += 1) {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = String(page);
        button.classList.toggle("is-active", page === currentPage);
        button.setAttribute("aria-current", page === currentPage ? "page" : "false");
        button.addEventListener("click", () => moveTo(page));
        numbersElement.append(button);
      }

      options.onRender(items.slice(startIndex, startIndex + pageSize), startIndex, items.length);
      root.hidden = items.length === 0;
    }

    function moveTo(page) {
      const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
      const nextPage = Math.min(Math.max(1, page), totalPages);
      if (nextPage === currentPage) return;
      currentPage = nextPage;
      render();
      options.scrollTarget?.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    previousButton.addEventListener("click", () => moveTo(currentPage - 1));
    nextButton.addEventListener("click", () => moveTo(currentPage + 1));

    return {
      setItems(nextItems) {
        items = Array.isArray(nextItems) ? nextItems : [];
        currentPage = 1;
        render();
      },
    };
  }

  global.BookMateListPagination = { create };
})(window);
