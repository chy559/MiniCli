package com.example.miniagent.agent;

import com.example.miniagent.llm.ChatMessage;
import com.example.miniagent.llm.ChatResponse;
import com.example.miniagent.llm.LlmClient;
import com.example.miniagent.memory.MemoryManager;
import com.example.miniagent.prompt.PromptAssembler;
import com.example.miniagent.rag.CodebaseRagService;
import com.example.miniagent.tool.ToolExecutionResult;
import com.example.miniagent.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.List;

public class Agent {
    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final MemoryManager memoryManager;
    private final PromptAssembler promptAssembler;
    private final CodebaseRagService codebaseRagService;
    private final int maxLoopCount;
    private final int conversationBudget;

    public Agent(LlmClient llmClient,
                 ToolRegistry toolRegistry,
                 MemoryManager memoryManager,
                 PromptAssembler promptAssembler,
                 int maxLoopCount,
                 int conversationBudget) {
        this(llmClient, toolRegistry, memoryManager, promptAssembler, null, maxLoopCount, conversationBudget);
    }

    public Agent(LlmClient llmClient,
                 ToolRegistry toolRegistry,
                 MemoryManager memoryManager,
                 PromptAssembler promptAssembler,
                 CodebaseRagService codebaseRagService,
                 int maxLoopCount,
                 int conversationBudget) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.memoryManager = memoryManager;
        this.promptAssembler = promptAssembler;
        this.codebaseRagService = codebaseRagService;
        this.maxLoopCount = maxLoopCount;
        this.conversationBudget = conversationBudget;
    }

    public String run(String userInput) {
        return runWithInstruction(userInput, null);
    }

    public String runWithInstruction(String userInput, String overrideInstruction) {
        memoryManager.addUserMessage(userInput);

        String memoryContext = memoryManager.buildContextForQuery(userInput, 3);
        String codeContext = codebaseRagService == null ? "" : codebaseRagService.buildContext(userInput, 5);
        String context = combineContexts(memoryContext, codeContext);
        List<ChatMessage> conversationHistory = new ArrayList<>();
        conversationHistory.add(ChatMessage.system(promptAssembler.assembleReactPrompt(context, overrideInstruction)));
        conversationHistory.add(ChatMessage.user(userInput));

        for (int loop = 0; loop < maxLoopCount; loop++) {
            maybeCompactConversationHistory(conversationHistory);
            ChatResponse response = llmClient.chat(conversationHistory, toolRegistry.getToolDefinitions());

            if (response.hasToolCalls()) {
                conversationHistory.add(ChatMessage.assistant(response.content()));
                List<ToolExecutionResult> results = toolRegistry.executeTools(response.toolCalls());
                for (ToolExecutionResult result : results) {
                    memoryManager.addToolResult(result.toolName(), result.result().content());
                    conversationHistory.add(ChatMessage.tool(result.toolCallId(), result.toolName(), result.result().content()));
                }
                continue;
            }

            memoryManager.addAssistantMessage(response.content());
            conversationHistory.add(ChatMessage.assistant(response.content()));
            return response.content();
        }

        String timeoutMessage = "Stopped after reaching max loop count without a final answer.";
        memoryManager.addAssistantMessage(timeoutMessage);
        return timeoutMessage;
    }

    private String combineContexts(String memoryContext, String codeContext) {
        if ((memoryContext == null || memoryContext.isBlank()) && (codeContext == null || codeContext.isBlank())) {
            return "";
        }
        if (memoryContext == null || memoryContext.isBlank()) {
            return codeContext;
        }
        if (codeContext == null || codeContext.isBlank()) {
            return memoryContext;
        }
        return memoryContext + "\n\n" + codeContext;
    }

    private void maybeCompactConversationHistory(List<ChatMessage> conversationHistory) {
        int estimatedTokens = conversationHistory.stream()
                .mapToInt(message -> memoryManager.contextCompressor().estimateTokens(message.content()))
                .sum();
        if (estimatedTokens <= conversationBudget || conversationHistory.size() < 8) {
            return;
        }

        int keepRecent = 4;
        List<ChatMessage> older = new ArrayList<>(conversationHistory.subList(0, conversationHistory.size() - keepRecent));
        List<ChatMessage> recent = new ArrayList<>(conversationHistory.subList(conversationHistory.size() - keepRecent, conversationHistory.size()));
        ChatMessage summary = memoryManager.contextCompressor().compactConversationHistory(older);

        conversationHistory.clear();
        conversationHistory.add(summary);
        conversationHistory.addAll(recent);
    }
}
