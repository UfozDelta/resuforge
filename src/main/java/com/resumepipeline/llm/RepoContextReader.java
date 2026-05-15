package com.resumepipeline.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads a local project directory into a single context blob:
 *   README.md (full)
 *   + shallow file listing (paths only, depth <= 3, ignoring junk dirs).
 * Capped at MAX_CHARS. Best-effort; missing/unreadable returns "".
 */
@Component
public class RepoContextReader {

    private static final Logger log = LoggerFactory.getLogger(RepoContextReader.class);
    private static final int MAX_CHARS = 30_000;
    private static final int MAX_DEPTH = 3;

    private static final Set<String> SKIP_DIRS = Set.of(
            "node_modules", ".git", "target", "build", "dist", ".next",
            "__pycache__", ".venv", "venv", ".idea", ".vscode", "coverage"
    );

    public String read(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) return "";
        Path root = Path.of(sourcePath);
        if (!Files.isDirectory(root)) {
            log.warn("sourcePath not a directory: {}", sourcePath);
            return "";
        }

        StringBuilder sb = new StringBuilder();

        Path readme = findReadme(root);
        if (readme != null) {
            sb.append("=== README.md ===\n");
            try {
                sb.append(Files.readString(readme, StandardCharsets.UTF_8)).append("\n\n");
            } catch (IOException e) {
                log.warn("Could not read README at {}: {}", readme, e.getMessage());
            }
        }

        sb.append("=== File listing ===\n");
        try (Stream<Path> walk = Files.walk(root, MAX_DEPTH)) {
            List<String> paths = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> !isUnderSkippedDir(root, p))
                    .map(p -> root.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
            for (String p : paths) {
                sb.append(p).append('\n');
                if (sb.length() > MAX_CHARS) break;
            }
        } catch (IOException e) {
            log.warn("Walk failed at {}: {}", root, e.getMessage());
        }

        return sb.length() > MAX_CHARS ? sb.substring(0, MAX_CHARS) : sb.toString();
    }

    private Path findReadme(Path root) {
        for (String name : List.of("README.md", "README.MD", "Readme.md", "readme.md")) {
            Path p = root.resolve(name);
            if (Files.isRegularFile(p)) return p;
        }
        return null;
    }

    private boolean isUnderSkippedDir(Path root, Path file) {
        Path rel = root.relativize(file);
        for (int i = 0; i < rel.getNameCount(); i++) {
            if (SKIP_DIRS.contains(rel.getName(i).toString())) return true;
        }
        return false;
    }
}
