package com.example.miniagent.tool;

import com.example.miniagent.llm.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitlToolRegistryTest {

    @Test
    void shouldSkipApprovalForReadOnlyTools() {
        AtomicInteger approvals = new AtomicInteger();
        HitlToolRegistry registry = new HitlToolRegistry((call, tool) -> {
            approvals.incrementAndGet();
            return false;
        });
        registry.register(new FakeTool("read_only", ToolPermission.READ_ONLY));

        List<ToolExecutionResult> results = registry.executeTools(List.of(new ToolCall("1", "read_only", Map.of())));

        assertEquals(0, approvals.get());
        assertTrue(results.get(0).result().success());
        assertEquals("executed read_only", results.get(0).result().content());
    }

    @Test
    void shouldDenyNonReadOnlyToolsWhenApprovalRejects() {
        HitlToolRegistry registry = new HitlToolRegistry((call, tool) -> false);
        registry.register(new FakeTool("write_tool", ToolPermission.WRITE));

        List<ToolExecutionResult> results = registry.executeTools(List.of(new ToolCall("1", "write_tool", Map.of())));

        assertFalse(results.get(0).result().success());
        assertEquals("Tool call denied by user approval layer.", results.get(0).result().content());
    }

    @Test
    void shouldPreserveOriginalResultOrderAcrossApprovedAndDeniedCalls() {
        HitlToolRegistry registry = new HitlToolRegistry((call, tool) -> !"denied_tool".equals(tool.name()));
        registry.register(new FakeTool("read_only", ToolPermission.READ_ONLY));
        registry.register(new FakeTool("denied_tool", ToolPermission.WRITE));
        registry.register(new FakeTool("approved_tool", ToolPermission.WRITE));

        List<ToolExecutionResult> results = registry.executeTools(List.of(
                new ToolCall("1", "read_only", Map.of()),
                new ToolCall("2", "denied_tool", Map.of()),
                new ToolCall("3", "approved_tool", Map.of())
        ));

        assertEquals("read_only", results.get(0).toolName());
        assertEquals("denied_tool", results.get(1).toolName());
        assertEquals("approved_tool", results.get(2).toolName());
        assertTrue(results.get(0).result().success());
        assertFalse(results.get(1).result().success());
        assertTrue(results.get(2).result().success());
    }

    private record FakeTool(String name, ToolPermission permission) implements Tool {
        @Override
        public String description() {
            return "Fake tool";
        }

        @Override
        public Map<String, Object> inputSchema() {
            return Map.of("type", "object");
        }

        @Override
        public ToolResult execute(Map<String, Object> args) {
            return new ToolResult(name(), "executed " + name(), true);
        }
    }
}
