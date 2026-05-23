package com.example.miniagent.plan;

import com.example.miniagent.agent.Agent;
import com.example.miniagent.agent.PlanExecuteAgent;
import com.example.miniagent.llm.ChatMessage;
import com.example.miniagent.llm.ChatResponse;
import com.example.miniagent.llm.LlmClient;
import com.example.miniagent.llm.ToolDefinition;
import com.example.miniagent.memory.ContextCompressor;
import com.example.miniagent.memory.ConversationMemory;
import com.example.miniagent.memory.LongTermMemoryStore;
import com.example.miniagent.memory.MemoryManager;
import com.example.miniagent.memory.MemoryRetriever;
import com.example.miniagent.prompt.PromptAssembler;
import com.example.miniagent.prompt.PromptRepository;
import com.example.miniagent.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannerAndExecutorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldParsePlanAndSortTopologically() throws Exception {
        Planner planner = new Planner(new QueueLlmClient("""
                {
                  "summary": "Do three things",
                  "tasks": [
                    {"id":"t1","description":"read","type":"ANALYSIS","dependencies":[]},
                    {"id":"t2","description":"write","type":"FILE_WRITE","dependencies":["t1"]},
                    {"id":"t3","description":"verify","type":"VERIFICATION","dependencies":["t2"]}
                  ]
                }
                """), new PromptAssembler(new PromptRepository()));

        ExecutionPlan plan = planner.createPlan("do work");
        assertEquals(List.of("t1", "t2", "t3"), plan.getExecutionOrder());
    }

    @Test
    void shouldDetectCycle() {
        Planner planner = new Planner(new QueueLlmClient("""
                {
                  "summary": "Cycle",
                  "tasks": [
                    {"id":"t1","description":"one","type":"ANALYSIS","dependencies":["t2"]},
                    {"id":"t2","description":"two","type":"FILE_WRITE","dependencies":["t1"]}
                  ]
                }
                """), new PromptAssembler(new PromptRepository()));

        assertThrows(IllegalStateException.class, () -> planner.createPlan("cycle"));
    }

    @Test
    void shouldExecutePlanInDependencyOrder() {
        Planner planner = new Planner(new QueueLlmClient("""
                {
                  "summary": "Execute tasks",
                  "tasks": [
                    {"id":"t1","description":"first","type":"ANALYSIS","dependencies":[]},
                    {"id":"t2","description":"second","type":"VERIFICATION","dependencies":["t1"]}
                  ]
                }
                """), new PromptAssembler(new PromptRepository()));

        Agent agent = new Agent(
                new QueueLlmClient("result-1", "result-2"),
                new ToolRegistry(),
                new MemoryManager(
                        new ConversationMemory(),
                        new LongTermMemoryStore(tempDir.resolve("memory.json")),
                        new MemoryRetriever(),
                        new ContextCompressor(),
                        100,
                        500
                ),
                new PromptAssembler(new PromptRepository()),
                4,
                500
        );

        PlanExecuteAgent executor = new PlanExecuteAgent(planner, agent);
        String result = executor.run("do work");
        assertTrue(result.contains("t1 [COMPLETED]: result-1"));
        assertTrue(result.contains("t2 [COMPLETED]: result-2"));
    }

    @Test
    void shouldExecuteIndependentPlanTasksConcurrently() {
        Planner planner = new Planner(new QueueLlmClient("""
                {
                  "summary": "Execute independent tasks",
                  "tasks": [
                    {"id":"t1","description":"first","type":"ANALYSIS","dependencies":[]},
                    {"id":"t2","description":"second","type":"ANALYSIS","dependencies":[]},
                    {"id":"t3","description":"third","type":"VERIFICATION","dependencies":["t1","t2"]}
                  ]
                }
                """), new PromptAssembler(new PromptRepository()));
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        Agent agent = new SleepingAgent(memoryManager(), active, maxActive);

        PlanExecuteAgent executor = new PlanExecuteAgent(planner, agent);
        String result = executor.run("do work");

        assertTrue(maxActive.get() > 1);
        assertTrue(result.contains("t1 [COMPLETED]: result-first"));
        assertTrue(result.contains("t2 [COMPLETED]: result-second"));
        assertTrue(result.contains("t3 [COMPLETED]: result-third"));
    }

    private MemoryManager memoryManager() {
        return new MemoryManager(
                new ConversationMemory(),
                new LongTermMemoryStore(tempDir.resolve("memory.json")),
                new MemoryRetriever(),
                new ContextCompressor(),
                100,
                500
        );
    }

    private static class QueueLlmClient implements LlmClient {
        private final Queue<ChatResponse> responses = new ArrayDeque<>();

        private QueueLlmClient(String... contents) {
            for (String content : contents) {
                responses.add(new ChatResponse(content, List.of()));
            }
        }

        @Override
        public ChatResponse chat(List<ChatMessage> conversationHistory, List<ToolDefinition> toolDefinitions) {
            return responses.remove();
        }
    }

    private static class SleepingAgent extends Agent {
        private final AtomicInteger active;
        private final AtomicInteger maxActive;

        private SleepingAgent(MemoryManager memoryManager, AtomicInteger active, AtomicInteger maxActive) {
            super(new QueueLlmClient(), new ToolRegistry(), memoryManager, new PromptAssembler(new PromptRepository()), 4, 500);
            this.active = active;
            this.maxActive = maxActive;
        }

        @Override
        public String runWithInstruction(String userInput, String overrideInstruction) {
            int running = active.incrementAndGet();
            maxActive.updateAndGet(previous -> Math.max(previous, running));
            try {
                Thread.sleep(150);
                return "result-" + userInput;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", e);
            } finally {
                active.decrementAndGet();
            }
        }
    }
}
