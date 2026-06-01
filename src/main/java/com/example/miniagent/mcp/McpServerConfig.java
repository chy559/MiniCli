package com.example.miniagent.mcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record McpServerConfig(
        String name,
        String transport,
        String command,
        List<String> args,
        Map<String, String> env,
        String url,
        Map<String, String> headers
) {
    public McpServerConfig {
        args = args == null ? List.of() : List.copyOf(args);
        env = env == null ? Map.of() : Map.copyOf(env);
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static McpServerConfig stdio(String name, String command, List<String> args, Map<String, String> env) {
        return new McpServerConfig(name, "stdio", command, args, env, null, Map.of());
    }

    public static McpServerConfig streamableHttp(String name, String url, Map<String, String> headers) {
        return new McpServerConfig(name, "streamable_http", null, List.of(), Map.of(), url, headers);
    }

    public List<String> commandLine() {
        List<String> values = new ArrayList<>();
        values.add(command);
        values.addAll(args);
        return values;
    }

    public Map<String, String> expandedEnv() {
        Map<String, String> values = new HashMap<>();
        env.forEach((key, value) -> values.put(key, expandEnvReference(value)));
        return values;
    }

    public Map<String, String> expandedHeaders() {
        Map<String, String> values = new HashMap<>();
        headers.forEach((key, value) -> values.put(key, expandEnvReference(value)));
        return values;
    }

    private String expandEnvReference(String value) {
        if (value == null) {
            return "";
        }
        if (value.startsWith("${") && value.endsWith("}")) {
            return System.getenv().getOrDefault(value.substring(2, value.length() - 1), "");
        }
        return value;
    }
}
