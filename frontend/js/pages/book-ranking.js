const filterElement = document.querySelector("#ranking-filter");
const genreElement = document.querySelector("#ranking-genre");
const sortElement = document.querySelector("#ranking-sort");
const minimumElement = document.querySelector("#ranking-minimum");
const statusElement = document.querySelector("#ranking-status");
const listElement = document.querySelector("#ranking-list");

async function loadRankings() {
  const params = new URLSearchParams({
    genre: genreElement.value,
    sort: sortElement.value,
    minimumRatings: minimumElement.value,
    limit: "20"
  });
  statusElement.textContent = "랭킹을 불러오는 중입니다.";
  delete statusElement.dataset.state;
  listElement.replaceChildren();

  try {
    const response = await fetch(`/api/books/rankings?${params}`);
    const result = await response.json();
    if (!response.ok || !result.success) throw new Error(result.message || "책 랭킹을 불러오지 못했습니다.");
    renderRankings(result.data);
  } catch (error) {
    statusElement.textContent = error.message || "책 랭킹을 불러오지 못했습니다.";
    statusElement.dataset.state = "error";
  }
}

function renderRankings(books) {
  if (books.length === 0) {
    statusElement.textContent = "선택한 조건을 충족하는 책이 없습니다. 최소 평가 인원을 낮춰보세요.";
    return;
  }
  statusElement.textContent = `${genreElement.value} 장르에서 ${books.length}권을 표시하고 있습니다.`;

  books.forEach((book, index) => {
    const item = document.createElement("li");
    item.className = "ranking-item";
    const rank = document.createElement("strong");
    rank.className = "ranking-number";
    rank.textContent = String(index + 1);

    const cover = document.createElement("div");
    cover.className = "ranking-cover";
    if (book.imageUrl) {
      const image = document.createElement("img");
      image.src = book.imageUrl;
      image.alt = `${book.title} 표지`;
      image.loading = "lazy";
      image.addEventListener("error", () => {
        image.remove();
        cover.textContent = book.title;
      }, { once: true });
      cover.append(image);
    } else {
      cover.textContent = book.title;
    }

    const content = document.createElement("div");
    content.className = "ranking-content";
    const title = document.createElement("a");
    title.href = `/pages/book/detail.html?id=${encodeURIComponent(book.bookId)}`;
    title.textContent = book.title;
    const author = document.createElement("p");
    author.textContent = book.authorName;
    const metrics = document.createElement("p");
    metrics.className = "ranking-metrics";
    metrics.textContent = `★ ${Number(book.averageRating).toFixed(1)} · ${book.ratingCount}명 참여`;
    content.append(title, author, metrics);
    item.append(rank, cover, content);
    listElement.append(item);
  });
}

filterElement.addEventListener("change", loadRankings);
loadRankings();
