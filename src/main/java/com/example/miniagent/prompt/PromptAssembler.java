package com.example.miniagent.prompt;

public class PromptAssembler {
    private final PromptRepository promptRepository;

    public PromptAssembler(PromptRepository promptRepository) {
        this.promptRepository = promptRepository;
    }

    public String assembleReactPrompt(String memoryContext, String overrideInstruction) {
        StringBuilder builder = new StringBuilder();
        builder.append(promptRepository.reactSystemPrompt());
        if (memoryContext != null && !memoryContext.isBlank()) {
            builder.append("\n\n").append(memoryContext);
        }
        if (overrideInstruction != null && !overrideInstruction.isBlank()) {
            builder.append("\n\n").append(overrideInstruction);
        }
        return builder.toString();
    }

    public String plannerPrompt() {
        return promptRepository.plannerPrompt();
    }

    public String taskExecutorPrompt(String taskDescription) {
        return promptRepository.taskExecutorPrompt(taskDescription);
    }
}
