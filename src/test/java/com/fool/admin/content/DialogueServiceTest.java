package com.fool.admin.content;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueServiceTest {
    private final UUID playerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        DialogueService.resetForTesting();
    }

    @AfterEach
    void tearDown() {
        DialogueService.resetForTesting();
    }

    @Test
    void beginSessionRejectsEmptyDialogue() {
        NpcDefinition npc = sampleNpc("npc-1");
        DialogueDefinition dialogue = new DialogueDefinition("dialogue-1", "Empty", List.of(), 0);
        assertFalse(DialogueService.beginSession(playerId, npc, dialogue, 0L));
        assertFalse(DialogueService.isNpcInDialogue("npc-1"));
    }

    @Test
    void beginSessionMarksNpcBusy() {
        DialogueDefinition dialogue = new DialogueDefinition(
                "dialogue-1",
                "Greeting",
                List.of(
                        new DialogueLine("Hello.", 0),
                        new DialogueLine("More.", 40)
                ),
                0
        );
        assertTrue(DialogueService.beginSession(playerId, sampleNpc("npc-1"), dialogue, 0L));
        assertTrue(DialogueService.isNpcInDialogue("npc-1"));
    }

    @Test
    void countsDownConfiguredDelayBeforeSecondLine() {
        DialogueDefinition dialogue = new DialogueDefinition(
                "dialogue-1",
                "Greeting",
                List.of(
                        new DialogueLine("One", 0),
                        new DialogueLine("Two", 3)
                ),
                0
        );
        DialogueService.DialogueSession session = new DialogueService.DialogueSession(
                "npc-1",
                "dialogue-1",
                1,
                3,
                0L
        );

        for (int tick = 0; tick < 3; tick++) {
            DialogueService.SessionAdvanceResult waiting = DialogueService.advanceSession(session, dialogue, "Guide");
            assertNull(waiting.deliveredLine());
            session = waiting.nextSession();
        }

        DialogueService.SessionAdvanceResult delivered = DialogueService.advanceSession(session, dialogue, "Guide");
        assertEquals("<Guide> Two", delivered.deliveredLine().getString());
        assertTrue(delivered.sessionEnded());
    }

    @Test
    void deliversLinesAtConfiguredDelaysAndEndsSession() {
        DialogueDefinition dialogue = new DialogueDefinition(
                "dialogue-1",
                "Greeting",
                List.of(
                        new DialogueLine("One", 0),
                        new DialogueLine("Two", 3),
                        new DialogueLine("Three", 2)
                ),
                0
        );
        DialogueService.DialogueSession session = new DialogueService.DialogueSession(
                "npc-1",
                "dialogue-1",
                0,
                0,
                0L
        );

        List<String> delivered = new ArrayList<>();
        DialogueService.SessionAdvanceResult result = DialogueService.advanceSession(session, dialogue, "Guide");
        delivered.add(result.deliveredLine().getString());
        session = result.nextSession();

        for (int tick = 0; tick < 3; tick++) {
            result = DialogueService.advanceSession(session, dialogue, "Guide");
            assertNull(result.deliveredLine());
            session = result.nextSession();
        }

        result = DialogueService.advanceSession(session, dialogue, "Guide");
        delivered.add(result.deliveredLine().getString());
        assertFalse(result.sessionEnded());
        session = result.nextSession();

        for (int tick = 0; tick < 2; tick++) {
            result = DialogueService.advanceSession(session, dialogue, "Guide");
            assertNull(result.deliveredLine());
            session = result.nextSession();
        }

        result = DialogueService.advanceSession(session, dialogue, "Guide");
        delivered.add(result.deliveredLine().getString());
        assertTrue(result.sessionEnded());
        assertNull(result.nextSession());

        assertEquals(List.of("<Guide> One", "<Guide> Two", "<Guide> Three"), delivered);
    }

    private static NpcDefinition sampleNpc(String id) {
        return new NpcDefinition(
                id,
                "Guide",
                Identifier.withDefaultNamespace("villager"),
                0,
                64,
                0,
                List.of(),
                true,
                false,
                "dialogue-1",
                null,
                0
        );
    }

    private static DialogueDefinition sampleDialogue() {
        return new DialogueDefinition(
                "dialogue-1",
                "Greeting",
                List.of(new DialogueLine("Hello.", 0)),
                0
        );
    }
}
