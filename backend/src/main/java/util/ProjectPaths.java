package util;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ProjectPaths {
    private ProjectPaths() {
    }

    public static Path findProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("backend"))
                    && Files.isDirectory(current.resolve("frontend"))
                    && Files.isDirectory(current.resolve("db"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("BookMate 프로젝트 루트를 찾을 수 없습니다.");
    }
}
