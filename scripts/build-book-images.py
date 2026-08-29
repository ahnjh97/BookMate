from __future__ import annotations

import csv
import io
import ssl
import urllib.error
import urllib.request
import urllib.parse
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageFilter, ImageOps


ROOT = Path(__file__).resolve().parents[1]
BOOK_DIR = ROOT / "frontend" / "assets" / "images" / "books"
BOOK_CSV = ROOT / "db" / "books.csv"


@dataclass(frozen=True)
class BookSource:
    book_id: int
    title: str
    genre: str
    source: str


def csv_books() -> list[BookSource]:
    with BOOK_CSV.open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream))
    return [
        BookSource(int(row["book_id"]), row["title"], row["genre"], row["image_url"].strip())
        for row in rows
    ]


def open_source(book: BookSource) -> Image.Image:
    if book.source.startswith("/assets/"):
        path = ROOT / "frontend" / book.source.removeprefix("/")
        if path.exists():
            return Image.open(path)
        raise FileNotFoundError(f"로컬 표지 없음: {path}")
    if not book.source:
        raise FileNotFoundError("표지 주소 없음")

    urls = [book.source]
    if book.source.endswith("-M.jpg"):
        urls.insert(0, book.source[:-6] + "-L.jpg")
    last_error: Exception | None = None
    for url in urls:
        try:
            if urllib.parse.urlparse(url).hostname not in {
                "covers.openlibrary.org",
                "books.google.com",
                "contents.kyobobook.co.kr",
            }:
                raise RuntimeError(f"허용되지 않은 표지 호스트: {url}")
            request = urllib.request.Request(url, headers={"User-Agent": "BookMateCoverBuilder/1.0"})
            context = ssl._create_unverified_context()
            with urllib.request.urlopen(request, timeout=20, context=context) as response:
                return Image.open(io.BytesIO(response.read()))
        except (OSError, urllib.error.URLError) as error:
            last_error = error
    raise RuntimeError(str(last_error))


def normalized_cover(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    image = ImageOps.exif_transpose(image).convert("RGB")
    background = ImageOps.fit(image, size, method=Image.Resampling.LANCZOS)
    background = background.filter(ImageFilter.GaussianBlur(radius=max(10, size[0] // 18)))
    scale = min(size[0] / image.width, size[1] / image.height)
    image = image.resize(
        (max(1, round(image.width * scale)), max(1, round(image.height * scale))),
        Image.Resampling.LANCZOS,
    )
    x = (size[0] - image.width) // 2
    y = (size[1] - image.height) // 2
    background.paste(image, (x, y))
    return background


def build_book(book: BookSource) -> int:
    with open_source(book) as source:
        small = normalized_cover(source, (240, 360))
        large = normalized_cover(source, (520, 780))

    small.save(BOOK_DIR / f"{book.book_id}-240.webp", "WEBP", quality=73, method=6)
    large.save(BOOK_DIR / f"{book.book_id}-520.webp", "WEBP", quality=80, method=6)
    return book.book_id


def main() -> None:
    BOOK_DIR.mkdir(parents=True, exist_ok=True)
    books = csv_books()
    if len(books) != 1000:
        raise RuntimeError(f"전체 도서가 1,000권이 아닙니다: {len(books)}권")

    completed = 0
    with ThreadPoolExecutor(max_workers=12) as executor:
        futures = [executor.submit(build_book, book) for book in books]
        for future in as_completed(futures):
            future.result()
            completed += 1
            if completed % 100 == 0:
                print(f"표지 변환 {completed}/1000", flush=True)

    total_bytes = sum(path.stat().st_size for path in BOOK_DIR.glob("*.webp"))
    print(f"완료: 도서 이미지 2,000장, {total_bytes / 1024 / 1024:.1f}MB")


if __name__ == "__main__":
    main()
