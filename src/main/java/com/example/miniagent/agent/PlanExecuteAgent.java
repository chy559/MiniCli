package com.example.miniagent.agent;

import com.example.miniagent.plan.ExecutionPlan;
import com.example.miniagent.plan.PlanStatus;
import com.example.miniagent.plan.Planner;
import com.example.miniagent.plan.Task;
import com.example.miniagent.plan.TaskStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PlanExecuteAgent {
    private final Planner planner;
    private final Agent taskExecutorAgent;

    public PlanExecuteAgent(Planner planner, Agent taskExecutorAgent) {
        this.planner = planner;
        this.taskExecutorAgent = taskExecutorAgent;
    }

    public String run(String goal) {
        ExecutionPlan plan = planner.createPlan(goal);
        plan.setStatus(PlanStatus.RUNNING);

        while (!allTasksCompleted(plan)) {
            List<Task> executableTasks = executableTasks(plan);
            if (executableTasks.isEmpty()) {
                markPendingTasksSkipped(plan);
                plan.setStatus(PlanStatus.FAILED);
                return "Plan failed\n- reason: no executable tasks remain";
            }

            TaskFailure failure = executeBatch(executableTasks);
            if (failure != null) {
                plan.setStatus(PlanStatus.FAILED);
                return """
                        Plan failed
                        - task: %s
                        - reason: %s
                        """.formatted(failure.task().getId(), failure.reason()).trim();
            }
        }

        plan.setStatus(PlanStatus.COMPLETED);
        StringBuilder builder = new StringBuilder();
        builder.append("Plan completed").append(System.lineSeparator());
        builder.append("- summary: ").append(plan.getSummary()).append(System.lineSeparator());
        for (String taskId : plan.getExecutionOrder()) {
            Task task = plan.getTasks().get(taskId);
            builder.append("- ").append(taskId).append(" [").append(task.getStatus()).append("]: ").append(task.getResult()).append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    private TaskFailure executeBatch(List<Task> tasks) {
        tasks.forEach(task -> task.setStatus(TaskStatus.RUNNING));
        ExecutorService executorService = Executors.newFixedThreadPool(tasks.size());
        try {
            List<Future<TaskResult>> futures = new ArrayList<>();
            for (Task task : tasks) {
                futures.add(executorService.submit(() -> executeTask(task)));
            }

            for (int i = 0; i < futures.size(); i++) {
                Task task = tasks.get(i);
                Future<TaskResult> future = futures.get(i);
                try {
                    TaskResult result = future.get();
                    task.setResult(result.result());
                    task.setStatus(TaskStatus.COMPLETED);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    task.setError("Task execution interrupted");
                    task.setStatus(TaskStatus.FAILED);
                    return new TaskFailure(task, task.getError());
                } catch (ExecutionException e) {
                    String reason = e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
                    task.setError(reason);
                    task.setStatus(TaskStatus.FAILED);
                    return new TaskFailure(task, reason);
                }
            }
            return null;
        } finally {
            executorService.shutdownNow();
        }
    }

    private TaskResult executeTask(Task task) {
        String result = taskExecutorAgent.runWithInstruction(task.getDescription(), """
                %s

                Return a compact execution result for this task only.
                """.formatted(task.getDescription()));
        return new TaskResult(task, result);
    }

    private List<Task> executableTasks(ExecutionPlan plan) {
        List<Task> executable = new ArrayList<>();
        for (String taskId : plan.getExecutionOrder()) {
            Task task = plan.getTasks().get(taskId);
            if (task.getStatus() == TaskStatus.PENDING && dependenciesCompleted(plan, task)) {
                executable.add(task);
            }
        }
        return executable;
    }

    private boolean allTasksCompleted(ExecutionPlan plan) {
        return plan.getTasks().values().stream().allMatch(task -> task.getStatus() == TaskStatus.COMPLETED);
    }

    private void markPendingTasksSkipped(ExecutionPlan plan) {
        for (Task task : plan.getTasks().values()) {
            if (task.getStatus() == TaskStatus.PENDING) {
                task.setStatus(TaskStatus.SKIPPED);
            }
        }
    }

    private boolean dependenciesCompleted(ExecutionPlan plan, Task task) {
        for (String dependencyId : task.getDependencies()) {
            Task dependency = plan.getTasks().get(dependencyId);
            if (dependency == null || dependency.getStatus() != TaskStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }

    private record TaskResult(Task task, String result) {
    }

    private record TaskFailure(Task task, String reason) {
    }
}
