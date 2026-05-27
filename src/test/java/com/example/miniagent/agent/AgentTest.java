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
import com.example.miniagent.rag.CodebaseIndexer;
import com.example.miniagent.rag.CodebaseRagService;
import com.example.miniagent.rag.CodebaseRagStore;
import com.example.miniagent.rag.HybridCodeRetriever;
import com.example.miniagent.rag.JiebaTokenizer;
import com.example.miniagent.rag.LocalHashEmbeddingModel;
import com.example.miniagent.rag.VectorJsonCodec;
import com.example.miniagent.tool.SaveMemoryTool;
import com.example.miniagent.tool.SearchCodeTool;
import com.example.miniagent.tool.ToolRegistry;
import com.example.miniagent.tool.WriteFileTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void shouldExecuteSaveMemoryToolAndContinueLoop() {
        ToolRegistry toolRegistry = new ToolRegistry();
        MemoryManager memoryManager = memoryManager();
        toolRegistry.register(new SaveMemoryTool(memoryManager));

        Agent agent = new Agent(
                new StubLlmClient(List.of(
                        new ChatResponse("", List.of(new ToolCall("1", "save_memory", Map.of("fact", "User prefers concise Chinese explanations")))),
                        new ChatResponse("memory saved", List.of())
                )),
                toolRegistry,
                memoryManager,
                new PromptAssembler(new PromptRepository()),
                4,
                500
        );

        String result = agent.run("remember that I prefer concise Chinese explanations");

        assertEquals("memory saved", result);
        assertTrue(memoryManager.buildContextForQuery("Chinese explanations", 3).contains("concise Chinese"));
    }

    @Test
    void shouldExecuteSearchCodeToolAndContinueLoop() throws IOException {
        Path source = tempDir.resolve("src/main/java/example/SearchTarget.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "class SearchTarget { void vectorSearch() {} }");
        CodebaseRagService codebaseRagService = ragService(tempDir.resolve("rag.sqlite"));
        codebaseRagService.index(tempDir);
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new SearchCodeTool(codebaseRagService));

        Agent agent = new Agent(
                new StubLlmClient(List.of(
                        new ChatResponse("", List.of(new ToolCall("1", "search_code", Map.of("query", "vector search", "topK", 3)))),
                        new ChatResponse("found code", List.of())
                )),
                toolRegistry,
                memoryManager(),
                new PromptAssembler(new PromptRepository()),
                4,
                500
        );

        String result = agent.run("find vector search implementation");

        assertEquals("found code", result);
    }

    @Test
    void shouldNotInjectCodeRagBeforeModelChoosesSearchCodeTool() throws IOException {
        Path source = tempDir.resolve("src/main/java/example/SearchTarget.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "class SearchTarget { void vectorSearch() {} }");
        CodebaseRagService codebaseRagService = ragService(tempDir.resolve("rag.sqlite"));
        codebaseRagService.index(tempDir);
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new SearchCodeTool(codebaseRagService));
        StubLlmClient llmClient = new StubLlmClient(List.of(new ChatResponse("answer", List.of())));

        Agent agent = new Agent(
                llmClient,
                toolRegistry,
                memoryManager(),
                new PromptAssembler(new PromptRepository()),
                4,
                500
        );

        String result = agent.run("find vector search implementation");

        assertEquals("answer", result);
        assertFalse(llmClient.lastConversationHistory.get(0).content().contains("Relevant codebase context"));
        assertTrue(llmClient.lastToolDefinitions.stream().anyMatch(tool -> tool.name().equals("search_code")));
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

    private CodebaseRagService ragService(Path dbPath) {
        JiebaTokenizer tokenizer = new JiebaTokenizer();
        LocalHashEmbeddingModel embeddingModel = new LocalHashEmbeddingModel(tokenizer);
        return new CodebaseRagService(
                new CodebaseIndexer(embeddingModel),
                new CodebaseRagStore(dbPath, new VectorJsonCodec()),
                new HybridCodeRetriever(embeddingModel, tokenizer)
        );
    }

    private static class StubLlmClient implements LlmClient {
        private final List<ChatResponse> responses;
        private int index;
        private List<ChatMessage> lastConversationHistory = List.of();
        private List<ToolDefinition> lastToolDefinitions = List.of();

        private StubLlmClient(List<ChatResponse> responses) {
            this.responses = responses;
        }

        @Override
        public ChatResponse chat(List<ChatMessage> conversationHistory, List<ToolDefinition> toolDefinitions) {
            this.lastConversationHistory = List.copyOf(conversationHistory);
            this.lastToolDefinitions = List.copyOf(toolDefinitions);
            return responses.get(index++);
        }
    }
}
