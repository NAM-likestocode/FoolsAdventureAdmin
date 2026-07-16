package com.fool.admin.content;

import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminContentCodecTest {
    @Test
    void waypointRoundTripIncludesDwellTicks() {
        Waypoint original = new Waypoint(10, 64, -5, 80);
        Waypoint decoded = roundTrip(Waypoint.CODEC, original);
        assertEquals(original, decoded);
    }

    @Test
    void waypointDefaultsMissingDwellTicksToZero() {
        var json = JsonOps.INSTANCE.createMap(Map.of(
                JsonOps.INSTANCE.createString("x"), JsonOps.INSTANCE.createInt(1),
                JsonOps.INSTANCE.createString("y"), JsonOps.INSTANCE.createInt(64),
                JsonOps.INSTANCE.createString("z"), JsonOps.INSTANCE.createInt(2)
        ));
        Waypoint waypoint = Waypoint.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(0, waypoint.dwellTicks());
    }

    @Test
    void npcRoundTripIncludesMovementAndDialogueFields() {
        NpcDefinition original = new NpcDefinition(
                "npc-1",
                "Guide",
                Identifier.withDefaultNamespace("villager"),
                12,
                70,
                -4,
                List.of(new Waypoint(20, 70, -4, 40)),
                false,
                true,
                "dialogue-1",
                null,
                3
        );
        NpcDefinition decoded = roundTrip(NpcDefinition.CODEC, original);
        assertEquals(original, decoded);
    }

    @Test
    void npcDefaultsMissingStationaryAndDialogue() {
        var json = JsonOps.INSTANCE.createMap(Map.of(
                JsonOps.INSTANCE.createString("id"), JsonOps.INSTANCE.createString("npc-1"),
                JsonOps.INSTANCE.createString("display_name"), JsonOps.INSTANCE.createString("Guide"),
                JsonOps.INSTANCE.createString("entity_type"), JsonOps.INSTANCE.createString("minecraft:villager"),
                JsonOps.INSTANCE.createString("spawn_x"), JsonOps.INSTANCE.createInt(0),
                JsonOps.INSTANCE.createString("spawn_y"), JsonOps.INSTANCE.createInt(64),
                JsonOps.INSTANCE.createString("spawn_z"), JsonOps.INSTANCE.createInt(0),
                JsonOps.INSTANCE.createString("waypoints"), JsonOps.INSTANCE.createList(java.util.stream.Stream.empty()),
                JsonOps.INSTANCE.createString("revision"), JsonOps.INSTANCE.createInt(0)
        ));
        NpcDefinition npc = NpcDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertFalse(npc.stationary());
        assertNull(npc.dialogueId());
        assertTrue(npc.repeatPath());
    }

    @Test
    void dialogueDefinitionRoundTrip() {
        DialogueDefinition original = new DialogueDefinition(
                "dialogue-1",
                "Welcome",
                List.of(
                        new DialogueLine("Hello.", 0),
                        new DialogueLine("Goodbye.", 60)
                ),
                2
        );
        DialogueDefinition decoded = roundTrip(DialogueDefinition.CODEC, original);
        assertEquals(original, decoded);
    }

    private static <T> T roundTrip(com.mojang.serialization.Codec<T> codec, T value) {
        var encoded = codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
        return codec.parse(JsonOps.INSTANCE, encoded).getOrThrow();
    }
}
