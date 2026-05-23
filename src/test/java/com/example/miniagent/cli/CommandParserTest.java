package com.example.miniagent.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandParserTest {

    private final CommandParser parser = new CommandParser();

    @Test
    void shouldParsePlanCommand() {
        CliCommand command = parser.parse("/plan analyze module");
        assertEquals(CommandType.PLAN, command.type());
        assertEquals("analyze module", command.payload());
    }

    @Test
    void shouldParseSaveCommand() {
        CliCommand command = parser.parse("/save remember this");
        assertEquals(CommandType.SAVE_MEMORY, command.type());
        assertEquals("remember this", command.payload());
    }

    @Test
    void shouldTreatNormalInputAsDefault() {
        CliCommand command = parser.parse("hello");
        assertEquals(CommandType.DEFAULT_INPUT, command.type());
        assertEquals("hello", command.payload());
    }
}
