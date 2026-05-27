package com.example.miniagent.rag;

import java.nio.file.Path;

public enum CodeType {
    SOURCE,
    TEST,
    DOC,
    CONFIG,
    OTHER;

    public static CodeType fromPath(Path path) {
        String normalized = path.toString().replace('\\', '/').toLowerCase();
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase();
        if (normalized.contains("/src/test/") || fileName.endsWith("test.java")) {
            return TEST;
        }
        if (fileName.endsWith(".md") || normalized.contains("/docs/")) {
            return DOC;
        }
        if (fileName.equals("pom.xml") || fileName.endsWith(".json") || fileName.endsWith(".yaml") || fileName.endsWith(".yml") || fileName.endsWith(".toml") || fileName.endsWith(".properties")) {
            return CONFIG;
        }
        if (fileName.endsWith(".java") || fileName.endsWith(".kt") || fileName.endsWith(".go") || fileName.endsWith(".rs") || fileName.endsWith(".py") || fileName.endsWith(".js") || fileName.endsWith(".ts")) {
            return SOURCE;
        }
        return OTHER;
    }
}
