const picker = document.querySelector("#picker"),
  search = document.querySelector("#book-search"),
  count = document.querySelector("#count"),
  message = document.querySelector("#message"),
  selected = new Set();
let books = [];
async function load() {
  try {
    const r = await fetch("/api/books?size=100&sort=title"), d = await r.json();
    books = d.data?.books || [];
    render();
  } catch {
    message.textContent = "책 목록을 불러오지 못했습니다.";
  }
}
function render() {
  const q = search.value.trim().toLowerCase();
  picker.replaceChildren();
  books.filter(b => !q || b.title.toLowerCase().includes(q) || (b.authorName || "").toLowerCase().includes(q)).forEach(
    b => {
      const l = document.createElement("label");
      l.className = "worldcup-book-option";
      l.innerHTML = `${
        b.imageUrl ? `<img src="${esc(b.imageUrl)}" alt="" loading="lazy" decoding="async">` : "<i class=\"cover-placeholder\"></i>"
      }<span><strong>${esc(b.title)}</strong><small>${esc(b.authorName)}</small></span><input type="checkbox" ${
        selected.has(b.bookId) ? "checked" : ""
      }>`;
      l.querySelector("input").onchange = e => {
        e.target.checked ? selected.add(b.bookId) : selected.delete(b.bookId);
        count.textContent = `${selected.size}권 선택`;
      };
      picker.append(l);
    },
  );
}
search.oninput = render;
document.querySelector("#form").onsubmit = async e => {
  e.preventDefault();
  if (selected.size < 16) {
    message.textContent = "책을 16권 이상 선택해 주세요.";
    return;
  }
  const button = e.currentTarget.querySelector("button");
  button.disabled = true;
  try {
    const r = await fetch("/api/worldcup/templates", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({
          title: document.querySelector("#title").value,
          description: document.querySelector("#description").value,
          category: document.querySelector("#category").value,
          bookIds: [...selected],
        }),
      }),
      d = await r.json();
    if (!r.ok) throw new Error(d.message);
    location.href = `/pages/worldcup/play.html?id=${d.templateId}`;
  } catch (x) {
    message.textContent = x.message || "템플릿을 만들지 못했습니다.";
  } finally {
    button.disabled = false;
  }
};
function esc(v) {
  return String(v ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
    "\"",
    "&quot;",
  );
}
load();
