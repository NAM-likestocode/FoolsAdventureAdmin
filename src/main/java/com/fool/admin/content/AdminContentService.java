package com.fool.admin.content;

import com.fool.admin.FoolsAdmin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        INVALID_QUEST,
        INVALID_DIALOGUE_SCRIPT,
        INVALID_OBJECTIVE_TARGET,
        TOO_MANY_QUESTS,
        INVALID_PREREQUISITE
    }

    public record MutationResult(
            boolean success,
            @Nullable MutationError error,
            @Nullable BossDefinition boss,
            @Nullable NpcDefinition npc,
            @Nullable QuestPoint quest
    ) {
        public static MutationResult bossSuccess(BossDefinition boss) {
            return new MutationResult(true, null, boss, null, null);
        }

        public static MutationResult npcSuccess(NpcDefinition npc) {
            return new MutationResult(true, null, null, npc, null);
        }

        public static MutationResult questSuccess(QuestPoint quest) {
            return new MutationResult(true, null, null, null, quest);
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
        return new ContentSnapshot(data.bosses(), data.npcs(), data.campaigns());
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
        FoolsAdmin.LOGGER.info("Saved boss definition {}", saved.id());
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
                existing.map(NpcDefinition::boundEntityUuid).orElse(null),
                revision
        );
        data.putNpc(saved);
        if (spawnEntity) {
            saved = AdminEntityService.spawnNpc(level, saved);
            data.putNpc(saved);
        }
        FoolsAdmin.LOGGER.info("Saved NPC definition {}", saved.id());
        return MutationResult.npcSuccess(saved);
    }

    public static MutationResult upsertCampaign(ServerLevel level, Campaign draft, int expectedRevision) {
        if (!isValidName(draft.name())) {
            return MutationResult.failure(MutationError.INVALID_NAME);
        }
        AdminContentSavedData data = get(level);
        Optional<Campaign> existing = data.campaign(draft.id());
        if (existing.isPresent() && existing.get().revision() != expectedRevision) {
            return MutationResult.failure(MutationError.INVALID_REVISION);
        }
        if (!isValidCampaignRequirements(data, draft)) {
            return MutationResult.failure(MutationError.INVALID_PREREQUISITE);
        }
        Campaign saved = new Campaign(
                draft.id(),
                draft.name().trim(),
                existing.map(Campaign::questPoints).orElse(List.of()),
                draft.prerequisiteCampaignIds(),
                draft.unlockAfterQuestKeys(),
                existing.map(value -> value.revision() + 1).orElse(1)
        );
        data.putCampaign(saved);
        return new MutationResult(true, null, null, null, null);
    }

    public static MutationResult upsertQuest(ServerLevel level, String campaignId, QuestPoint draft, int expectedRevision) {
        if (!isValidName(draft.name())) {
            return MutationResult.failure(MutationError.INVALID_NAME);
        }

        AdminContentSavedData data = get(level);
        Campaign campaign = data.campaign(campaignId).orElse(null);
        if (campaign == null) {
            return MutationResult.failure(MutationError.NOT_FOUND);
        }
        if (!data.quest(campaignId, draft.id()).isPresent() && campaign.questPoints().size() >= AdminContentConstants.MAX_QUEST_POINTS) {
            return MutationResult.failure(MutationError.TOO_MANY_QUESTS);
        }

        Optional<QuestPoint> existing = data.quest(campaignId, draft.id());
        if (existing.isPresent() && existing.get().revision() != expectedRevision) {
            return MutationResult.failure(MutationError.INVALID_REVISION);
        }

        if (!isValidObjectiveTarget(level, draft)) {
            return MutationResult.failure(MutationError.INVALID_OBJECTIVE_TARGET);
        }
        if (!isValidPrerequisites(data, campaignId, draft)) {
            return MutationResult.failure(MutationError.INVALID_PREREQUISITE);
        }

        DialogueScriptParser.ParseResult parseResult = DialogueScriptParser.parse(draft.dialogueScript());
        if (!parseResult.success()) {
            return MutationResult.failure(MutationError.INVALID_DIALOGUE_SCRIPT);
        }

        int revision = existing.map(value -> value.revision() + 1).orElse(1);
        QuestPoint saved = new QuestPoint(
                draft.id(),
                draft.name().trim(),
                clampCanvas(draft.canvasX()),
                clampCanvas(draft.canvasY()),
                draft.objectiveType(),
                draft.targetNpcId(),
                draft.targetBossId(),
                draft.requiredItem(),
                Math.clamp(draft.requiredCount(), 1, AdminContentConstants.MAX_REQUIRED_ITEM_COUNT),
                List.copyOf(draft.prerequisiteIds()),
                draft.dialogueScript(),
                revision
        );
        data.putQuest(campaignId, saved);
        FoolsAdmin.LOGGER.info("Saved quest point {}", saved.id());
        return MutationResult.questSuccess(saved);
    }

    public static MutationResult upsertQuest(ServerLevel level, QuestPoint draft, int expectedRevision) {
        AdminContentSavedData data = get(level);
        if (data.campaigns().isEmpty()) {
            return MutationResult.failure(MutationError.NOT_FOUND);
        }
        return upsertQuest(level, data.campaigns().getFirst().id(), draft, expectedRevision);
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

    public static MutationResult deleteQuest(ServerLevel level, String id) {
        AdminContentSavedData data = get(level);
        if (data.quest(id).isEmpty()) {
            return MutationResult.failure(MutationError.NOT_FOUND);
        }
        for (QuestPoint quest : data.quests()) {
            if (quest.prerequisiteIds().contains(id)) {
                List<String> updated = new ArrayList<>(quest.prerequisiteIds());
                updated.remove(id);
                data.putQuest(quest.withPrerequisiteIds(updated).withRevision(quest.revision() + 1));
            }
        }
        data.removeQuest(id);
        return MutationResult.questSuccess(null);
    }

    public static MutationResult deleteCampaign(ServerLevel level, String id) {
        AdminContentSavedData data = get(level);
        if (data.campaign(id).isEmpty()) {
            return MutationResult.failure(MutationError.NOT_FOUND);
        }
        data.removeCampaign(id);
        for (Campaign campaign : data.campaigns()) {
            List<String> prerequisiteCampaignIds = new ArrayList<>(campaign.prerequisiteCampaignIds());
            prerequisiteCampaignIds.remove(id);
            List<String> unlockAfterQuestKeys = new ArrayList<>(campaign.unlockAfterQuestKeys());
            unlockAfterQuestKeys.removeIf(key -> key.startsWith(id + "/"));
            if (!prerequisiteCampaignIds.equals(campaign.prerequisiteCampaignIds())
                    || !unlockAfterQuestKeys.equals(campaign.unlockAfterQuestKeys())) {
                data.putCampaign(campaign
                        .withPrerequisiteCampaignIds(prerequisiteCampaignIds)
                        .withUnlockAfterQuestKeys(unlockAfterQuestKeys)
                        .withRevision(campaign.revision() + 1));
            }
        }
        return new MutationResult(true, null, null, null, null);
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    private static boolean isValidName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return !trimmed.isEmpty() && trimmed.length() <= AdminContentConstants.MAX_DISPLAY_NAME_LENGTH;
    }

    private static boolean isValidDwellTicks(int dwellTicks) {
        return dwellTicks >= 0 && dwellTicks <= AdminContentConstants.MAX_WAYPOINT_DWELL_TICKS;
    }

    private static float clampCanvas(float value) {
        return Math.clamp(value, AdminContentConstants.QUEST_CANVAS_MIN, AdminContentConstants.QUEST_CANVAS_MAX);
    }

    private static boolean isValidObjectiveTarget(ServerLevel level, QuestPoint quest) {
        return switch (quest.objectiveType()) {
            case TALK_TO_NPC -> quest.targetNpcId() != null && get(level).npc(quest.targetNpcId()).isPresent();
            case ITEM_TO_NPC -> quest.targetNpcId() != null
                    && get(level).npc(quest.targetNpcId()).isPresent()
                    && quest.requiredItem() != null
                    && BuiltInRegistries.ITEM.containsKey(quest.requiredItem());
            case KILL_BOSS -> quest.targetBossId() != null && get(level).boss(quest.targetBossId()).isPresent();
            case CLEAR_DUNGEON -> false;
        };
    }

    private static boolean isValidCampaignRequirements(AdminContentSavedData data, Campaign draft) {
        Set<String> prerequisiteCampaigns = new HashSet<>();
        for (String campaignId : draft.prerequisiteCampaignIds()) {
            if (campaignId == null || campaignId.isBlank() || campaignId.equals(draft.id())
                    || !prerequisiteCampaigns.add(campaignId) || data.campaign(campaignId).isEmpty()) {
                return false;
            }
        }
        Set<String> unlockQuestKeys = new HashSet<>();
        for (String questKey : draft.unlockAfterQuestKeys()) {
            int separator = questKey == null ? -1 : questKey.indexOf('/');
            if (separator <= 0 || separator == questKey.length() - 1 || !unlockQuestKeys.add(questKey)) {
                return false;
            }
            String campaignId = questKey.substring(0, separator);
            String questId = questKey.substring(separator + 1);
            if (campaignId.equals(draft.id()) || data.quest(campaignId, questId).isEmpty()) {
                return false;
            }
        }
        return !hasCampaignCycle(data, draft, draft.id(), new HashSet<>());
    }

    private static boolean hasCampaignCycle(
            AdminContentSavedData data,
            Campaign draft,
            String campaignId,
            Set<String> visiting
    ) {
        if (!visiting.add(campaignId)) {
            return true;
        }
        Campaign campaign = campaignId.equals(draft.id()) ? draft : data.campaign(campaignId).orElse(null);
        if (campaign != null) {
            for (String prerequisite : campaign.prerequisiteCampaignIds()) {
                if (hasCampaignCycle(data, draft, prerequisite, visiting)) {
                    return true;
                }
            }
        }
        visiting.remove(campaignId);
        return false;
    }

    private static boolean isValidPrerequisites(AdminContentSavedData data, String campaignId, QuestPoint quest) {
        if (quest.prerequisiteIds().size() > AdminContentConstants.MAX_QUEST_PREREQUISITES) {
            return false;
        }
        Set<String> seen = new HashSet<>();
        for (String prerequisiteId : quest.prerequisiteIds()) {
            if (prerequisiteId == null || prerequisiteId.isBlank() || prerequisiteId.equals(quest.id())) {
                return false;
            }
            if (!seen.add(prerequisiteId)) {
                return false;
            }
            if (data.quest(campaignId, prerequisiteId).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public record ContentSnapshot(List<BossDefinition> bosses, List<NpcDefinition> npcs, List<Campaign> campaigns) {
    }
}
