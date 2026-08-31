package service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dto.CatalogBookDTO;
import util.ProjectPaths;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

public class AladinCatalogService {
    private static final URI SEARCH_ENDPOINT = URI.create("https://www.aladin.co.kr/ttb/api/ItemSearch.aspx");
    private static final URI LOOKUP_ENDPOINT = URI.create("https://www.aladin.co.kr/ttb/api/ItemLookUp.aspx");

    private final Gson gson = new Gson();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final String apiKey = loadApiKey();

    public List<CatalogBookDTO> search(String rawQuery) {
        String query = required(rawQuery, "검색어", 100);
        if (query.length() < 2) throw new IllegalArgumentException("검색어를 두 글자 이상 입력해 주세요.");

        String possibleIsbn = query.replaceAll("[^0-9]", "");
        if (possibleIsbn.length() == 13) {
            try {
                return List.of(findCompleteBook(possibleIsbn));
            } catch (NoSuchElementException | IllegalArgumentException exception) {
                return List.of();
            }
        }

        URI uri = withQuery(SEARCH_ENDPOINT,
                "ttbkey", apiKey,
                "Query", query,
                "QueryType", "Keyword",
                "MaxResults", "20",
                "start", "1",
                "SearchTarget", "Book",
                "output", "js",
                "Version", "20131101",
                "Cover", "Big");

        JsonArray items = requestItems(uri);
        List<CatalogBookDTO> books = new ArrayList<>();
        for (JsonElement item : items) {
            CatalogBookDTO book = mapCompleteBook(item.getAsJsonObject());
            if (book != null) books.add(book);
        }
        return books;
    }

    public CatalogBookDTO findCompleteBook(String rawIsbn) {
        String isbn = BookRequestService.normalizeIsbn(rawIsbn);
        URI uri = withQuery(LOOKUP_ENDPOINT,
                "ttbkey", apiKey,
                "itemIdType", "ISBN13",
                "ItemId", isbn,
                "output", "js",
                "Version", "20131101",
                "Cover", "Big");
        JsonArray items = requestItems(uri);
        if (items.isEmpty()) throw new NoSuchElementException("알라딘에서 해당 ISBN의 책을 찾을 수 없습니다.");
        CatalogBookDTO book = mapCompleteBook(items.get(0).getAsJsonObject());
        if (book == null) throw new IllegalArgumentException("필수 정보가 모두 등록된 책만 신청할 수 있습니다.");
        return book;
    }

    private JsonArray requestItems(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header("User-Agent", "BookMate/1.0")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalStateException("도서 검색 서비스가 응답하지 않습니다.");
            }
            JsonObject body = gson.fromJson(response.body(), JsonObject.class);
            if (body == null) throw new IllegalStateException("도서 검색 응답을 해석하지 못했습니다.");
            if (body.has("errorCode")) {
                throw new IllegalStateException("알라딘 API 설정을 확인해 주세요.");
            }
            return body.has("item") && body.get("item").isJsonArray()
                    ? body.getAsJsonArray("item")
                    : new JsonArray();
        } catch (IOException exception) {
            throw new IllegalStateException("도서 검색 서비스에 연결하지 못했습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("도서 검색 요청이 중단되었습니다.", exception);
        }
    }

    private CatalogBookDTO mapCompleteBook(JsonObject item) {
        String isbn = text(item, "isbn13").replaceAll("[^0-9]", "");
        String title = clean(text(item, "title"));
        String author = clean(text(item, "author"));
        String publisher = clean(text(item, "publisher"));
        String publishedDate = text(item, "pubDate");
        String description = clean(text(item, "description"));
        String imageUrl = secureUrl(text(item, "cover"));
        String sourceUrl = secureUrl(text(item, "link"));
        String genre = mapGenre(text(item, "categoryName"));

        try {
            BookRequestService.normalizeIsbn(isbn);
            LocalDate.parse(publishedDate);
        } catch (RuntimeException exception) {
            return null;
        }
        if (title.isBlank() || author.isBlank() || publisher.isBlank() || description.isBlank()
                || imageUrl.isBlank() || sourceUrl.isBlank() || genre == null) {
            return null;
        }
        return new CatalogBookDTO(isbn, limit(title, 200), limit(author, 100), genre,
                limit(publisher, 100), publishedDate, limit(description, 1000),
                limit(imageUrl, 500), limit(sourceUrl, 500));
    }

    private String mapGenre(String categoryName) {
        String category = categoryName.toLowerCase(Locale.ROOT);
        if (contains(category, "판타지")) return "판타지";
        if (contains(category, "sf", "과학소설")) return "SF";
        if (contains(category, "추리", "미스터리")) return "추리";
        if (contains(category, "스릴러", "공포")) return "스릴러";
        if (contains(category, "로맨스")) return "로맨스";
        if (contains(category, "고전")) return "고전";
        if (contains(category, "역사소설")) return "역사소설";
        if (contains(category, "소설", "시/희곡")) return "소설";
        if (contains(category, "자기계발")) return "자기계발";
        if (contains(category, "컴퓨터", "모바일", "it")) return "IT";
        if (contains(category, "과학")) return "과학";
        if (contains(category, "철학")) return "철학";
        if (contains(category, "역사")) return "역사";
        if (contains(category, "인물", "평전", "전기")) return "전기";
        if (contains(category, "인문", "사회과학")) return "인문";
        return null;
    }

    private boolean contains(String value, String... candidates) {
        for (String candidate : candidates) if (value.contains(candidate)) return true;
        return false;
    }

    private String text(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return "";
        return object.get(key).getAsString().trim();
    }

    private String clean(String value) {
        return value.replaceAll("<[^>]+>", "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String secureUrl(String value) {
        if (value.startsWith("http://")) return "https://" + value.substring(7);
        return value.startsWith("https://") ? value : "";
    }

    private String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max).trim();
    }

    private String required(String value, String label, int max) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + "을(를) 입력해 주세요.");
        if (normalized.length() > max) throw new IllegalArgumentException(label + "은(는) " + max + "자 이하여야 합니다.");
        return normalized;
    }

    private URI withQuery(URI endpoint, String... pairs) {
        StringBuilder query = new StringBuilder(endpoint.toString()).append('?');
        for (int index = 0; index < pairs.length; index += 2) {
            if (index > 0) query.append('&');
            query.append(URLEncoder.encode(pairs[index], StandardCharsets.UTF_8));
            query.append('=');
            query.append(URLEncoder.encode(pairs[index + 1], StandardCharsets.UTF_8));
        }
        return URI.create(query.toString());
    }

    private String loadApiKey() {
        String value = System.getenv("ALADIN_TTB_KEY");
        if (value == null || value.isBlank()) value = System.getProperty("ALADIN_TTB_KEY");
        if (value == null || value.isBlank()) value = readSetting(ProjectPaths.findProjectRoot().resolve(".env.local"));
        if (value == null || value.isBlank()) value = readSetting(ProjectPaths.findProjectRoot().resolve(".env"));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("ALADIN_TTB_KEY가 설정되지 않았습니다.");
        }
        return value.trim();
    }

    private String readSetting(Path path) {
        if (!Files.isRegularFile(path)) return null;
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.startsWith("ALADIN_TTB_KEY=")) return line.substring("ALADIN_TTB_KEY=".length()).trim();
            }
            return null;
        } catch (IOException exception) {
            throw new IllegalStateException("API 설정 파일을 읽지 못했습니다.", exception);
        }
    }
}
