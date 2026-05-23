package com.example.miniagent.plan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExecutionPlan {
    private String id = UUID.randomUUID().toString();
    private String goal;
    private String summary;
    private Map<String, Task> tasks = new LinkedHashMap<>();
    private List<String> executionOrder = new ArrayList<>();
    private PlanStatus status = PlanStatus.CREATED;

    public String getId() {
        return id;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Map<String, Task> getTasks() {
        return tasks;
    }

    public void setTasks(Map<String, Task> tasks) {
        this.tasks = tasks;
    }

    public List<String> getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(List<String> executionOrder) {
        this.executionOrder = executionOrder;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }
}
