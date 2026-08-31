package util;

import java.util.regex.Pattern;

/** 화면의 실제 표지 크기에 맞춰 외부 도서 이미지 URL을 변환합니다. */
public final class BookImageUrlUtil {
    public static final int THUMBNAIL_WIDTH = 160;
    public static final int COMPACT_THUMBNAIL_WIDTH = 160;

    private static final Pattern KYOBO_SIZE = Pattern.compile("/fit-in/\\d+x0/");
    private static final Pattern ALADIN_COVER = Pattern.compile("/(?:cover500|cover200|coversum)/");

    private BookImageUrlUtil() {
    }

    public static String thumbnail(String imageUrl) {
        return thumbnail(imageUrl, THUMBNAIL_WIDTH);
    }

    public static String compactThumbnail(String imageUrl) {
        return thumbnail(imageUrl, COMPACT_THUMBNAIL_WIDTH);
    }

    public static String detail(String imageUrl) {
        String normalized = normalize(imageUrl);
        if (normalized == null) return null;

        String kyobo = KYOBO_SIZE.matcher(normalized).replaceFirst("/fit-in/600x0/");
        return ALADIN_COVER.matcher(kyobo).replaceFirst("/cover500/");
    }

    public static String normalize(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        String normalized = imageUrl.trim();
        if (normalized.startsWith("/bookmate/")) {
            normalized = normalized.substring("/bookmate".length());
        }
        return normalized;
    }

    private static String thumbnail(String imageUrl, int width) {
        String normalized = normalize(imageUrl);
        if (normalized == null) return null;

        String kyobo = KYOBO_SIZE.matcher(normalized).replaceFirst("/fit-in/" + width + "x0/");
        String aladinSize = width <= COMPACT_THUMBNAIL_WIDTH ? "coversum" : "cover200";
        return ALADIN_COVER.matcher(kyobo).replaceFirst("/" + aladinSize + "/");
    }
}
