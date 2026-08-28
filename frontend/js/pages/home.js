const numberFormatter = new Intl.NumberFormat("ko-KR");

loadSummary();
loadHomeBooks();

async function loadSummary() {
  try {
    const summaryResponse = await fetch("/api/home/summary");
    const summaryResult = await summaryResponse.json();
    if (summaryResponse.ok && summaryResult.success) renderSummary(summaryResult.data);
  } catch (error) {
    console.error("홈 통계를 불러오지 못했습니다.", error);
  }
}

async function loadHomeBooks() {
  try {
    const booksResponse = await fetch("/api/books?page=1&size=100");
    const booksResult = await booksResponse.json();
    if (!booksResponse.ok || !booksResult.success) throw new Error(booksResult.message);
    renderHomeBooks(booksResult.data.books);

    const bookStat = document.querySelector('[data-home-stat="books"]');
    if (bookStat.textContent === "-") {
      bookStat.textContent = `${numberFormatter.format(booksResult.data.books.length)}${booksResult.data.hasMore ? "+" : ""}`;
    }
  } catch (error) {
    console.error("홈 도서를 불러오지 못했습니다.", error);
  }
}

function renderSummary(summary) {
  document.querySelector('[data-home-stat="books"]').textContent = numberFormatter.format(summary.bookCount);
  document.querySelector('[data-home-stat="ratings"]').textContent = numberFormatter.format(summary.ratingCount);
  document.querySelector('[data-home-stat="tiers"]').textContent = numberFormatter.format(summary.tierListCount);
}

function renderHomeBooks(books) {
  const mosaic = document.querySelector("#home-book-mosaic");
  mosaic.replaceChildren();

  books.slice(0, 6).forEach((book, index) => {
    const link = document.createElement("a");
    link.className = `cover cover-${index + 1}`;
    link.href = `/pages/book/detail.html?id=${encodeURIComponent(book.bookId)}`;
    link.setAttribute("aria-label", `${book.title} 상세 보기`);
    appendCover(link, book);
    mosaic.append(link);
  });

}

function appendCover(container, book) {
  if (book.imageUrl) {
    const image = document.createElement("img");
    image.src = book.imageUrl;
    image.alt = `${book.title} 표지`;
    image.loading = "lazy";
    image.addEventListener("error", () => {
      image.remove();
      container.append(createCoverTitle(book.title));
    }, { once: true });
    container.append(image);
    return;
  }
  container.append(createCoverTitle(book.title));
}

function createCoverTitle(title) {
  const label = document.createElement("span");
  label.textContent = title;
  return label;
}
