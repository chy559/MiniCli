package com.example.miniagent.tool;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class ExecuteCommandTool implements Tool {
    private final Path workingDirectory;

    public ExecuteCommandTool(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public String name() {
        return "execute_command";
    }

    @Override
    public String description() {
        return "Execute a shell command and return stdout, stderr, and exitCode.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of("command", Map.of("type", "string")),
                "required", java.util.List.of("command")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        String command = String.valueOf(args.get("command"));
        ProcessBuilder builder = new ProcessBuilder("powershell", "-Command", command);
        builder.directory(workingDirectory.toFile());
        try {
            Process process = builder.start();
            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());
            int exitCode = process.waitFor();
            String content = """
                    exitCode: %d
                    stdout:
                    %s
                    stderr:
                    %s
                    """.formatted(exitCode, stdout.strip(), stderr.strip()).trim();
            return new ToolResult(name(), content, exitCode == 0);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ToolResult(name(), "Failed to execute command: " + e.getMessage(), false);
        }
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.EXECUTE;
    }
}
