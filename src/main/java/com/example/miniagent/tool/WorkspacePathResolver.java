package com.example.miniagent.tool;

import java.nio.file.Path;

class WorkspacePathResolver {
    private final Path workspaceRoot;

    WorkspacePathResolver(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
    }

    Path resolveInsideWorkspace(String rawPath) {
        String value = rawPath == null || rawPath.isBlank() || "null".equalsIgnoreCase(rawPath) ? "." : rawPath;
        Path candidate = Path.of(value);
        Path resolved = candidate.isAbsolute()
                ? candidate.toAbsolutePath().normalize()
                : workspaceRoot.resolve(candidate).normalize();
        if (!resolved.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("Path is outside workspace: " + value);
        }
        return resolved;
    }

    String relativize(Path path) {
        return workspaceRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }
}
