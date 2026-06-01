package com.example.miniagent.mcp;

import java.util.Map;

public interface McpTransport extends AutoCloseable {
    void start();

    Map<String, Object> request(String method, Map<String, Object> params);

    void notify(String method, Map<String, Object> params);

    @Override
    void close();
}
