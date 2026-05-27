package com.example.miniagent.cli;

import com.example.miniagent.agent.Agent;
import com.example.miniagent.agent.PlanExecuteAgent;
import com.example.miniagent.llm.LlmClient;
import com.example.miniagent.llm.OpenAiCompatibleClient;
import com.example.miniagent.memory.MemoryManager;
import com.example.miniagent.memory.MemoryRetriever;
import com.example.miniagent.memory.ConversationMemory;
import com.example.miniagent.memory.ContextCompressor;
import com.example.miniagent.memory.LongTermMemoryStore;
import com.example.miniagent.plan.Planner;
import com.example.miniagent.prompt.PromptAssembler;
import com.example.miniagent.prompt.PromptRepository;
import com.example.miniagent.rag.CodebaseIndexer;
import com.example.miniagent.rag.CodebaseRagService;
import com.example.miniagent.rag.CodebaseRagStore;
import com.example.miniagent.rag.HybridCodeRetriever;
import com.example.miniagent.rag.JiebaTokenizer;
import com.example.miniagent.rag.LocalHashEmbeddingModel;
import com.example.miniagent.rag.RagSearchResult;
import com.example.miniagent.rag.VectorJsonCodec;
import com.example.miniagent.tool.ExecuteCommandTool;
import com.example.miniagent.tool.IndexCodeTool;
import com.example.miniagent.tool.ListDirTool;
import com.example.miniagent.tool.ReadFileTool;
import com.example.miniagent.tool.SaveMemoryTool;
import com.example.miniagent.tool.SearchCodeTool;
import com.example.miniagent.tool.ToolRegistry;
import com.example.miniagent.tool.WriteFileTool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException {
        Path memoryPath = Path.of(System.getProperty("user.home"), ".mini-agent", "memory.json");
        Path ragPath = Path.of(System.getProperty("user.home"), ".mini-agent", "code-rag.sqlite");
        Path workspaceRoot = Path.of("").toAbsolutePath();
        LlmClient llmClient = OpenAiCompatibleClient.fromEnvironment();

        LongTermMemoryStore longTermMemoryStore = new LongTermMemoryStore(memoryPath);
        MemoryManager memoryManager = new MemoryManager(
                new ConversationMemory(),
                longTermMemoryStore,
                new MemoryRetriever(),
                new ContextCompressor(),
                4_000,
                10_000,
                2
        );
        JiebaTokenizer jiebaTokenizer = new JiebaTokenizer();
        LocalHashEmbeddingModel embeddingModel = new LocalHashEmbeddingModel(jiebaTokenizer);
        CodebaseRagService codebaseRagService = new CodebaseRagService(
                new CodebaseIndexer(embeddingModel),
                new CodebaseRagStore(ragPath, new VectorJsonCodec()),
                new HybridCodeRetriever(embeddingModel, jiebaTokenizer)
        );

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new ReadFileTool());
        toolRegistry.register(new WriteFileTool());
        toolRegistry.register(new ListDirTool());
        toolRegistry.register(new ExecuteCommandTool(Path.of("").toAbsolutePath()));
        toolRegistry.register(new SaveMemoryTool(memoryManager));
        toolRegistry.register(new SearchCodeTool(codebaseRagService));
        toolRegistry.register(new IndexCodeTool(codebaseRagService, workspaceRoot));

        PromptRepository promptRepository = new PromptRepository();
        PromptAssembler promptAssembler = new PromptAssembler(promptRepository);
        Agent agent = new Agent(llmClient, toolRegistry, memoryManager, promptAssembler, 8, 12_000);
        Planner planner = new Planner(llmClient, promptAssembler);
        PlanExecuteAgent planExecuteAgent = new PlanExecuteAgent(planner, agent);
        CommandParser parser = new CommandParser();

        System.out.println("Mini Agent CLI started. Type /exit to quit.");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("> ");
                String line = reader.readLine();
                if (line == null) {
                    break;
                }

                CliCommand command = parser.parse(line);
                switch (command.type()) {
                    case EXIT -> {
                        System.out.println("Bye.");
                        return;
                    }
                    case MEMORY_STATUS -> System.out.println(memoryManager.describeStatus());
                    case MEMORY_CLEAR -> {
                        memoryManager.clearLongTermMemory();
                        System.out.println("Long-term memory cleared.");
                    }
                    case RAG_STATUS -> System.out.println(codebaseRagService.describeStatus());
                    case RAG_INDEX -> {
                        int chunks = codebaseRagService.index(workspaceRoot);
                        System.out.println("Code RAG indexed chunks: " + chunks);
                    }
                    case RAG_SEARCH -> {
                        for (RagSearchResult result : codebaseRagService.search(command.payload(), 5)) {
                            System.out.printf("%s#chunk-%d score=%.3f semantic=%.3f token=%.3f typeBonus=%.3f%n",
                                    result.chunk().path(),
                                    result.chunk().chunkIndex(),
                                    result.score(),
                                    result.semanticScore(),
                                    result.tokenScore(),
                                    result.typeBonus());
                        }
                    }
                    case SAVE_MEMORY -> {
                        memoryManager.saveLongTermFact(command.payload());
                        System.out.println("Saved.");
                    }
                    case PLAN -> System.out.println(planExecuteAgent.run(command.payload()));
                    case DEFAULT_INPUT -> System.out.println(agent.run(command.payload()));
                }
            }
        }
    }
}
