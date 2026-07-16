package com.fool.adventure.admin.content;

import com.fool.adventure.FoolsAdventure;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AdminContentService {
    public enum MutationError {
        INVALID_NAME,
        INVALID_ENTITY_TYPE,
        INVALID_REVISION,
        NOT_FOUND,
        ZONE_TOO_LARGE,
        TOO_MANY_WAYPOINTS,
        INVALID_COORDINATES,
        EMPTY_ZONE,
        INVALID_DIALOGUE
    }

    public record MutationResult(
            boolean success,
            @Nullable MutationError error,
            @Nullable BossDefinition boss,
            @Nullable NpcDefinition npc,
            @Nullable DialogueDefinition dialogue
    ) {
        public static MutationResult bossSuccess(BossDefinition boss) {
            return new MutationResult(true, null, boss, null, null);
        }

        public static MutationResult npcSuccess(NpcDefinition npc) {
            return new MutationResult(true, null, null, npc, null);
        }

        public static MutationResult dialogueSuccess(DialogueDefinition dialogue) {
            return new MutationResult(true, null, null, null, dialogue);
        }

        public static MutationResult failure(MutationError error) {
            return new MutationResult(false, error, null, null, null);
        }
    }

    private AdminContentService() {
    }

    public static AdminContentSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(AdminContentSavedData.TYPE);
    }

    public static ContentSnapshot snapshot(ServerLevel level) {
        AdminContentSavedData data = get(level);
        return new ContentSnapshot(data.bosses(), data.npcs(), data.dialogues());
    }

    public static MutationResult upsertBoss(ServerLevel level, BossDefinition draft, int expectedRevision, boolean spawnEntity) {
        if (!isValidName(draft.displayName())) {
            return MutationResult.failure(MutationError.INVALID_NAME);
        }
        if (!AdminEntityCatalog.isValidMobType(level, draft.entityTypeId())) {
            return MutationResult.failure(MutationError.INVALID_ENTITY_TYPE);
        }
        if (draft.zone().isEmpty()) {
            return MutationResult.failure(MutationError.EMPTY_ZONE);
        }
        if (draft.zone().paintedBlockCount() > AdminContentConstants.MAX_ZONE_BLOCKS) {
            return MutationResult.failure(MutationError.ZONE_TOO_LARGE);
        }
        if (draft.hasAttractionPoint() && !draft.zone().contains(draft.attractionX(), draft.attractionZ())) {
            return MutationResult.failure(MutationError.INVALID_COORDINATES);
        }

        AdminContentSavedData data = get(level);
        Optional<BossDefinition> existing = data.boss(draft.id());
        if (existing.isPresent() && existing.get().revision() != expectedRevision) {
            return MutationResult.failure(MutationError.INVALID_REVISION);
        }

        BlockPos spawn = WorldPositionResolver.resolveSurface(level, draft.spawnX(), draft.spawnZ());
        boolean hasAttraction = draft.hasAttractionPoint();
        int attractionX = draft.attractionX();
        int attractionY = draft.attractionY();
        int attractionZ = draft.attractionZ();
        if (hasAttraction) {
            BlockPos attraction = WorldPositionResolver.resolveSurface(level, draft.attractionX(), draft.attractionZ());
            attractionX = attraction.getX();
            attractionY = attraction.getY();
            attractionZ = attraction.getZ();
        }

        int revision = existing.map(value -> value.revision() + 1).orElse(1);
        BossDefinition saved = new BossDefinition(
                draft.id(),
                draft.displayName().trim(),
                draft.entityTypeId(),
                spawn.getX(),
                spawn.getY(),
                spawn.getZ(),
                draft.zone().copy(),
                hasAttraction,
                attractionX,
                attractionY,
                attractionZ,
                existing.map(BossDefinition::boundEntityUuid).orElse(null),
                revision
        );
        data.putBoss(saved);
        if (spawnEntity) {
            saved = AdminEntityService.spawnBoss(level, saved);
            data.putBoss(saved);
        }
        FoolsAdventure.LOGGER.info("Saved boss definition {}", saved.id());
        return MutationResult.bossSuccess(saved);
    }

    public static MutationResult upsertNpc(ServerLevel level, NpcDefinition draft, int expectedRevision, boolean spawnEntity) {
        if (!isValidName(draft.displayName())) {
            return MutationResult.failure(MutationError.INVALID_NAME);
        }
        if (!AdminEntityCatalog.isValidMobType(level, draft.entityTypeId())) {
            return MutationResult.failure(MutationError.INVALID_ENTITY_TYPE);
        }
        if (draft.waypoints().size() > AdminContentConstants.MAX_WAYPOINTS) {
            return MutationResult.failure(MutationError.TOO_MANY_WAYPOINTS);
        }
        for (Waypoint waypoint : draft.waypoints()) {
            if (!isValidDwellTicks(waypoint.dwellTicks())) {
                return MutationResult.failure(MutationError.INVALID_COORDINATES);
            }
        }

        AdminContentSavedData data = get(level);
        Optional<NpcDefinition> existing = data.npc(draft.id());
        if (existing.isPresent() && existing.get().revision() != expectedRevision) {
            return MutationResult.failure(MutationError.INVALID_REVISION);
        }

        @Nullable String dialogueId = draft.dialogueId();
        if (dialogueId == null) {
            dialogueId = existing.map(NpcDefinition::dialogueId).orElse(null);
        }
        if (!isValidDialogueReference(level, dialogueId)) {
            return MutationResult.failure(MutationError.INVALID_DIALOGUE);
        }

        BlockPos spawn = WorldPositionResolver.resolveSurface(level, draft.spawnX(), draft.spawnZ());
        List<Waypoint> resolvedWaypoints = new ArrayList<>();
        for (Waypoint waypoint : draft.waypoints()) {
            BlockPos resolved = WorldPositionResolver.resolveSurface(level, waypoint.x(), waypoint.z());
            resolvedWaypoints.add(new Waypoint(resolved.getX(), resolved.getY(), resolved.getZ(), waypoint.dwellTicks()));
        }

        int revision = existing.map(value -> value.revision() + 1).orElse(1);
        NpcDefinition saved = new NpcDefinition(
                draft.id(),
                draft.displayName().trim(),
                draft.entityTypeId(),
                spawn.getX(),
                spawn.getY(),
                spawn.getZ(),
                resolvedWaypoints,
                draft.repeatPath(),
                draft.stationary(),
                dialogueId,
                existing.map(NpcDefinition::boundEntityUuid).orElse(null),
                revision
        );
        data.putNpc(saved);
        if (spawnEntity) {
            saved = AdminEntityService.spawnNpc(level, saved);
            data.putNpc(saved);
        }
        FoolsAdventure.LOGGER.info("Saved NPC definition {}", saved.id());
        return MutationResult.npcSuccess(saved);
    }

    public static MutationResult upsertDialogue(ServerLevel level, DialogueDefinition draft, int expectedRevision, List<String> assignedNpcIds) {
        if (!isValidName(draft.name())) {
            return MutationResult.failure(MutationError.INVALID_NAME);
        }
        if (!isValidDialogue(draft)) {
            return MutationResult.failure(MutationError.INVALID_DIALOGUE);
        }

        AdminContentSavedData data = get(level);
        Optional<DialogueDefinition> existing = data.dialogue(draft.id());
        if (existing.isPresent() && existing.get().revision() != expectedRevision) {
            return MutationResult.failure(MutationError.INVALID_REVISION);
        }

        int revision = existing.map(value -> value.revision() + 1).orElse(1);
        List<DialogueLine> lines = draft.lines().stream()
                .map(line -> new DialogueLine(line.text().trim(), line.delayTicks()))
                .toList();
        DialogueDefinition saved = new DialogueDefinition(
                draft.id(),
                draft.name().trim(),
                lines,
                revision
        );
        data.putDialogue(saved);
        syncDialogueAssignments(level, saved.id(), assignedNpcIds);
        FoolsAdventure.LOGGER.info("Saved dialogue {} and assigned it to {} NPC(s)", saved.id(), assignedNpcIds.size());
        return MutationResult.dialogueSuccess(saved);
    }

    public static MutationResult deleteBoss(ServerLevel level, String id) {
        AdminContentSavedData data = get(level);
        Optional<BossDefinition> boss = data.boss(id);
        if (boss.isEmpty()) {
            return MutationResult.failure(MutationError.NOT_FOUND);
        }
        AdminEntityService.removeBoundEntity(level, boss.get().boundEntityUuid());
        data.removeBoss(id);
        AdminEntityService.clearBossRuntime(id);
        return MutationResult.bossSuccess(null);
    }

    public static MutationResult deleteNpc(ServerLevel level, String id) {
        AdminContentSavedData data = get(level);
        Optional<NpcDefinition> npc = data.npc(id);
        if (npc.isEmpty()) {
            return MutationResult.failure(MutationError.NOT_FOUND);
        }
        AdminEntityService.removeBoundEntity(level, npc.get().boundEntityUuid());
        data.removeNpc(id);
        AdminEntityService.clearNpcRuntime(id);
        return MutationResult.npcSuccess(null);
    }

    public static MutationResult deleteDialogue(ServerLevel level, String id) {
        AdminContentSavedData data = get(level);
        if (data.dialogue(id).isEmpty()) {
            return MutationResult.failure(MutationError.NOT_FOUND);
        }
        for (NpcDefinition npc : data.npcsWithDialogue(id)) {
            data.putNpc(npc.withDialogueId(null).withRevision(npc.revision() + 1));
        }
        data.removeDialogue(id);
        return MutationResult.dialogueSuccess(null);
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    private static void syncDialogueAssignments(ServerLevel level, String dialogueId, List<String> assignedNpcIds) {
        AdminContentSavedData data = get(level);
        Set<String> desired = new HashSet<>(assignedNpcIds);
        for (NpcDefinition npc : data.npcs()) {
            String current = npc.dialogueId();
            boolean shouldAssign = desired.contains(npc.id());
            if (dialogueId.equals(current) && !shouldAssign) {
                NpcDefinition updated = npc.withDialogueId(null).withRevision(npc.revision() + 1);
                data.putNpc(updated);
                AdminEntityService.ensureManagedTag(level, updated);
            } else if (shouldAssign && !dialogueId.equals(current)) {
                NpcDefinition updated = npc.withDialogueId(dialogueId).withRevision(npc.revision() + 1);
                data.putNpc(updated);
                AdminEntityService.ensureManagedTag(level, updated);
            }
        }
    }

    private static boolean isValidName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return !trimmed.isEmpty() && trimmed.length() <= AdminContentConstants.MAX_DISPLAY_NAME_LENGTH;
    }

    private static boolean isValidDialogue(DialogueDefinition dialogue) {
        if (dialogue.lines().isEmpty() || dialogue.lines().size() > AdminContentConstants.MAX_DIALOGUE_LINES) {
            return false;
        }
        for (DialogueLine line : dialogue.lines()) {
            if (line.text() == null) {
                return false;
            }
            String trimmed = line.text().trim();
            if (trimmed.isEmpty() || trimmed.length() > AdminContentConstants.MAX_DIALOGUE_LINE_LENGTH) {
                return false;
            }
            if (!isValidDelayTicks(line.delayTicks())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidDialogueReference(ServerLevel level, @Nullable String dialogueId) {
        if (dialogueId == null || dialogueId.isBlank()) {
            return true;
        }
        return get(level).dialogue(dialogueId).isPresent();
    }

    private static boolean isValidDelayTicks(int delayTicks) {
        return delayTicks >= AdminContentConstants.MIN_LINE_DELAY_TICKS
                && delayTicks <= AdminContentConstants.MAX_LINE_DELAY_TICKS;
    }

    private static boolean isValidDwellTicks(int dwellTicks) {
        return dwellTicks >= 0 && dwellTicks <= AdminContentConstants.MAX_WAYPOINT_DWELL_TICKS;
    }

    public record ContentSnapshot(List<BossDefinition> bosses, List<NpcDefinition> npcs, List<DialogueDefinition> dialogues) {
    }
}
