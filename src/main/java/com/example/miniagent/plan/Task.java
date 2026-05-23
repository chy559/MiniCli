package com.example.miniagent.plan;

import java.util.ArrayList;
import java.util.List;

public class Task {
    private String id;
    private String description;
    private TaskType type;
    private List<String> dependencies = new ArrayList<>();
    private List<String> dependents = new ArrayList<>();
    private TaskStatus status = TaskStatus.PENDING;
    private String result;
    private String error;

    public Task() {
    }

    public Task(String id, String description, TaskType type, List<String> dependencies) {
        this.id = id;
        this.description = description;
        this.type = type;
        this.dependencies = dependencies == null ? new ArrayList<>() : new ArrayList<>(dependencies);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public TaskType getType() {
        return type;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public List<String> getDependents() {
        return dependents;
    }

    public void setDependents(List<String> dependents) {
        this.dependents = dependents;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
