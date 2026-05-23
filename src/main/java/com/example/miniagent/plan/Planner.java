package com.example.miniagent.plan;

import com.example.miniagent.llm.ChatMessage;
import com.example.miniagent.llm.ChatResponse;
import com.example.miniagent.llm.LlmClient;
import com.example.miniagent.prompt.PromptAssembler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Planner {
    private final LlmClient llmClient;
    private final PromptAssembler promptAssembler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Planner(LlmClient llmClient, PromptAssembler promptAssembler) {
        this.llmClient = llmClient;
        this.promptAssembler = promptAssembler;
    }

    public ExecutionPlan createPlan(String goal) {
        String json = requestPlan(goal, false);
        try {
            return parsePlan(goal, json);
        } catch (JsonProcessingException firstError) {
            String retryJson = requestPlan(goal, true);
            try {
                return parsePlan(goal, retryJson);
            } catch (JsonProcessingException secondError) {
                throw new IllegalStateException("Planner JSON parsing failed after one retry", secondError);
            }
        }
    }

    private String requestPlan(String goal, boolean retry) {
        String userPrompt = retry
                ? "Regenerate the plan as strict valid JSON only.\nTask: " + goal
                : goal;
        ChatResponse response = llmClient.chat(List.of(
                ChatMessage.system(promptAssembler.plannerPrompt()),
                ChatMessage.user(userPrompt)
        ), List.of());
        return response.content();
    }

    public ExecutionPlan parsePlan(String goal, String json) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(json);
        ExecutionPlan plan = new ExecutionPlan();
        plan.setGoal(goal);
        plan.setSummary(root.path("summary").asText());

        Map<String, Task> tasks = new LinkedHashMap<>();
        for (JsonNode taskNode : root.path("tasks")) {
            Task task = new Task(
                    taskNode.path("id").asText(),
                    taskNode.path("description").asText(),
                    TaskType.valueOf(taskNode.path("type").asText()),
                    toList(taskNode.path("dependencies"))
            );
            tasks.put(task.getId(), task);
        }

        populateDependents(tasks);
        plan.setTasks(tasks);
        plan.setExecutionOrder(topologicalSort(tasks));
        return plan;
    }

    private void populateDependents(Map<String, Task> tasks) {
        for (Task task : tasks.values()) {
            task.setDependents(new ArrayList<>());
        }
        for (Task task : tasks.values()) {
            for (String dependency : task.getDependencies()) {
                Task parent = tasks.get(dependency);
                if (parent == null) {
                    throw new IllegalStateException("Unknown dependency: " + dependency);
                }
                parent.getDependents().add(task.getId());
            }
        }
    }

    private List<String> topologicalSort(Map<String, Task> tasks) {
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (Task task : tasks.values()) {
            inDegree.put(task.getId(), task.getDependencies().size());
        }

        ArrayDeque<String> queue = new ArrayDeque<>();
        inDegree.forEach((id, degree) -> {
            if (degree == 0) {
                queue.add(id);
            }
        });

        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String currentId = queue.removeFirst();
            order.add(currentId);
            for (String dependent : tasks.get(currentId).getDependents()) {
                int next = inDegree.computeIfPresent(dependent, (key, value) -> value - 1);
                if (next == 0) {
                    queue.add(dependent);
                }
            }
        }

        if (order.size() != tasks.size()) {
            throw new IllegalStateException("Cycle detected in execution plan");
        }
        return order;
    }

    private List<String> toList(JsonNode node) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }
}
