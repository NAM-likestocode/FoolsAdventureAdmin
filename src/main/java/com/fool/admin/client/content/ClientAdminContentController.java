package com.fool.admin.client.content;

import com.fool.admin.content.AdminContentConstants;
import com.fool.admin.content.AdminContentService;
import com.fool.admin.content.AdminEntityCatalog;
import com.fool.admin.content.BossDefinition;
import com.fool.admin.content.Campaign;
import com.fool.admin.content.NpcDefinition;
import com.fool.admin.content.QuestObjectiveType;
import com.fool.admin.content.QuestPoint;
import com.fool.admin.content.ZoneMask;
import com.fool.admin.network.payload.ContentMutationResultPayload;
import com.fool.admin.network.payload.ContentSnapshotPayload;
import com.fool.admin.network.payload.DeleteContentPayload;
import com.fool.admin.network.payload.RequestContentSnapshotPayload;
import com.fool.admin.network.payload.UpsertBossPayload;
import com.fool.admin.network.payload.UpsertNpcPayload;
import com.fool.admin.network.payload.UpsertQuestPayload;
import com.fool.admin.network.payload.UpsertCampaignPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ClientAdminContentController {
    private final Consumer<Void> changeListener;
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private final Map<String, BossDefinition> bosses = new LinkedHashMap<>();
    private final Map<String, NpcDefinition> npcs = new LinkedHashMap<>();
    private final Map<String, QuestPoint> quests = new LinkedHashMap<>();
    private final Map<String, Campaign> campaigns = new LinkedHashMap<>();
    private final Map<String, String> questCampaignIds = new LinkedHashMap<>();
    private final Map<String, QuestPoint> questDrafts = new LinkedHashMap<>();
    private final Map<String, String> questDraftCampaignIds = new LinkedHashMap<>();
    private @Nullable BossDefinition bossDraft;
    private @Nullable NpcDefinition npcDraft;
    private @Nullable QuestPoint questDraft;
    private @Nullable String bossEntityTypeInput;
    private @Nullable String npcEntityTypeInput;
    private @Nullable String requiredItemInput;
    private @Nullable String selectedBossId;
    private @Nullable String selectedNpcId;
    private @Nullable String selectedQuestId;
    private @Nullable String selectedCampaignId;
    private @Nullable String linkSourceQuestId;
    private AdminTab activeTab = AdminTab.MAP;
    private AdminMapTool activeTool = AdminMapTool.PAN;
    private AdminQuestTool activeQuestTool = AdminQuestTool.PAN;
    private int brushRadius = AdminContentConstants.DEFAULT_BRUSH_RADIUS;
    private @Nullable String lastError;

    public ClientAdminContentController(Consumer<Void> changeListener) {
        this.changeListener = changeListener;
    }

    public void beginSession(ResourceKey<Level> dimension) {
        this.dimension = dimension;
        bosses.clear();
        npcs.clear();
        quests.clear();
        campaigns.clear();
        questCampaignIds.clear();
        questDrafts.clear();
        questDraftCampaignIds.clear();
        bossDraft = null;
        npcDraft = null;
        questDraft = null;
        bossEntityTypeInput = null;
        npcEntityTypeInput = null;
        requiredItemInput = null;
        selectedBossId = null;
        selectedNpcId = null;
        selectedQuestId = null;
        selectedCampaignId = null;
        linkSourceQuestId = null;
        requestSnapshot();
        notifyChanged();
    }

    public void requestSnapshot() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(new RequestContentSnapshotPayload(dimension)));
        }
    }

    public void handleSnapshot(ContentSnapshotPayload payload) {
        if (!dimension.equals(payload.dimension())) {
            return;
        }
        bosses.clear();
        npcs.clear();
        quests.clear();
        campaigns.clear();
        questCampaignIds.clear();
        for (BossDefinition boss : payload.bosses()) {
            bosses.put(boss.id(), boss);
        }
        for (NpcDefinition npc : payload.npcs()) {
            npcs.put(npc.id(), npc);
        }
        for (Campaign campaign : payload.campaigns()) {
            campaigns.put(campaign.id(), campaign);
            for (QuestPoint quest : campaign.questPoints()) {
                quests.put(quest.id(), quest);
                questCampaignIds.put(quest.id(), campaign.id());
            }
        }
        if (selectedCampaignId == null || !campaigns.containsKey(selectedCampaignId)) {
            selectedCampaignId = campaigns.isEmpty() ? null : campaigns.keySet().iterator().next();
        }
        refreshBossDraftFromSnapshot();
        refreshNpcDraftFromSnapshot();
        refreshQuestDraftFromSnapshot();
        notifyChanged();
    }

    public void handleMutation(ContentMutationResultPayload payload) {
        if (!payload.success()) {
            lastError = payload.errorCode();
            notifyChanged();
            return;
        }
        lastError = null;
        boolean refreshFromServer = false;
        if (payload.deletedId() != null) {
            refreshFromServer = quests.containsKey(payload.deletedId()) || campaigns.containsKey(payload.deletedId());
            bosses.remove(payload.deletedId());
            npcs.remove(payload.deletedId());
            quests.remove(payload.deletedId());
            questDrafts.remove(payload.deletedId());
            if (campaigns.remove(payload.deletedId()) != null) {
                selectedCampaignId = null;
                clearQuestSelection();
            }
            if (payload.deletedId().equals(selectedBossId)) {
                selectedBossId = null;
                bossDraft = null;
                bossEntityTypeInput = null;
            }
            if (payload.deletedId().equals(selectedNpcId)) {
                selectedNpcId = null;
                npcDraft = null;
                npcEntityTypeInput = null;
            }
            if (payload.deletedId().equals(selectedQuestId)) {
                selectedQuestId = null;
                questDraft = null;
                linkSourceQuestId = null;
            }
            if (linkSourceQuestId != null && linkSourceQuestId.equals(payload.deletedId())) {
                linkSourceQuestId = null;
            }
        }
        if (payload.boss() != null) {
            bosses.put(payload.boss().id(), payload.boss());
            selectedBossId = payload.boss().id();
            bossDraft = payload.boss().copyForEdit();
            bossEntityTypeInput = bossDraft.entityTypeId().toString();
        }
        if (payload.npc() != null) {
            npcs.put(payload.npc().id(), payload.npc());
            selectedNpcId = payload.npc().id();
            npcDraft = payload.npc().copyForEdit();
            npcEntityTypeInput = npcDraft.entityTypeId().toString();
        }
        if (payload.quest() != null) {
            refreshFromServer = true;
            quests.put(payload.quest().id(), payload.quest());
            if (selectedCampaignId != null) {
                questCampaignIds.put(payload.quest().id(), selectedCampaignId);
            }
            selectedQuestId = payload.quest().id();
            questDraft = payload.quest().copyForEdit();
            questDrafts.remove(payload.quest().id());
            requiredItemInput = questDraft.requiredItem() == null ? "" : questDraft.requiredItem().toString();
        }
        if (refreshFromServer) {
            requestSnapshot();
        } else {
            notifyChanged();
        }
    }

    public void setActiveTab(AdminTab tab) {
        this.activeTab = tab;
        if (tab == AdminTab.BOSSES || tab == AdminTab.NPCS) {
            activeTool = AdminMapTool.SET_SPAWN;
        } else if (tab == AdminTab.QUESTS) {
            activeQuestTool = AdminQuestTool.ADD;
        } else {
            activeTool = AdminMapTool.PAN;
        }
        notifyChanged();
    }

    public void setActiveTool(AdminMapTool tool) {
        this.activeTool = tool;
    }

    public void setActiveQuestTool(AdminQuestTool tool) {
        this.activeQuestTool = tool;
        if (tool != AdminQuestTool.LINK) {
            linkSourceQuestId = null;
        }
    }

    public void setBrushRadius(int brushRadius) {
        this.brushRadius = Math.clamp(brushRadius, AdminContentConstants.MIN_BRUSH_RADIUS, AdminContentConstants.MAX_BRUSH_RADIUS);
    }

    public void createBossDraft(int spawnX, int spawnZ) {
        Identifier entityType = AdminEntityCatalog.defaultBoss()
                .map(AdminEntityCatalog.CatalogEntry::entityTypeId)
                .orElse(Identifier.withDefaultNamespace("zombie"));
        String id = AdminContentService.newId();
        bossDraft = new BossDefinition(id, "New Boss", entityType, spawnX, 64, spawnZ, new ZoneMask(), false, 0, 64, 0, null, 0);
        bossEntityTypeInput = entityType.toString();
        selectedBossId = id;
        clearQuestSelection();
        npcDraft = null;
        selectedNpcId = null;
        activeTool = AdminMapTool.PAINT_ZONE;
        notifyChanged();
    }

    public void createNpcDraft(int spawnX, int spawnZ) {
        Identifier entityType = AdminEntityCatalog.defaultNpc()
                .map(AdminEntityCatalog.CatalogEntry::entityTypeId)
                .orElse(Identifier.withDefaultNamespace("villager"));
        String id = AdminContentService.newId();
        npcDraft = new NpcDefinition(id, "New NPC", entityType, spawnX, 64, spawnZ, List.of(), true, false, null, 0);
        npcEntityTypeInput = entityType.toString();
        selectedNpcId = id;
        clearQuestSelection();
        bossDraft = null;
        selectedBossId = null;
        activeTool = AdminMapTool.ADD_WAYPOINT;
        notifyChanged();
    }

    public void createQuestDraft(float canvasX, float canvasY) {
        if (selectedCampaignId == null) {
            return;
        }
        storeQuestDraft();
        String id = AdminContentService.newId();
        @Nullable String defaultNpcId = npcs.isEmpty() ? null : npcs.values().iterator().next().id();
        questDraft = new QuestPoint(
                id,
                "New Quest",
                canvasX,
                canvasY,
                QuestObjectiveType.TALK_TO_NPC,
                defaultNpcId,
                null,
                null,
                1,
                List.of(),
                """
                --> Hello, traveler.
                <-- [Continue]
                --> Welcome to our village.
                """,
                0
        );
        selectedQuestId = id;
        questDrafts.put(id, questDraft);
        questDraftCampaignIds.put(id, selectedCampaignId);
        requiredItemInput = "";
        bossDraft = null;
        npcDraft = null;
        selectedBossId = null;
        selectedNpcId = null;
        linkSourceQuestId = null;
        notifyChanged();
    }

    public void selectBoss(String id) {
        BossDefinition boss = bosses.get(id);
        if (boss == null) {
            return;
        }
        selectedBossId = id;
        bossDraft = boss.copyForEdit();
        bossEntityTypeInput = bossDraft.entityTypeId().toString();
        clearQuestSelection();
        npcDraft = null;
        selectedNpcId = null;
        notifyChanged();
    }

    public void selectNpc(String id) {
        NpcDefinition npc = npcs.get(id);
        if (npc == null) {
            return;
        }
        selectedNpcId = id;
        npcDraft = npc.copyForEdit();
        npcEntityTypeInput = npcDraft.entityTypeId().toString();
        clearQuestSelection();
        bossDraft = null;
        selectedBossId = null;
        notifyChanged();
    }

    public void selectQuest(String id) {
        selectQuestInternal(id, true);
    }

    /** Selects a quest for a canvas drag without rebuilding the screen mid-drag. */
    public boolean beginQuestCanvasMove(String id) {
        return selectQuestInternal(id, false);
    }

    public void finishQuestCanvasMove() {
        notifyChanged();
    }

    private boolean selectQuestInternal(String id, boolean notify) {
        storeQuestDraft();
        QuestPoint quest = questDrafts.get(id);
        if (quest == null) {
            quest = quests.get(id);
        }
        if (quest == null) {
            return false;
        }
        selectedQuestId = id;
        questDraft = quest.copyForEdit();
        if (questDraft != null) {
            requiredItemInput = questDraft.requiredItem() == null ? "" : questDraft.requiredItem().toString();
        }
        bossDraft = null;
        npcDraft = null;
        selectedBossId = null;
        selectedNpcId = null;
        if (notify) {
            notifyChanged();
        }
        return true;
    }

    public void setQuestName(String name) {
        if (questDraft == null) {
            return;
        }
        updateQuestDraft(questDraft.withName(name));
    }

    public void cycleQuestObjectiveType() {
        if (questDraft == null) {
            return;
        }
        QuestObjectiveType[] values = QuestObjectiveType.values();
        int index = questDraft.objectiveType().ordinal();
        QuestObjectiveType next;
        do {
            index = (index + 1) % values.length;
            next = values[index];
        } while (!next.isEnabledInEditor());
        updateQuestDraft(questDraft.withObjectiveType(next));
        notifyChanged();
    }

    public void cycleQuestTargetNpc() {
        if (questDraft == null || npcs.isEmpty()) {
            return;
        }
        List<String> ids = npcs.keySet().stream().toList();
        int index = questDraft.targetNpcId() == null ? -1 : ids.indexOf(questDraft.targetNpcId());
        int nextIndex = (index + 1) % ids.size();
        updateQuestDraft(questDraft.withTargetNpcId(ids.get(nextIndex)));
        notifyChanged();
    }

    public void cycleQuestTargetBoss() {
        if (questDraft == null || bosses.isEmpty()) {
            return;
        }
        List<String> ids = bosses.keySet().stream().toList();
        int index = questDraft.targetBossId() == null ? -1 : ids.indexOf(questDraft.targetBossId());
        int nextIndex = (index + 1) % ids.size();
        updateQuestDraft(questDraft.withTargetBossId(ids.get(nextIndex)));
        notifyChanged();
    }

    public void setRequiredItemInput(String raw) {
        requiredItemInput = raw;
        if (questDraft == null) {
            return;
        }
        Identifier itemId = AdminEntityCatalog.parseEntityTypeId(raw);
        updateQuestDraft(questDraft.withRequiredItem(itemId, questDraft.requiredCount()));
    }

    public void setRequiredItemCount(int count) {
        if (questDraft == null) {
            return;
        }
        updateQuestDraft(questDraft.withRequiredItem(questDraft.requiredItem(), count));
    }

    public void setQuestDialogueScript(String script) {
        if (questDraft == null) {
            return;
        }
        updateQuestDraft(questDraft.withDialogueScript(script));
    }

    public void setQuestCanvasPosition(float canvasX, float canvasY) {
        if (questDraft == null) {
            return;
        }
        updateQuestDraft(questDraft.withCanvasPosition(canvasX, canvasY));
    }

    public void setLinkSourceQuest(@Nullable String questId) {
        this.linkSourceQuestId = questId;
        notifyChanged();
    }

    public boolean handleQuestLinkClick(String targetQuestId) {
        if (activeQuestTool != AdminQuestTool.LINK || linkSourceQuestId == null) {
            return false;
        }
        if (linkSourceQuestId.equals(targetQuestId)) {
            return false;
        }
        QuestPoint source = questForEditing(linkSourceQuestId);
        if (source == null) {
            return false;
        }
        if (source.prerequisiteIds().contains(targetQuestId)) {
            linkSourceQuestId = null;
            notifyChanged();
            return true;
        }
        List<String> prerequisites = new ArrayList<>(source.prerequisiteIds());
        if (!prerequisites.contains(targetQuestId)) {
            prerequisites.add(targetQuestId);
        }
        updateQuestDraft(source.withPrerequisiteIds(prerequisites));
        linkSourceQuestId = null;
        notifyChanged();
        return true;
    }

    public void removePrerequisite(String prerequisiteId) {
        if (questDraft == null) {
            return;
        }
        List<String> prerequisites = new ArrayList<>(questDraft.prerequisiteIds());
        prerequisites.remove(prerequisiteId);
        updateQuestDraft(questDraft.withPrerequisiteIds(prerequisites));
        notifyChanged();
    }

    public void saveQuestDraft() {
        if (questDraft == null || selectedCampaignId == null) {
            return;
        }
        if (questDraft.dialogueScript().isBlank()) {
            lastError = "EMPTY_DIALOGUE_SCRIPT";
            notifyChanged();
            return;
        }
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(new UpsertQuestPayload(
                    selectedCampaignId,
                    questDraft,
                    questDraft.revision()
            )));
        }
    }

    public void deleteSelectedQuest() {
        if (selectedQuestId == null) {
            return;
        }
        if (!quests.containsKey(selectedQuestId)) {
            questDrafts.remove(selectedQuestId);
            questDraft = null;
            selectedQuestId = null;
            requiredItemInput = null;
            notifyChanged();
            return;
        }
        sendDelete(DeleteContentPayload.ContentKind.QUEST, selectedQuestId);
    }

    public void setBossName(String name) {
        if (bossDraft == null) {
            return;
        }
        bossDraft = bossDraft.withDisplayName(name);
    }

    public void setBossEntityType(Identifier entityTypeId) {
        if (bossDraft == null) {
            return;
        }
        bossDraft = bossDraft.withEntityType(entityTypeId);
    }

    public void setBossEntityTypeInput(String raw) {
        bossEntityTypeInput = raw;
        Identifier entityTypeId = AdminEntityCatalog.parseEntityTypeId(raw);
        if (entityTypeId != null) {
            setBossEntityType(entityTypeId);
        }
    }

    public void setNpcEntityTypeInput(String raw) {
        npcEntityTypeInput = raw;
        Identifier entityTypeId = AdminEntityCatalog.parseEntityTypeId(raw);
        if (entityTypeId != null) {
            setNpcEntityType(entityTypeId);
        }
    }

    public @Nullable String bossEntityTypeInput() {
        return bossEntityTypeInput;
    }

    public @Nullable String npcEntityTypeInput() {
        return npcEntityTypeInput;
    }

    public @Nullable String requiredItemInput() {
        return requiredItemInput;
    }

    public boolean applyInspectorInputs(@Nullable String name, @Nullable String entityTypeRaw, boolean boss) {
        lastError = null;
        if (boss) {
            if (bossDraft == null) {
                return false;
            }
            if (name != null) {
                setBossName(name);
            }
            if (entityTypeRaw != null) {
                bossEntityTypeInput = entityTypeRaw;
                Identifier entityTypeId = AdminEntityCatalog.parseEntityTypeId(entityTypeRaw);
                if (entityTypeId == null) {
                    lastError = "INVALID_ENTITY_TYPE";
                    notifyChanged();
                    return false;
                }
                setBossEntityType(entityTypeId);
            }
            return true;
        }

        if (npcDraft == null) {
            return false;
        }
        if (name != null) {
            setNpcName(name);
        }
        if (entityTypeRaw != null) {
            npcEntityTypeInput = entityTypeRaw;
            Identifier entityTypeId = AdminEntityCatalog.parseEntityTypeId(entityTypeRaw);
            if (entityTypeId == null) {
                lastError = "INVALID_ENTITY_TYPE";
                notifyChanged();
                return false;
            }
            setNpcEntityType(entityTypeId);
        }
        return true;
    }

    public void setNpcName(String name) {
        if (npcDraft == null) {
            return;
        }
        npcDraft = copyNpc(npcDraft, name, null, null, null, null, null, null);
    }

    public void setNpcEntityType(Identifier entityTypeId) {
        if (npcDraft == null) {
            return;
        }
        npcDraft = copyNpc(npcDraft, null, entityTypeId, null, null, null, null, null);
    }

    public void setNpcRepeatPath(boolean repeatPath) {
        if (npcDraft == null) {
            return;
        }
        npcDraft = copyNpc(npcDraft, null, null, null, null, null, repeatPath, null);
    }

    public void setNpcStationary(boolean stationary) {
        if (npcDraft == null) {
            return;
        }
        npcDraft = copyNpc(npcDraft, null, null, null, null, null, null, stationary);
    }

    public void setWaypointDwell(int index, int dwellTicks) {
        if (npcDraft == null || index < 0 || index >= npcDraft.waypoints().size()) {
            return;
        }
        int clamped = Math.clamp(dwellTicks, 0, AdminContentConstants.MAX_WAYPOINT_DWELL_TICKS);
        List<com.fool.admin.content.Waypoint> waypoints = new ArrayList<>(npcDraft.waypoints());
        com.fool.admin.content.Waypoint existing = waypoints.get(index);
        waypoints.set(index, existing.withDwellTicks(clamped));
        npcDraft = copyNpc(npcDraft, null, null, null, null, waypoints, null, null);
    }

    public void setBossSpawn(int blockX, int blockZ) {
        if (bossDraft == null) {
            return;
        }
        bossDraft = bossDraft.withSpawn(blockX, bossDraft.spawnY(), blockZ);
    }

    public void setBossAttraction(int blockX, int blockZ) {
        if (bossDraft == null) {
            return;
        }
        bossDraft = bossDraft.withAttractionPoint(blockX, bossDraft.spawnY(), blockZ);
    }

    public void setNpcSpawn(int blockX, int blockZ) {
        if (npcDraft == null) {
            return;
        }
        npcDraft = copyNpc(npcDraft, null, null, blockX, blockZ, null, null, null);
    }

    public void paintBossZone(int blockX, int blockZ, boolean erase) {
        if (bossDraft == null) {
            return;
        }
        ZoneMask zone = bossDraft.zone().copy();
        zone.paintDisc(blockX, blockZ, brushRadius, erase);
        bossDraft = bossDraft.withZone(zone);
    }

    public void addNpcWaypoint(int blockX, int blockZ) {
        if (npcDraft == null) {
            return;
        }
        List<com.fool.admin.content.Waypoint> waypoints = new ArrayList<>(npcDraft.waypoints());
        if (waypoints.size() >= AdminContentConstants.MAX_WAYPOINTS) {
            return;
        }
        waypoints.add(new com.fool.admin.content.Waypoint(blockX, npcDraft.spawnY(), blockZ, 0));
        npcDraft = copyNpc(npcDraft, null, null, null, null, waypoints, null, null);
    }

    public void removeLastNpcWaypoint() {
        if (npcDraft == null || npcDraft.waypoints().isEmpty()) {
            return;
        }
        List<com.fool.admin.content.Waypoint> waypoints = new ArrayList<>(npcDraft.waypoints());
        waypoints.removeLast();
        npcDraft = copyNpc(npcDraft, null, null, null, null, waypoints, null, null);
    }

    public void clearNpcWaypoints() {
        if (npcDraft == null) {
            return;
        }
        npcDraft = copyNpc(npcDraft, null, null, null, null, List.of(), null, null);
    }

    public void saveBossDraft() {
        if (bossDraft == null) {
            return;
        }
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(new UpsertBossPayload(bossDraft, bossDraft.revision(), true)));
        }
    }

    public void saveNpcDraft() {
        if (npcDraft == null) {
            return;
        }
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(new UpsertNpcPayload(npcDraft, npcDraft.revision(), true)));
        }
    }

    public void deleteSelectedBoss() {
        if (selectedBossId == null) {
            return;
        }
        sendDelete(DeleteContentPayload.ContentKind.BOSS, selectedBossId);
    }

    public void deleteSelectedNpc() {
        if (selectedNpcId == null) {
            return;
        }
        sendDelete(DeleteContentPayload.ContentKind.NPC, selectedNpcId);
    }

    public List<BossDefinition> bosses() {
        return List.copyOf(bosses.values());
    }

    public List<NpcDefinition> npcs() {
        return List.copyOf(npcs.values());
    }

    public List<QuestPoint> quests() {
        Map<String, QuestPoint> result = new LinkedHashMap<>();
        for (Map.Entry<String, QuestPoint> entry : quests.entrySet()) {
            if (selectedCampaignId != null && selectedCampaignId.equals(questCampaignIds.get(entry.getKey()))) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, QuestPoint> entry : questDrafts.entrySet()) {
            if (selectedCampaignId != null && selectedCampaignId.equals(questDraftCampaignIds.get(entry.getKey()))) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        if (questDraft != null) {
            result.put(questDraft.id(), questDraft);
        }
        return List.copyOf(result.values());
    }

    public @Nullable BossDefinition bossDraft() {
        return bossDraft;
    }

    public @Nullable NpcDefinition npcDraft() {
        return npcDraft;
    }

    public @Nullable QuestPoint questDraft() {
        return questDraft;
    }

    public AdminTab activeTab() {
        return activeTab;
    }

    public AdminMapTool activeTool() {
        return activeTool;
    }

    public AdminQuestTool activeQuestTool() {
        return activeQuestTool;
    }

    public int brushRadius() {
        return brushRadius;
    }

    public @Nullable String lastError() {
        return lastError;
    }

    public @Nullable String selectedBossId() {
        return selectedBossId;
    }

    public @Nullable String selectedNpcId() {
        return selectedNpcId;
    }

    public @Nullable String selectedQuestId() {
        return selectedQuestId;
    }

    public List<Campaign> campaigns() {
        return List.copyOf(campaigns.values());
    }

    public @Nullable String selectedCampaignId() {
        return selectedCampaignId;
    }

    public void selectCampaign(String id) {
        if (!campaigns.containsKey(id)) {
            return;
        }
        storeQuestDraft();
        selectedCampaignId = id;
        selectedQuestId = null;
        questDraft = null;
        linkSourceQuestId = null;
        notifyChanged();
    }

    public void createCampaign() {
        String id = AdminContentService.newId();
        selectedCampaignId = id;
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(
                    new UpsertCampaignPayload(new Campaign(id, "New Campaign", List.of(), 0), 0)
            ));
        }
    }

    public @Nullable Campaign selectedCampaign() {
        return selectedCampaignId == null ? null : campaigns.get(selectedCampaignId);
    }

    public void setCampaignName(String name) {
        Campaign campaign = selectedCampaign();
        if (campaign != null) {
            campaigns.put(campaign.id(), campaign.withName(name));
        }
    }

    public void saveSelectedCampaign() {
        Campaign campaign = selectedCampaign();
        var connection = Minecraft.getInstance().getConnection();
        if (campaign != null && connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(new UpsertCampaignPayload(campaign, campaign.revision())));
        }
    }

    public void deleteSelectedCampaign() {
        if (selectedCampaignId != null) {
            sendDelete(DeleteContentPayload.ContentKind.CAMPAIGN, selectedCampaignId);
        }
    }

    public void toggleCampaignPrerequisite(String prerequisiteCampaignId) {
        Campaign campaign = selectedCampaign();
        if (campaign == null || campaign.id().equals(prerequisiteCampaignId)) {
            return;
        }
        List<String> prerequisites = new ArrayList<>(campaign.prerequisiteCampaignIds());
        if (!prerequisites.remove(prerequisiteCampaignId)) {
            prerequisites.add(prerequisiteCampaignId);
        }
        campaigns.put(campaign.id(), campaign.withPrerequisiteCampaignIds(prerequisites));
        notifyChanged();
    }

    public void toggleCampaignUnlockQuest(String campaignId, String questId) {
        Campaign campaign = selectedCampaign();
        if (campaign == null) {
            return;
        }
        String key = campaignId + "/" + questId;
        List<String> unlocks = new ArrayList<>(campaign.unlockAfterQuestKeys());
        if (!unlocks.remove(key)) {
            unlocks.add(key);
        }
        campaigns.put(campaign.id(), campaign.withUnlockAfterQuestKeys(unlocks));
        notifyChanged();
    }

    public @Nullable String linkSourceQuestId() {
        return linkSourceQuestId;
    }

    public @Nullable QuestPoint questForEditing(String questId) {
        if (questDraft != null && questDraft.id().equals(questId)) {
            return questDraft;
        }
        QuestPoint draft = questDrafts.get(questId);
        if (draft != null) {
            return draft;
        }
        return quests.get(questId);
    }

    private void updateQuestDraft(QuestPoint updated) {
        questDrafts.put(updated.id(), updated);
        if (selectedCampaignId != null) {
            questDraftCampaignIds.put(updated.id(), selectedCampaignId);
        }
        if (questDraft != null && questDraft.id().equals(updated.id())) {
            questDraft = updated;
        }
    }

    private void storeQuestDraft() {
        if (questDraft != null) {
            questDrafts.put(questDraft.id(), questDraft);
            if (selectedCampaignId != null) {
                questDraftCampaignIds.put(questDraft.id(), selectedCampaignId);
            }
        }
    }

    private void clearQuestSelection() {
        selectedQuestId = null;
        questDraft = null;
        questDrafts.clear();
        requiredItemInput = null;
        linkSourceQuestId = null;
    }

    private void sendDelete(DeleteContentPayload.ContentKind kind, String id) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(new DeleteContentPayload(kind, id)));
        }
    }

    private void refreshBossDraftFromSnapshot() {
        if (selectedBossId == null) {
            return;
        }
        bossDraft = bosses.get(selectedBossId);
        if (bossDraft == null) {
            selectedBossId = null;
            bossEntityTypeInput = null;
            return;
        }
        bossDraft = bossDraft.copyForEdit();
        bossEntityTypeInput = bossDraft.entityTypeId().toString();
    }

    private void refreshNpcDraftFromSnapshot() {
        if (selectedNpcId == null) {
            return;
        }
        npcDraft = npcs.get(selectedNpcId);
        if (npcDraft == null) {
            selectedNpcId = null;
            npcEntityTypeInput = null;
            return;
        }
        npcDraft = npcDraft.copyForEdit();
        npcEntityTypeInput = npcDraft.entityTypeId().toString();
    }

    private void refreshQuestDraftFromSnapshot() {
        if (selectedQuestId == null) {
            return;
        }
        QuestPoint localDraft = questDrafts.get(selectedQuestId);
        if (localDraft != null) {
            questDraft = localDraft.copyForEdit();
            requiredItemInput = questDraft.requiredItem() == null ? "" : questDraft.requiredItem().toString();
            return;
        }
        questDraft = quests.get(selectedQuestId);
        if (questDraft == null) {
            selectedQuestId = null;
            requiredItemInput = null;
            return;
        }
        questDraft = questDraft.copyForEdit();
        requiredItemInput = questDraft.requiredItem() == null ? "" : questDraft.requiredItem().toString();
    }

    private static NpcDefinition copyNpc(
            NpcDefinition source,
            @Nullable String displayName,
            @Nullable Identifier entityTypeId,
            @Nullable Integer spawnX,
            @Nullable Integer spawnZ,
            @Nullable List<com.fool.admin.content.Waypoint> waypoints,
            @Nullable Boolean repeatPath,
            @Nullable Boolean stationary
    ) {
        return new NpcDefinition(
                source.id(),
                displayName != null ? displayName : source.displayName(),
                entityTypeId != null ? entityTypeId : source.entityTypeId(),
                spawnX != null ? spawnX : source.spawnX(),
                source.spawnY(),
                spawnZ != null ? spawnZ : source.spawnZ(),
                waypoints != null ? waypoints : source.waypoints(),
                repeatPath != null ? repeatPath : source.repeatPath(),
                stationary != null ? stationary : source.stationary(),
                source.boundEntityUuid(),
                source.revision()
        );
    }

    private void notifyChanged() {
        changeListener.accept(null);
    }
}
