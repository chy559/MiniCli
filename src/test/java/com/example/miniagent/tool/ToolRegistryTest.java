package com.example.miniagent.tool;

import com.example.miniagent.llm.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryTest {

    @Test
    void shouldExecuteMultipleToolCallsConcurrentlyAndKeepResultOrder() {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        ToolRegistry registry = new ToolRegistry();
        registry.register(new SleepingTool(active, maxActive));

        List<ToolExecutionResult> results = registry.executeTools(List.of(
                new ToolCall("1", "sleeping_tool", Map.of("label", "first")),
                new ToolCall("2", "sleeping_tool", Map.of("label", "second"))
        ));

        assertTrue(maxActive.get() > 1);
        assertEquals("first", results.get(0).result().content());
        assertEquals("second", results.get(1).result().content());
    }

    private record SleepingTool(AtomicInteger active, AtomicInteger maxActive) implements Tool {
        @Override
        public String name() {
            return "sleeping_tool";
        }

        @Override
        public String description() {
            return "Sleeps briefly and returns a label.";
        }

        @Override
        public Map<String, Object> inputSchema() {
            return Map.of("type", "object");
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            int running = active.incrementAndGet();
            maxActive.updateAndGet(previous -> Math.max(previous, running));
            try {
                Thread.sleep(150);
                return new ToolResult(name(), String.valueOf(args.get("label")), true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new ToolResult(name(), "interrupted", false);
            } finally {
                active.decrementAndGet();
            }
        }
    }
}
