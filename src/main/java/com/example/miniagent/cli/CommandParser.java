package com.example.miniagent.cli;

public class CommandParser {

    public CliCommand parse(String rawInput) {
        String input = rawInput == null ? "" : rawInput.trim();
        if (input.equalsIgnoreCase("/exit")) {
            return new CliCommand(CommandType.EXIT, "");
        }
        if (input.equalsIgnoreCase("/memory")) {
            return new CliCommand(CommandType.MEMORY_STATUS, "");
        }
        if (input.equalsIgnoreCase("/memory clear")) {
            return new CliCommand(CommandType.MEMORY_CLEAR, "");
        }
        if (input.startsWith("/plan ")) {
            return new CliCommand(CommandType.PLAN, input.substring("/plan ".length()).trim());
        }
        if (input.startsWith("/save ")) {
            return new CliCommand(CommandType.SAVE_MEMORY, input.substring("/save ".length()).trim());
        }
        if (input.equalsIgnoreCase("/rag")) {
            return new CliCommand(CommandType.RAG_STATUS, "");
        }
        if (input.equalsIgnoreCase("/rag index")) {
            return new CliCommand(CommandType.RAG_INDEX, "");
        }
        if (input.startsWith("/rag search ")) {
            return new CliCommand(CommandType.RAG_SEARCH, input.substring("/rag search ".length()).trim());
        }
        return new CliCommand(CommandType.DEFAULT_INPUT, input);
    }
}
