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

    @Test
    void shouldParseRagCommands() {
        assertEquals(CommandType.RAG_STATUS, parser.parse("/rag").type());
        assertEquals(CommandType.RAG_INDEX, parser.parse("/rag index").type());

        CliCommand search = parser.parse("/rag search memory compression");
        assertEquals(CommandType.RAG_SEARCH, search.type());
        assertEquals("memory compression", search.payload());
    }
}
