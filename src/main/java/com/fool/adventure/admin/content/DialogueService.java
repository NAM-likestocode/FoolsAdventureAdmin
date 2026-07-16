package com.fool.adventure.admin.content;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class DialogueService {
    private static final Map<UUID, DialogueSession> SESSIONS = new HashMap<>();
    private static final Map<String, Integer> BUSY_NPCS = new HashMap<>();
    private static @Nullable Function<ServerLevel, AdminContentSavedData> savedDataOverride;

    private DialogueService() {
    }

    static void setSavedDataOverrideForTesting(@Nullable Function<ServerLevel, AdminContentSavedData> override) {
        savedDataOverride = override;
    }

    static void resetForTesting() {
        SESSIONS.clear();
        BUSY_NPCS.clear();
        savedDataOverride = null;
    }

    private static AdminContentSavedData resolveSavedData(ServerLevel level) {
        if (savedDataOverride != null) {
            return savedDataOverride.apply(level);
        }
        return AdminContentService.get(level);
    }

    public static boolean trigger(ServerPlayer player, NpcDefinition npc, DialogueDefinition dialogue) {
        UUID playerId = player.getUUID();
        DialogueSession existing = SESSIONS.get(playerId);
        if (existing != null && existing.npcDefinitionId().equals(npc.id())) {
            return true;
        }

        return beginSession(player, npc, dialogue, player.level().getGameTime());
    }

    static boolean beginSession(ServerPlayer player, NpcDefinition npc, DialogueDefinition dialogue, long gameTime) {
        if (dialogue.lines().isEmpty()) {
            return false;
        }

        UUID playerId = player.getUUID();
        endSession(playerId);

        sendLine(player, npc.displayName(), dialogue.lines().getFirst());

        if (dialogue.lines().size() == 1) {
            return true;
        }

        int nextDelay = Math.max(0, dialogue.lines().get(1).delayTicks());
        SESSIONS.put(playerId, new DialogueSession(
                npc.id(),
                dialogue.id(),
                1,
                nextDelay,
                gameTime
        ));
        BUSY_NPCS.put(npc.id(), 1);
        return true;
    }

    /** Test helper without sending chat lines. */
    static boolean beginSession(UUID playerId, NpcDefinition npc, DialogueDefinition dialogue, long gameTime) {
        if (dialogue.lines().isEmpty()) {
            return false;
        }

        endSession(playerId);
        if (dialogue.lines().size() == 1) {
            return true;
        }

        int nextDelay = Math.max(0, dialogue.lines().get(1).delayTicks());
        SESSIONS.put(playerId, new DialogueSession(
                npc.id(),
                dialogue.id(),
                1,
                nextDelay,
                gameTime
        ));
        BUSY_NPCS.put(npc.id(), 1);
        return true;
    }

    private static void sendLine(ServerPlayer player, String npcDisplayName, DialogueLine line) {
        Component message = formatLine(npcDisplayName, line.text());
        player.sendSystemMessage(message);
    }

    public static void tick(ServerLevel level) {
        Iterator<Map.Entry<UUID, DialogueSession>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DialogueSession> entry = iterator.next();
            UUID playerId = entry.getKey();
            DialogueSession session = entry.getValue();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player == null) {
                releaseBusyNpc(session.npcDefinitionId());
                iterator.remove();
                continue;
            }
            if (!player.level().dimension().equals(level.dimension())) {
                continue;
            }

            AdminContentSavedData data = resolveSavedData(level);
            DialogueDefinition dialogue = data.dialogue(session.dialogueId()).orElse(null);
            if (dialogue == null) {
                releaseBusyNpc(session.npcDefinitionId());
                iterator.remove();
                continue;
            }

            SessionAdvanceResult result = advanceSession(session, dialogue, npcName(data, session.npcDefinitionId()));
            if (result.deliveredLine() != null) {
                player.sendSystemMessage(result.deliveredLine());
            }
            if (result.sessionEnded()) {
                releaseBusyNpc(session.npcDefinitionId());
                iterator.remove();
            } else if (result.nextSession() != null) {
                entry.setValue(result.nextSession());
            }
        }
    }

    static SessionAdvanceResult advanceSession(DialogueSession session, DialogueDefinition dialogue, String npcDisplayName) {
        if (session.lineIndex() >= dialogue.lines().size()) {
            return SessionAdvanceResult.complete();
        }
        if (session.ticksUntilNextLine() > 0) {
            return SessionAdvanceResult.waiting(session.withTicksUntilNextLine(session.ticksUntilNextLine() - 1));
        }

        DialogueLine line = dialogue.lines().get(session.lineIndex());
        Component deliveredLine = formatLine(npcDisplayName, line.text());
        int nextIndex = session.lineIndex() + 1;
        if (nextIndex >= dialogue.lines().size()) {
            return SessionAdvanceResult.finished(deliveredLine);
        }

        int nextDelay = Math.max(0, dialogue.lines().get(nextIndex).delayTicks());
        DialogueSession nextSession = new DialogueSession(
                session.npcDefinitionId(),
                session.dialogueId(),
                nextIndex,
                nextDelay,
                session.startedAtGameTime()
        );
        return SessionAdvanceResult.continuing(nextSession, deliveredLine);
    }

    public static boolean isNpcInDialogue(String npcDefinitionId) {
        return BUSY_NPCS.containsKey(npcDefinitionId);
    }

    public static void endSession(UUID playerId) {
        DialogueSession session = SESSIONS.remove(playerId);
        if (session != null) {
            releaseBusyNpc(session.npcDefinitionId());
        }
    }

    private static void releaseBusyNpc(String npcDefinitionId) {
        BUSY_NPCS.remove(npcDefinitionId);
    }

    private static String npcName(AdminContentSavedData data, String npcDefinitionId) {
        return data.npc(npcDefinitionId).map(NpcDefinition::displayName).orElse("NPC");
    }

    private static Component formatLine(String npcName, String text) {
        return Component.literal("<" + npcName + "> " + text);
    }

    public static void facePlayer(Mob mob, ServerLevel level) {
        var nearest = level.getNearestPlayer(
                mob,
                AdminContentConstants.NPC_FACE_PLAYER_RANGE
        );
        if (!(nearest instanceof ServerPlayer serverPlayer)) {
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

    record DialogueSession(
            String npcDefinitionId,
            String dialogueId,
            int lineIndex,
            int ticksUntilNextLine,
            long startedAtGameTime
    ) {
        private DialogueSession withTicksUntilNextLine(int ticksUntilNextLine) {
            return new DialogueSession(npcDefinitionId, dialogueId, lineIndex, ticksUntilNextLine, startedAtGameTime);
        }
    }

    record SessionAdvanceResult(
            @Nullable DialogueSession nextSession,
            @Nullable Component deliveredLine,
            boolean sessionEnded
    ) {
        private static SessionAdvanceResult waiting(DialogueSession nextSession) {
            return new SessionAdvanceResult(nextSession, null, false);
        }

        private static SessionAdvanceResult continuing(DialogueSession nextSession, Component deliveredLine) {
            return new SessionAdvanceResult(nextSession, deliveredLine, false);
        }

        private static SessionAdvanceResult finished(Component deliveredLine) {
            return new SessionAdvanceResult(null, deliveredLine, true);
        }

        private static SessionAdvanceResult complete() {
            return new SessionAdvanceResult(null, null, true);
        }
    }
}
