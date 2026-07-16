package com.fool.admin.content;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void beginOverlayMarksNpcBusy() {
        assertTrue(DialogueService.beginOverlay(playerId, "npc-1", "campaign-1", "quest-1"));
        assertTrue(DialogueService.isNpcInDialogue("npc-1"));
    }

    @Test
    void endOverlayReleasesNpc() {
        DialogueService.beginOverlay(playerId, "npc-1", "campaign-1", "quest-1");
        DialogueService.endOverlay(playerId);
        assertFalse(DialogueService.isNpcInDialogue("npc-1"));
    }

    @Test
    void rebeginSameNpcAndQuestIsRejectedWhileDialogueIsOpen() {
        assertTrue(DialogueService.beginOverlay(playerId, "npc-1", "campaign-1", "quest-1"));
        assertFalse(DialogueService.beginOverlay(playerId, "npc-1", "campaign-1", "quest-1"));
    }

    @Test
    void campaignSelectionCanOnlyBeConsumedOnceForTheNpcAndCampaignShown() {
        DialogueService.beginCampaignSelection(playerId, "npc-1", Set.of("chapter-one", "chapter-two"));

        assertFalse(DialogueService.consumeCampaignSelection(playerId, "npc-2", "chapter-one"));
        assertTrue(DialogueService.consumeCampaignSelection(playerId, "npc-1", "chapter-one"));
        assertFalse(DialogueService.consumeCampaignSelection(playerId, "npc-1", "chapter-two"));
    }
}
