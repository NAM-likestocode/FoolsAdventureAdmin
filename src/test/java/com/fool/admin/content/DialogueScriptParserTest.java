package com.fool.admin.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueScriptParserTest {
    @Test
    void parsesNpcLinesWithDelay() {
        DialogueScriptParser.ParseResult result = DialogueScriptParser.parse("""
                --> Hello there +2
                --> Welcome.
                """);
        assertTrue(result.success());
        assertNotNull(result.script());
        assertEquals(2, result.script().nodes().size());
        assertEquals(40, result.script().nodes().getFirst().delayTicks());
        assertEquals("n1", result.script().nodes().getFirst().nextNodeId());
    }

    @Test
    void parsesChoicesWithLabelJump() {
        DialogueScriptParser.ParseResult result = DialogueScriptParser.parse("""
                --> Do you have a sword?
                <-- [give sword] -> hasSword
                <-- [not yet] -> later
                :hasSword
                --> Excellent.
                :later
                --> Come back later.
                """);
        assertTrue(result.success());
        assertNotNull(result.script());
        DialogueNode choiceNode = result.script().nodes().get(1);
        assertTrue(choiceNode.isChoiceNode());
        assertEquals("n2", choiceNode.choices().getFirst().targetNodeId());
        assertEquals("n3", choiceNode.choices().get(1).targetNodeId());
    }

    @Test
    void rejectsUnknownLabel() {
        DialogueScriptParser.ParseResult result = DialogueScriptParser.parse("""
                --> Hello
                <-- [yes] -> missing
                """);
        assertFalse(result.success());
        assertEquals("UNKNOWN_LABEL", result.errorCode());
    }
}
