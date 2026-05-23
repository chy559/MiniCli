package com.example.miniagent.agent;

import com.example.miniagent.plan.ExecutionPlan;
import com.example.miniagent.plan.PlanStatus;
import com.example.miniagent.plan.Planner;
import com.example.miniagent.plan.Task;
import com.example.miniagent.plan.TaskStatus;

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

        for (String taskId : plan.getExecutionOrder()) {
            Task task = plan.getTasks().get(taskId);
            if (!dependenciesCompleted(plan, task)) {
                task.setStatus(TaskStatus.SKIPPED);
                continue;
            }

            task.setStatus(TaskStatus.RUNNING);
            try {
                String result = taskExecutorAgent.runWithInstruction(task.getDescription(), """
                        %s

                        Return a compact execution result for this task only.
                        """.formatted(task.getDescription()));
                task.setResult(result);
                task.setStatus(TaskStatus.COMPLETED);
            } catch (Exception e) {
                task.setError(e.getMessage());
                task.setStatus(TaskStatus.FAILED);
                plan.setStatus(PlanStatus.FAILED);
                return """
                        Plan failed
                        - task: %s
                        - reason: %s
                        """.formatted(task.getId(), e.getMessage()).trim();
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

    private boolean dependenciesCompleted(ExecutionPlan plan, Task task) {
        for (String dependencyId : task.getDependencies()) {
            Task dependency = plan.getTasks().get(dependencyId);
            if (dependency == null || dependency.getStatus() != TaskStatus.COMPLETED) {
                return false;
            }
        }
        return true;
    }
}
