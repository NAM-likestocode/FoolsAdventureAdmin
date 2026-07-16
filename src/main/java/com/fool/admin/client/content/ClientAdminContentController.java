package com.fool.admin.client.content;

import com.fool.admin.content.AdminContentConstants;
import com.fool.admin.content.AdminContentService;
import com.fool.admin.content.AdminEntityCatalog;
import com.fool.admin.content.BossDefinition;
import com.fool.admin.content.DialogueDefinition;
import com.fool.admin.content.DialogueLine;
import com.fool.admin.content.NpcDefinition;
import com.fool.admin.content.Waypoint;
import com.fool.admin.content.ZoneMask;
import com.fool.admin.network.payload.ContentMutationResultPayload;
import com.fool.admin.network.payload.ContentSnapshotPayload;
import com.fool.admin.network.payload.DeleteContentPayload;
import com.fool.admin.network.payload.RequestContentSnapshotPayload;
import com.fool.admin.network.payload.UpsertBossPayload;
import com.fool.admin.network.payload.UpsertDialoguePayload;
import com.fool.admin.network.payload.UpsertNpcPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ClientAdminContentController {
    private final Consumer<Void> changeListener;
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private final Map<String, BossDefinition> bosses = new LinkedHashMap<>();
    private final Map<String, NpcDefinition> npcs = new LinkedHashMap<>();
    private final Map<String, DialogueDefinition> dialogues = new LinkedHashMap<>();
    private @Nullable BossDefinition bossDraft;
    private @Nullable NpcDefinition npcDraft;
    private @Nullable DialogueDefinition dialogueDraft;
    private @Nullable String bossEntityTypeInput;
    private @Nullable String npcEntityTypeInput;
    private @Nullable String selectedBossId;
    private @Nullable String selectedNpcId;
    private @Nullable String selectedDialogueId;
    private final Set<String> assignedNpcIds = new LinkedHashSet<>();
    private AdminTab activeTab = AdminTab.MAP;
    private AdminMapTool activeTool = AdminMapTool.PAN;
    private int brushRadius = AdminContentConstants.DEFAULT_BRUSH_RADIUS;
    private @Nullable String lastError;

    public ClientAdminContentController(Consumer<Void> changeListener) {
        this.changeListener = changeListener;
    }

    public void beginSession(ResourceKey<Level> dimension) {
        this.dimension = dimension;
        bosses.clear();
        npcs.clear();
        dialogues.clear();
        bossDraft = null;
        npcDraft = null;
        dialogueDraft = null;
        bossEntityTypeInput = null;
        npcEntityTypeInput = null;
        selectedBossId = null;
        selectedNpcId = null;
        selectedDialogueId = null;
        assignedNpcIds.clear();
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
        dialogues.clear();
        for (BossDefinition boss : payload.bosses()) {
            bosses.put(boss.id(), boss);
        }
        for (NpcDefinition npc : payload.npcs()) {
            npcs.put(npc.id(), npc);
        }
        for (DialogueDefinition dialogue : payload.dialogues()) {
            dialogues.put(dialogue.id(), dialogue);
        }
        refreshBossDraftFromSnapshot();
        refreshNpcDraftFromSnapshot();
        refreshDialogueDraftFromSnapshot();
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
            refreshFromServer = dialogues.containsKey(payload.deletedId());
            bosses.remove(payload.deletedId());
            npcs.remove(payload.deletedId());
            dialogues.remove(payload.deletedId());
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
            if (payload.deletedId().equals(selectedDialogueId)) {
                selectedDialogueId = null;
                dialogueDraft = null;
                assignedNpcIds.clear();
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
        if (payload.dialogue() != null) {
            refreshFromServer = true;
            dialogues.put(payload.dialogue().id(), payload.dialogue());
            selectedDialogueId = payload.dialogue().id();
            dialogueDraft = payload.dialogue().copyForEdit();
            refreshAssignedNpcIds();
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
        } else {
            activeTool = AdminMapTool.PAN;
        }
        notifyChanged();
    }

    public void setActiveTool(AdminMapTool tool) {
        this.activeTool = tool;
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
        npcDraft = null;
        dialogueDraft = null;
        selectedNpcId = null;
        selectedDialogueId = null;
        assignedNpcIds.clear();
        activeTool = AdminMapTool.PAINT_ZONE;
        notifyChanged();
    }

    public void createNpcDraft(int spawnX, int spawnZ) {
        Identifier entityType = AdminEntityCatalog.defaultNpc()
                .map(AdminEntityCatalog.CatalogEntry::entityTypeId)
                .orElse(Identifier.withDefaultNamespace("villager"));
        String id = AdminContentService.newId();
        npcDraft = new NpcDefinition(id, "New NPC", entityType, spawnX, 64, spawnZ, List.of(), true, false, null, null, 0);
        npcEntityTypeInput = entityType.toString();
        selectedNpcId = id;
        bossDraft = null;
        dialogueDraft = null;
        selectedBossId = null;
        selectedDialogueId = null;
        assignedNpcIds.clear();
        activeTool = AdminMapTool.ADD_WAYPOINT;
        notifyChanged();
    }

    public void createDialogueDraft() {
        String id = AdminContentService.newId();
        dialogueDraft = new DialogueDefinition(
                id,
                "New Dialogue",
                List.of(new DialogueLine("Hello, traveler.", 0)),
                0
        );
        selectedDialogueId = id;
        assignedNpcIds.clear();
        bossDraft = null;
        npcDraft = null;
        selectedBossId = null;
        selectedNpcId = null;
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
        clearOtherSelectionsExceptBoss();
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
        clearOtherSelectionsExceptNpc();
        notifyChanged();
    }

    public void selectDialogue(String id) {
        DialogueDefinition dialogue = dialogues.get(id);
        if (dialogue == null) {
            return;
        }
        selectedDialogueId = id;
        dialogueDraft = dialogue.copyForEdit();
        refreshAssignedNpcIds();
        clearOtherSelectionsExceptDialogue();
        notifyChanged();
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

    public void setDialogueName(String name) {
        if (dialogueDraft == null) {
            return;
        }
        dialogueDraft = new DialogueDefinition(dialogueDraft.id(), name, dialogueDraft.lines(), dialogueDraft.revision());
    }

    public void setDialogueLineText(int index, String text) {
        if (dialogueDraft == null || index < 0 || index >= dialogueDraft.lines().size()) {
            return;
        }
        List<DialogueLine> lines = new ArrayList<>(dialogueDraft.lines());
        DialogueLine existing = lines.get(index);
        lines.set(index, new DialogueLine(text, existing.delayTicks()));
        dialogueDraft = new DialogueDefinition(dialogueDraft.id(), dialogueDraft.name(), lines, dialogueDraft.revision());
    }

    public void setDialogueLineDelay(int index, int delayTicks) {
        if (dialogueDraft == null || index < 0 || index >= dialogueDraft.lines().size()) {
            return;
        }
        int clamped = Math.clamp(delayTicks, AdminContentConstants.MIN_LINE_DELAY_TICKS, AdminContentConstants.MAX_LINE_DELAY_TICKS);
        List<DialogueLine> lines = new ArrayList<>(dialogueDraft.lines());
        DialogueLine existing = lines.get(index);
        lines.set(index, new DialogueLine(existing.text(), clamped));
        dialogueDraft = new DialogueDefinition(dialogueDraft.id(), dialogueDraft.name(), lines, dialogueDraft.revision());
    }

    public void addDialogueLine() {
        if (dialogueDraft == null || dialogueDraft.lines().size() >= AdminContentConstants.MAX_DIALOGUE_LINES) {
            return;
        }
        List<DialogueLine> lines = new ArrayList<>(dialogueDraft.lines());
        lines.add(new DialogueLine("", AdminContentConstants.DEFAULT_LINE_DELAY_TICKS));
        dialogueDraft = new DialogueDefinition(dialogueDraft.id(), dialogueDraft.name(), lines, dialogueDraft.revision());
    }

    public void removeDialogueLine(int index) {
        if (dialogueDraft == null || index < 0 || index >= dialogueDraft.lines().size()) {
            return;
        }
        List<DialogueLine> lines = new ArrayList<>(dialogueDraft.lines());
        lines.remove(index);
        dialogueDraft = new DialogueDefinition(dialogueDraft.id(), dialogueDraft.name(), lines, dialogueDraft.revision());
    }

    public void toggleDialogueNpcAssignment(String npcId) {
        setDialogueNpcAssignment(npcId, !assignedNpcIds.contains(npcId));
    }

    public void setDialogueNpcAssignment(String npcId, boolean assigned) {
        if (assigned) {
            assignedNpcIds.add(npcId);
        } else {
            assignedNpcIds.remove(npcId);
        }
    }

    public boolean isNpcAssignedToDialogueDraft(String npcId) {
        return assignedNpcIds.contains(npcId);
    }

    public @Nullable String bossEntityTypeInput() {
        return bossEntityTypeInput;
    }

    public @Nullable String npcEntityTypeInput() {
        return npcEntityTypeInput;
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
        npcDraft = copyNpc(npcDraft, name, null, null, null, null, null, null, null, null);
    }

    public void setNpcEntityType(Identifier entityTypeId) {
        if (npcDraft == null) {
            return;
        }
        npcDraft = copyNpc(npcDraft, null, entityTypeId, null, null, null, null, null, null, null);
    }

    public void setNpcRepeatPath(boolean repeatPath) {
        if (npcDraft == null) {
            return;
        }
        npcDraft = copyNpc(npcDraft, null, null, null, null, null, repeatPath, null, null, null);
    }

    public void setNpcStationary(boolean stationary) {
        if (npcDraft == null) {
            return;
        }
        npcDraft = copyNpc(npcDraft, null, null, null, null, null, null, stationary, null, null);
    }

    public void setWaypointDwell(int index, int dwellTicks) {
        if (npcDraft == null || index < 0 || index >= npcDraft.waypoints().size()) {
            return;
        }
        int clamped = Math.clamp(dwellTicks, 0, AdminContentConstants.MAX_WAYPOINT_DWELL_TICKS);
        List<Waypoint> waypoints = new ArrayList<>(npcDraft.waypoints());
        Waypoint existing = waypoints.get(index);
        waypoints.set(index, existing.withDwellTicks(clamped));
        npcDraft = copyNpc(npcDraft, null, null, null, null, waypoints, null, null, null, null);
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
        npcDraft = copyNpc(npcDraft, null, null, blockX, blockZ, null, null, null, null, null);
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
        List<Waypoint> waypoints = new ArrayList<>(npcDraft.waypoints());
        if (waypoints.size() >= AdminContentConstants.MAX_WAYPOINTS) {
            return;
        }
        waypoints.add(new Waypoint(blockX, npcDraft.spawnY(), blockZ, 0));
        npcDraft = copyNpc(npcDraft, null, null, null, null, waypoints, null, null, null, null);
    }

    public void removeLastNpcWaypoint() {
        if (npcDraft == null || npcDraft.waypoints().isEmpty()) {
            return;
        }
        List<Waypoint> waypoints = new ArrayList<>(npcDraft.waypoints());
        waypoints.removeLast();
        npcDraft = copyNpc(npcDraft, null, null, null, null, waypoints, null, null, null, null);
    }

    public void clearNpcWaypoints() {
        if (npcDraft == null) {
            return;
        }
        npcDraft = copyNpc(npcDraft, null, null, null, null, List.of(), null, null, null, null);
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

    public void saveDialogueDraft() {
        if (dialogueDraft == null) {
            return;
        }
        for (DialogueLine line : dialogueDraft.lines()) {
            if (line.text() == null || line.text().trim().isEmpty()) {
                lastError = "INVALID_DIALOGUE";
                notifyChanged();
                return;
            }
        }
        if (assignedNpcIds.isEmpty()) {
            lastError = "NO_NPCS_ASSIGNED";
            notifyChanged();
            return;
        }
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(new UpsertDialoguePayload(
                    dialogueDraft,
                    dialogueDraft.revision(),
                    List.copyOf(assignedNpcIds)
            )));
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

    public void deleteSelectedDialogue() {
        if (selectedDialogueId == null) {
            return;
        }
        sendDelete(DeleteContentPayload.ContentKind.DIALOGUE, selectedDialogueId);
    }

    public List<BossDefinition> bosses() {
        return List.copyOf(bosses.values());
    }

    public List<NpcDefinition> npcs() {
        return List.copyOf(npcs.values());
    }

    public List<DialogueDefinition> dialogues() {
        return List.copyOf(dialogues.values());
    }

    public @Nullable BossDefinition bossDraft() {
        return bossDraft;
    }

    public @Nullable NpcDefinition npcDraft() {
        return npcDraft;
    }

    public @Nullable DialogueDefinition dialogueDraft() {
        return dialogueDraft;
    }

    public AdminTab activeTab() {
        return activeTab;
    }

    public AdminMapTool activeTool() {
        return activeTool;
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

    public @Nullable String selectedDialogueId() {
        return selectedDialogueId;
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

    private void refreshDialogueDraftFromSnapshot() {
        if (selectedDialogueId == null) {
            return;
        }
        dialogueDraft = dialogues.get(selectedDialogueId);
        if (dialogueDraft == null) {
            selectedDialogueId = null;
            assignedNpcIds.clear();
            return;
        }
        dialogueDraft = dialogueDraft.copyForEdit();
        refreshAssignedNpcIds();
    }

    private void refreshAssignedNpcIds() {
        assignedNpcIds.clear();
        if (selectedDialogueId == null) {
            return;
        }
        for (NpcDefinition npc : npcs.values()) {
            if (selectedDialogueId.equals(npc.dialogueId())) {
                assignedNpcIds.add(npc.id());
            }
        }
    }

    private void clearOtherSelectionsExceptBoss() {
        selectedNpcId = null;
        npcDraft = null;
        npcEntityTypeInput = null;
        selectedDialogueId = null;
        dialogueDraft = null;
        assignedNpcIds.clear();
    }

    private void clearOtherSelectionsExceptNpc() {
        selectedBossId = null;
        bossDraft = null;
        bossEntityTypeInput = null;
        selectedDialogueId = null;
        dialogueDraft = null;
        assignedNpcIds.clear();
    }

    private void clearOtherSelectionsExceptDialogue() {
        selectedBossId = null;
        bossDraft = null;
        bossEntityTypeInput = null;
        selectedNpcId = null;
        npcDraft = null;
        npcEntityTypeInput = null;
    }

    private static NpcDefinition copyNpc(
            NpcDefinition source,
            @Nullable String displayName,
            @Nullable Identifier entityTypeId,
            @Nullable Integer spawnX,
            @Nullable Integer spawnZ,
            @Nullable List<Waypoint> waypoints,
            @Nullable Boolean repeatPath,
            @Nullable Boolean stationary,
            @Nullable String dialogueId,
            @Nullable Boolean clearDialogue
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
                clearDialogue != null && clearDialogue ? null : dialogueId != null ? dialogueId : source.dialogueId(),
                source.boundEntityUuid(),
                source.revision()
        );
    }

    private void notifyChanged() {
        changeListener.accept(null);
    }
}
