(function initializeBookshelfVisit(global) {
  function closeAll() {
    document.querySelectorAll(".member-bookshelf-prompt:not([hidden])").forEach(prompt => {
      prompt.hidden = true;
    });
  }

  function render(container, nickname, memberId) {
    container.replaceChildren();
    container.classList.add("member-bookshelf-control");

    const nicknameButton = document.createElement("button");
    nicknameButton.type = "button";
    nicknameButton.className = "member-bookshelf-nickname";
    nicknameButton.textContent = nickname || "알 수 없음";
    nicknameButton.title = nickname || "알 수 없음";

    const prompt = document.createElement("span");
    prompt.className = "member-bookshelf-prompt";
    prompt.hidden = true;

    const visitLink = document.createElement("a");
    visitLink.href = `/pages/member/bookshelf.html?memberId=${Number(memberId)}`;
    visitLink.textContent = "책장 방문하기";
    prompt.append(visitLink);

    nicknameButton.addEventListener("click", event => {
      event.stopPropagation();
      const willOpen = prompt.hidden;
      closeAll();
      prompt.hidden = !willOpen;
    });
    prompt.addEventListener("click", event => event.stopPropagation());
    container.append(nicknameButton, prompt);
  }

  document.addEventListener("click", closeAll);
  global.BookMateBookshelfVisit = { render, closeAll };
})(window);
