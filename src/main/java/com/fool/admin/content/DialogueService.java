package com.fool.admin.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DialogueService {
    private static final Map<UUID, ActiveSession> ACTIVE_PLAYERS = new HashMap<>();
    private static final Map<UUID, PendingSelection> PENDING_SELECTIONS = new HashMap<>();
    private static final Set<String> BUSY_NPCS = new HashSet<>();

    private DialogueService() {
    }

    static void resetForTesting() {
        ACTIVE_PLAYERS.clear();
        PENDING_SELECTIONS.clear();
        BUSY_NPCS.clear();
    }

    public static boolean beginOverlay(UUID playerId, String npcDefinitionId, String campaignId, String questId) {
        return beginOverlay(playerId, npcDefinitionId, campaignId, questId, null);
    }

    public static boolean beginOverlay(
            UUID playerId,
            String npcDefinitionId,
            String campaignId,
            String questId,
            @Nullable ItemReservation itemReservation
    ) {
        ActiveSession existing = ACTIVE_PLAYERS.get(playerId);
        if (existing != null) {
            return false;
        }
        ACTIVE_PLAYERS.put(playerId, new ActiveSession(npcDefinitionId, campaignId, questId, itemReservation));
        BUSY_NPCS.add(npcDefinitionId);
        return true;
    }

    public static @Nullable ActiveSession endOverlay(UUID playerId) {
        ActiveSession session = ACTIVE_PLAYERS.remove(playerId);
        if (session != null) {
            BUSY_NPCS.remove(session.npcDefinitionId());
        }
        return session;
    }

    public static @Nullable String activeQuestId(UUID playerId) {
        ActiveSession session = ACTIVE_PLAYERS.get(playerId);
        return session == null ? null : session.questId();
    }

    public static boolean hasActiveOverlay(UUID playerId) {
        return ACTIVE_PLAYERS.containsKey(playerId);
    }

    public static void beginCampaignSelection(UUID playerId, String npcDefinitionId, Set<String> campaignIds) {
        PENDING_SELECTIONS.put(playerId, new PendingSelection(npcDefinitionId, Set.copyOf(campaignIds)));
    }

    public static boolean consumeCampaignSelection(UUID playerId, String npcDefinitionId, String campaignId) {
        PendingSelection pending = PENDING_SELECTIONS.get(playerId);
        if (pending == null || !pending.npcDefinitionId().equals(npcDefinitionId) || !pending.campaignIds().contains(campaignId)) {
            return false;
        }
        PENDING_SELECTIONS.remove(playerId);
        return true;
    }

    public static boolean isNpcInDialogue(String npcDefinitionId) {
        return BUSY_NPCS.contains(npcDefinitionId);
    }

    public static void facePlayer(Mob mob, ServerLevel level) {
        var nearest = level.getNearestPlayer(
                mob,
                AdminContentConstants.NPC_FACE_PLAYER_RANGE
        );
        if (!(nearest instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        mob.getLookControl().setLookAt(serverPlayer, 30.0F, 30.0F);
    }

    public static @Nullable Entity findNpcEntity(ServerLevel level, NpcDefinition definition) {
        if (definition.boundEntityUuid() == null) {
            return null;
        }
        return level.getEntity(definition.boundEntityUuid());
    }

    public record ActiveSession(
            String npcDefinitionId,
            String campaignId,
            String questId,
            @Nullable ItemReservation itemReservation
    ) {
    }

    public record ItemReservation(Identifier itemId, int count) {
    }

    private record PendingSelection(String npcDefinitionId, Set<String> campaignIds) {
    }
}
