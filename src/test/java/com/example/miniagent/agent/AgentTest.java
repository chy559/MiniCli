package com.example.miniagent.agent;

import com.example.miniagent.llm.ChatMessage;
import com.example.miniagent.llm.ChatResponse;
import com.example.miniagent.llm.LlmClient;
import com.example.miniagent.llm.ToolCall;
import com.example.miniagent.llm.ToolDefinition;
import com.example.miniagent.memory.ContextCompressor;
import com.example.miniagent.memory.ConversationMemory;
import com.example.miniagent.memory.LongTermMemoryStore;
import com.example.miniagent.memory.MemoryManager;
import com.example.miniagent.memory.MemoryRetriever;
import com.example.miniagent.prompt.PromptAssembler;
import com.example.miniagent.prompt.PromptRepository;
import com.example.miniagent.tool.ToolRegistry;
import com.example.miniagent.tool.WriteFileTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnDirectAnswerWithoutTools() {
        Agent agent = new Agent(
                new StubLlmClient(List.of(new ChatResponse("done", List.of()))),
                new ToolRegistry(),
                memoryManager(),
                new PromptAssembler(new PromptRepository()),
                4,
                500
        );

        assertEquals("done", agent.run("Say done"));
    }

    @Test
    void shouldExecuteToolAndContinueLoop() {
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new WriteFileTool());

        Agent agent = new Agent(
                new StubLlmClient(List.of(
                        new ChatResponse("", List.of(new ToolCall("1", "write_file", Map.of("path", tempDir.resolve("a.txt").toString(), "content", "hello")))),
                        new ChatResponse("file written", List.of())
                )),
                toolRegistry,
                memoryManager(),
                new PromptAssembler(new PromptRepository()),
                4,
                500
        );

        String result = agent.run("write a file");
        assertEquals("file written", result);
        assertTrue(tempDir.resolve("a.txt").toFile().exists());
    }

    private MemoryManager memoryManager() {
        return new MemoryManager(
                new ConversationMemory(),
                new LongTermMemoryStore(tempDir.resolve("memory.json")),
                new MemoryRetriever(),
                new ContextCompressor(),
                200,
                500
        );
    }

    private static class StubLlmClient implements LlmClient {
        private final List<ChatResponse> responses;
        private int index;

        private StubLlmClient(List<ChatResponse> responses) {
            this.responses = responses;
        }

        @Override
        public ChatResponse chat(List<ChatMessage> conversationHistory, List<ToolDefinition> toolDefinitions) {
            return responses.get(index++);
        }
    }
}
