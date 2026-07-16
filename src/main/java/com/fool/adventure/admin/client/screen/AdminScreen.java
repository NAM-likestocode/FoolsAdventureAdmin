package com.fool.adventure.admin.client.screen;

import com.fool.adventure.admin.client.AdminUiTheme;
import com.fool.adventure.admin.client.content.AdminTab;
import com.fool.adventure.admin.client.content.ClientAdminContentController;
import com.fool.adventure.admin.client.map.AdminMapTextureCache;
import com.fool.adventure.admin.client.map.ClientAdminMapController;
import com.fool.adventure.admin.content.AdminContentConstants;
import com.fool.adventure.admin.content.BossDefinition;
import com.fool.adventure.admin.content.DialogueDefinition;
import com.fool.adventure.admin.content.DialogueLine;
import com.fool.adventure.admin.content.NpcDefinition;
import com.fool.adventure.admin.content.Waypoint;
import com.fool.adventure.admin.network.payload.ContentMutationResultPayload;
import com.fool.adventure.admin.network.payload.ContentSnapshotPayload;
import com.fool.adventure.admin.network.payload.MapTilesResponsePayload;
import com.fool.adventure.admin.network.payload.OpenAdminScreenPayload;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminScreen extends Screen {
    private static @Nullable AdminScreen activeScreen;

    private final OpenAdminScreenPayload openPayload;
    private final AdminMapTextureCache textureCache;
    private final ClientAdminMapController mapController;
    private final ClientAdminContentController contentController;
    private AdminMapWidget mapWidget;
    private AdminMapWidget.MapViewState viewState;
    private @Nullable AdminInspectorScrollPanel inspectorPanel;
    private @Nullable EditBox nameBox;
    private @Nullable EditBox entityTypeBox;
    private @Nullable AdminMapToolDock mapToolDock;
    private final List<EditBox> dialogueLineBoxes = new ArrayList<>();
    private double inspectorScroll;

    public AdminScreen(OpenAdminScreenPayload openPayload) {
        super(Component.translatable("foolsadventure.admin.title"));
        this.openPayload = openPayload;
        this.textureCache = new AdminMapTextureCache(Minecraft.getInstance().getTextureManager());
        this.mapController = new ClientAdminMapController(textureCache);
        this.contentController = new ClientAdminContentController(ignored -> refreshAdminUi());
        this.viewState = new AdminMapWidget.MapViewState(
                openPayload.playerBlockX() + 0.5D,
                openPayload.playerBlockZ() + 0.5D,
                1.0D,
                Integer.MIN_VALUE,
                Integer.MIN_VALUE,
                openPayload.playerBlockX(),
                openPayload.playerBlockZ()
        );
    }

    public static @Nullable AdminScreen getActive() {
        return activeScreen;
    }

    @Override
    protected void init() {
        activeScreen = this;
        this.minecraft.mouseHandler.releaseMouse();
        mapController.beginSession(openPayload.dimension(), openPayload.sessionId());
        contentController.beginSession(openPayload.dimension());
        refreshAdminUi();
    }

    private record ScreenLayout(
            int mapX,
            int mapY,
            int mapWidth,
            int mapHeight,
            int inspectorX,
            int inspectorWidth,
            int inspectorContentY,
            int inspectorContentHeight,
            int dividerX,
            int contentBottom
    ) {
    }

    private ScreenLayout layout() {
        int mapY = mapY();
        int contentBottom = height - AdminUiTheme.STATUS_HEIGHT;
        int mapHeight = contentBottom - mapY - AdminUiTheme.PANEL_PADDING;
        int mapX = mapX();

        boolean inspectorOpen = contentController.activeTab() != AdminTab.MAP;
        int inspectorWidth = inspectorOpen ? AdminUiTheme.INSPECTOR_WIDTH : 0;
        int inspectorX = width - AdminUiTheme.PANEL_PADDING - inspectorWidth;
        int dividerX = inspectorOpen ? inspectorX - AdminUiTheme.INSPECTOR_GAP : width;
        int inspectorContentY = mapY + AdminUiTheme.INSPECTOR_HEADER_HEIGHT + AdminUiTheme.PANEL_PADDING;
        int inspectorContentHeight = contentBottom - inspectorContentY - AdminUiTheme.PANEL_PADDING;

        int mapWidth = inspectorOpen
                ? dividerX - AdminUiTheme.INSPECTOR_GAP - mapX
                : width - AdminUiTheme.PANEL_PADDING - mapX;

        return new ScreenLayout(
                mapX,
                mapY,
                Math.max(1, mapWidth),
                Math.max(1, mapHeight),
                inspectorX,
                inspectorWidth,
                inspectorContentY,
                Math.max(1, inspectorContentHeight),
                dividerX,
                contentBottom
        );
    }

    private int mapX() {
        return AdminUiTheme.NAV_WIDTH + AdminUiTheme.PANEL_PADDING;
    }

    private int mapY() {
        return AdminUiTheme.HEADER_HEIGHT + AdminUiTheme.TOOLBAR_HEIGHT + AdminUiTheme.PANEL_PADDING;
    }

    private void refreshAdminUi() {
        clearWidgets();
        nameBox = null;
        entityTypeBox = null;
        dialogueLineBoxes.clear();
        inspectorPanel = null;
        mapToolDock = null;

        ScreenLayout layout = layout();

        mapWidget = new AdminMapWidget(
                layout.mapX(),
                layout.mapY(),
                layout.mapWidth(),
                layout.mapHeight(),
                mapController,
                contentController,
                viewState.centerX(),
                viewState.centerZ(),
                state -> viewState = state
        );
        mapWidget.applyViewState(viewState);
        mapWidget.setPlayerPosition(openPayload.playerBlockX(), openPayload.playerBlockZ(), openPayload.playerYaw());
        addRenderableWidget(mapWidget);

        addNavButton(AdminUiTheme.HEADER_HEIGHT + 40, "foolsadventure.admin.nav.map", AdminTab.MAP);
        addNavButton(AdminUiTheme.HEADER_HEIGHT + 58, "foolsadventure.admin.nav.bosses", AdminTab.BOSSES);
        addNavButton(AdminUiTheme.HEADER_HEIGHT + 76, "foolsadventure.admin.nav.npcs", AdminTab.NPCS);
        addNavButton(AdminUiTheme.HEADER_HEIGHT + 94, "foolsadventure.admin.nav.quests", AdminTab.QUESTS);

        addToolbarButtons();

        if (contentController.activeTab() == AdminTab.BOSSES || contentController.activeTab() == AdminTab.NPCS) {
            addMapToolDock(layout);
        }

        if (contentController.activeTab() != AdminTab.MAP) {
            addRenderableWidget(new AdminInspectorChrome(
                    layout.inspectorX(),
                    layout.mapY(),
                    layout.inspectorWidth(),
                    layout.dividerX(),
                    layout.contentBottom(),
                    contentController.activeTab()
            ));

            inspectorPanel = new AdminInspectorScrollPanel(
                    layout.inspectorX(),
                    layout.inspectorContentY(),
                    layout.inspectorWidth(),
                    layout.inspectorContentHeight()
            );
            inspectorPanel.setScroll(inspectorScroll);
            buildInspector(inspectorPanel, layout.inspectorWidth() - 6);
            inspectorScroll = inspectorPanel.scroll();
            addRenderableWidget(inspectorPanel);
        }
    }

    private void addNavButton(int y, String translationKey, AdminTab tab) {
        boolean selected = contentController.activeTab() == tab;
        addRenderableWidget(new AdminTextButton(
                AdminUiTheme.PANEL_PADDING,
                y,
                AdminUiTheme.NAV_WIDTH - AdminUiTheme.PANEL_PADDING * 2,
                16,
                Component.translatable(translationKey),
                selected,
                false,
                () -> {
                    if (contentController.activeTab() != tab) {
                        inspectorScroll = 0.0D;
                    }
                    contentController.setActiveTab(tab);
                    refreshAdminUi();
                }
        ));
    }

    private void addToolbarButtons() {
        int buttonY = AdminUiTheme.HEADER_HEIGHT + 3;
        int buttonHeight = 18;
        int gap = 4;

        int cancelWidth = 50;
        int recenterWidth = 72;
        int zoomButtonWidth = 20;
        int rightEdge = width - AdminUiTheme.PANEL_PADDING;
        int cancelX = rightEdge - cancelWidth;
        int recenterX = cancelX - gap - recenterWidth;
        int minusX = recenterX - gap - zoomButtonWidth;
        int plusX = minusX - gap - zoomButtonWidth;

        addRenderableWidget(new AdminActionButton(plusX, buttonY, zoomButtonWidth, buttonHeight, Component.literal("+"), false, false, () ->
                mapWidget.zoomBy(0.85D, mapWidget.getX() + mapWidget.getWidth() / 2.0D, mapWidget.getY() + mapWidget.getHeight() / 2.0D)));
        addRenderableWidget(new AdminActionButton(minusX, buttonY, zoomButtonWidth, buttonHeight, Component.literal("-"), false, false, () ->
                mapWidget.zoomBy(1.15D, mapWidget.getX() + mapWidget.getWidth() / 2.0D, mapWidget.getY() + mapWidget.getHeight() / 2.0D)));
        addRenderableWidget(new AdminActionButton(recenterX, buttonY, recenterWidth, buttonHeight, Component.translatable("foolsadventure.admin.recenter"), false, false, mapWidget::recenterOnPlayer));
        addRenderableWidget(new AdminActionButton(cancelX, buttonY, cancelWidth, buttonHeight, Component.translatable("gui.cancel"), false, false, this::onClose));
    }

    private void addMapToolDock(ScreenLayout layout) {
        mapToolDock = AdminMapToolDock.create(
                layout.mapX() + AdminUiTheme.PANEL_PADDING,
                layout.contentBottom() - AdminUiTheme.STATUS_HEIGHT - AdminUiTheme.MAP_TOOL_DOCK_HEIGHT - AdminUiTheme.PANEL_PADDING,
                contentController.activeTab(),
                contentController.activeTool(),
                tool -> {
                    contentController.setActiveTool(tool);
                    if (mapToolDock != null) {
                        mapToolDock.setActiveTool(tool);
                    }
                }
        );
        addRenderableWidget(mapToolDock);
    }

    private void buildInspector(AdminInspectorScrollPanel panel, int panelWidth) {
        AdminTab tab = contentController.activeTab();
        int y = 0;

        panel.add(new AdminActionButton(
                0,
                y,
                panelWidth,
                20,
                Component.translatable("foolsadventure.admin.inspector.new"),
                false,
                false,
                () -> {
                    if (tab == AdminTab.BOSSES) {
                        contentController.createBossDraft(openPayload.playerBlockX(), openPayload.playerBlockZ());
                    } else if (tab == AdminTab.NPCS) {
                        contentController.createNpcDraft(openPayload.playerBlockX(), openPayload.playerBlockZ());
                    } else if (tab == AdminTab.QUESTS) {
                        contentController.createDialogueDraft();
                    }
                    refreshAdminUi();
                }
        ), y);
        y += 26;

        if (tab == AdminTab.BOSSES) {
            for (BossDefinition boss : contentController.bosses()) {
                if (contentController.bossDraft() != null && boss.id().equals(contentController.selectedBossId())) {
                    continue;
                }
                addInspectorEntry(panel, y, panelWidth, boss.displayName(), () -> {
                    contentController.selectBoss(boss.id());
                    refreshAdminUi();
                });
                y += 18;
            }
            if (contentController.bossDraft() != null) {
                if (y > 26) {
                    y += 4;
                }
                panel.add(new AdminFieldLabel(0, y, panelWidth, Component.translatable("foolsadventure.admin.inspector.properties")), y);
                y += 14;
                y = buildBossEditor(panel, y, panelWidth);
            }
        } else if (tab == AdminTab.NPCS) {
            for (NpcDefinition npc : contentController.npcs()) {
                if (contentController.npcDraft() != null && npc.id().equals(contentController.selectedNpcId())) {
                    continue;
                }
                addInspectorEntry(panel, y, panelWidth, npc.displayName(), () -> {
                    contentController.selectNpc(npc.id());
                    refreshAdminUi();
                });
                y += 18;
            }
            if (contentController.npcDraft() != null) {
                if (y > 26) {
                    y += 4;
                }
                panel.add(new AdminFieldLabel(0, y, panelWidth, Component.translatable("foolsadventure.admin.inspector.properties")), y);
                y += 14;
                y = buildNpcEditor(panel, y, panelWidth);
            }
        } else if (tab == AdminTab.QUESTS) {
            for (DialogueDefinition dialogue : contentController.dialogues()) {
                if (contentController.dialogueDraft() != null && dialogue.id().equals(contentController.selectedDialogueId())) {
                    continue;
                }
                addInspectorEntry(panel, y, panelWidth, dialogue.name(), () -> {
                    contentController.selectDialogue(dialogue.id());
                    refreshAdminUi();
                });
                y += 18;
            }
            if (contentController.dialogueDraft() != null) {
                if (y > 26) {
                    y += 4;
                }
                panel.add(new AdminFieldLabel(0, y, panelWidth, Component.translatable("foolsadventure.admin.inspector.properties")), y);
                y += 14;
                y = buildDialogueEditor(panel, y, panelWidth);
            }
        }
    }

    private int buildBossEditor(AdminInspectorScrollPanel panel, int y, int width) {
        BossDefinition draft = contentController.bossDraft();
        if (draft == null) {
            return y;
        }

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.name")), y);
        y += 14;
        nameBox = new EditBox(font, 0, y, width, 18, Component.empty());
        nameBox.setMaxLength(64);
        nameBox.setValue(draft.displayName());
        nameBox.setResponder(contentController::setBossName);
        panel.add(nameBox, y);
        y += 24;

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.entity_type")), y);
        y += 14;
        entityTypeBox = new EditBox(font, 0, y, width, 18, Component.empty());
        entityTypeBox.setMaxLength(128);
        entityTypeBox.setValue(contentController.bossEntityTypeInput() != null
                ? contentController.bossEntityTypeInput()
                : draft.entityTypeId().toString());
        entityTypeBox.setResponder(contentController::setBossEntityTypeInput);
        panel.add(entityTypeBox, y);
        y += 26;

        int brushHalf = (width - 4) / 2;
        panel.add(new AdminActionButton(0, y, brushHalf, 20, Component.translatable("foolsadventure.admin.inspector.brush_minus"), false, false, () -> {
            contentController.setBrushRadius(contentController.brushRadius() - 1);
            refreshAdminUi();
        }), 0, y);
        panel.add(new AdminActionButton(0, y, brushHalf, 20, Component.translatable("foolsadventure.admin.inspector.brush_plus"), false, false, () -> {
            contentController.setBrushRadius(contentController.brushRadius() + 1);
            refreshAdminUi();
        }), brushHalf + 4, y);
        y += 24;
        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.brush_size", contentController.brushRadius())), y);
        y += 18;

        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadventure.admin.inspector.save"), false, false, this::saveBossDraft), y);
        y += 26;
        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadventure.admin.inspector.delete"), true, false, contentController::deleteSelectedBoss), y);
        return y + 26;
    }

    private int buildNpcEditor(AdminInspectorScrollPanel panel, int y, int width) {
        NpcDefinition draft = contentController.npcDraft();
        if (draft == null) {
            return y;
        }

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.name")), y);
        y += 14;
        nameBox = new EditBox(font, 0, y, width, 18, Component.empty());
        nameBox.setMaxLength(64);
        nameBox.setValue(draft.displayName());
        nameBox.setResponder(contentController::setNpcName);
        panel.add(nameBox, y);
        y += 24;

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.entity_type")), y);
        y += 14;
        entityTypeBox = new EditBox(font, 0, y, width, 18, Component.empty());
        entityTypeBox.setMaxLength(128);
        entityTypeBox.setValue(contentController.npcEntityTypeInput() != null
                ? contentController.npcEntityTypeInput()
                : draft.entityTypeId().toString());
        entityTypeBox.setResponder(contentController::setNpcEntityTypeInput);
        panel.add(entityTypeBox, y);
        y += 26;

        Checkbox repeatCheckbox = Checkbox.builder(Component.translatable("foolsadventure.admin.inspector.repeat"), font)
                .pos(0, y)
                .selected(draft.repeatPath())
                .onValueChange((box, selected) -> contentController.setNpcRepeatPath(selected))
                .build();
        panel.add(repeatCheckbox, y);
        y += 24;

        Checkbox stationaryCheckbox = Checkbox.builder(Component.translatable("foolsadventure.admin.inspector.stationary"), font)
                .pos(0, y)
                .selected(draft.stationary())
                .onValueChange((box, selected) -> contentController.setNpcStationary(selected))
                .build();
        panel.add(stationaryCheckbox, y);
        y += 24;

        if (draft.dialogueId() != null) {
            String dialogueName = contentController.dialogues().stream()
                    .filter(dialogue -> draft.dialogueId().equals(dialogue.id()))
                    .map(DialogueDefinition::name)
                    .findFirst()
                    .orElse(draft.dialogueId());
            panel.add(new AdminFieldLabel(0, y, width, Component.translatable(
                    "foolsadventure.admin.inspector.npc_dialogue",
                    dialogueName
            )), y);
            y += 18;
        } else {
            panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.npc_dialogue_none")), y);
            y += 18;
        }

        if (!draft.waypoints().isEmpty()) {
            panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.waypoint_dwell")), y);
            y += 14;
            for (int index = 0; index < draft.waypoints().size(); index++) {
                Waypoint waypoint = draft.waypoints().get(index);
                int dwellSeconds = waypoint.dwellTicks() / 20;
                panel.add(new AdminFieldLabel(0, y, width, Component.translatable(
                        "foolsadventure.admin.inspector.waypoint_dwell_entry",
                        index + 1,
                        dwellSeconds
                )), y);
                y += 14;
                int buttonHalf = (width - 4) / 2;
                final int waypointIndex = index;
                panel.add(new AdminActionButton(0, y, buttonHalf, 20, Component.translatable("foolsadventure.admin.inspector.dwell_minus"), false, false, () -> {
                    Waypoint current = contentController.npcDraft().waypoints().get(waypointIndex);
                    contentController.setWaypointDwell(waypointIndex, current.dwellTicks() - 20);
                    refreshAdminUi();
                }), 0, y);
                panel.add(new AdminActionButton(0, y, buttonHalf, 20, Component.translatable("foolsadventure.admin.inspector.dwell_plus"), false, false, () -> {
                    Waypoint current = contentController.npcDraft().waypoints().get(waypointIndex);
                    contentController.setWaypointDwell(waypointIndex, current.dwellTicks() + 20);
                    refreshAdminUi();
                }), buttonHalf + 4, y);
                y += 24;
            }
        }

        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadventure.admin.inspector.undo_waypoint"), false, false, contentController::removeLastNpcWaypoint), y);
        y += 24;
        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadventure.admin.inspector.clear_waypoints"), false, false, contentController::clearNpcWaypoints), y);
        y += 28;

        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadventure.admin.inspector.save"), false, false, this::saveNpcDraft), y);
        y += 26;
        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadventure.admin.inspector.delete"), true, false, contentController::deleteSelectedNpc), y);
        return y + 26;
    }

    private int buildDialogueEditor(AdminInspectorScrollPanel panel, int y, int width) {
        DialogueDefinition draft = contentController.dialogueDraft();
        if (draft == null) {
            return y;
        }

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.name")), y);
        y += 14;
        nameBox = new EditBox(font, 0, y, width, 18, Component.empty());
        nameBox.setMaxLength(AdminContentConstants.MAX_DISPLAY_NAME_LENGTH);
        nameBox.setValue(draft.name());
        nameBox.setResponder(contentController::setDialogueName);
        panel.add(nameBox, y);
        y += 24;

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.dialogue_lines")), y);
        y += 14;

        for (int index = 0; index < draft.lines().size(); index++) {
            DialogueLine line = draft.lines().get(index);
            panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.dialogue_line", index + 1)), y);
            y += 14;
            final int lineIndex = index;
            EditBox lineBox = new EditBox(font, 0, y, width, 18, Component.empty());
            lineBox.setMaxLength(AdminContentConstants.MAX_DIALOGUE_LINE_LENGTH);
            lineBox.setValue(line.text());
            lineBox.setResponder(text -> contentController.setDialogueLineText(lineIndex, text));
            panel.add(lineBox, y);
            dialogueLineBoxes.add(lineBox);
            y += 22;

            int delaySeconds = line.delayTicks() / 20;
            panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.line_delay", delaySeconds)), y);
            y += 14;
            int buttonHalf = (width - 4) / 2;
            panel.add(new AdminActionButton(0, y, buttonHalf, 20, Component.translatable("foolsadventure.admin.inspector.delay_minus"), false, false, () -> {
                DialogueLine current = contentController.dialogueDraft().lines().get(lineIndex);
                contentController.setDialogueLineDelay(lineIndex, current.delayTicks() - 20);
                refreshAdminUi();
            }), 0, y);
            panel.add(new AdminActionButton(0, y, buttonHalf, 20, Component.translatable("foolsadventure.admin.inspector.delay_plus"), false, false, () -> {
                DialogueLine current = contentController.dialogueDraft().lines().get(lineIndex);
                contentController.setDialogueLineDelay(lineIndex, current.delayTicks() + 20);
                refreshAdminUi();
            }), buttonHalf + 4, y);
            y += 24;

            panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadventure.admin.inspector.remove_line"), false, false, () -> {
                contentController.removeDialogueLine(lineIndex);
                refreshAdminUi();
            }), y);
            y += 26;
        }

        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadventure.admin.inspector.add_line"), false, false, () -> {
            contentController.addDialogueLine();
            refreshAdminUi();
        }), y);
        y += 28;

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.assign_npcs")), y);
        y += 14;
        if (contentController.npcs().isEmpty()) {
            panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadventure.admin.inspector.assign_npcs_empty")), y);
            y += 18;
        } else {
            for (NpcDefinition npc : contentController.npcs()) {
                Checkbox assignmentCheckbox = Checkbox.builder(Component.literal(npc.displayName()), font)
                        .pos(0, y)
                        .selected(contentController.isNpcAssignedToDialogueDraft(npc.id()))
                        .onValueChange((box, selected) -> contentController.setDialogueNpcAssignment(npc.id(), selected))
                        .build();
                panel.add(assignmentCheckbox, y);
                y += 20;
            }
        }
        y += 8;

        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadventure.admin.inspector.save_dialogue"), false, false, this::saveDialogueDraft), y);
        y += 26;
        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadventure.admin.inspector.delete"), true, false, contentController::deleteSelectedDialogue), y);
        return y + 26;
    }

    private void addInspectorEntry(AdminInspectorScrollPanel panel, int y, int width, String label, Runnable action) {
        panel.add(new AdminTextButton(
                0,
                y,
                width,
                16,
                Component.literal(label),
                false,
                false,
                action
        ), y);
    }

    private void saveBossDraft() {
        if (!syncInspectorInputs(true)) {
            refreshAdminUi();
            return;
        }
        contentController.saveBossDraft();
    }

    private void saveNpcDraft() {
        if (!syncInspectorInputs(false)) {
            refreshAdminUi();
            return;
        }
        contentController.saveNpcDraft();
    }

    private void saveDialogueDraft() {
        if (nameBox != null) {
            contentController.setDialogueName(nameBox.getValue());
        }
        for (int index = 0; index < dialogueLineBoxes.size(); index++) {
            contentController.setDialogueLineText(index, dialogueLineBoxes.get(index).getValue());
        }
        contentController.saveDialogueDraft();
    }

    private boolean syncInspectorInputs(boolean boss) {
        String name = nameBox != null ? nameBox.getValue() : null;
        String entityType = entityTypeBox != null ? entityTypeBox.getValue() : null;
        return contentController.applyInspectorInputs(name, entityType, boss);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (inspectorPanel != null) {
            Optional<GuiEventListener> child = inspectorPanel.getChildAt(event.x(), event.y());
            if (child.isPresent()) {
                setFocused(child.get());
                if (child.get().mouseClicked(event, doubleClick)) {
                    return true;
                }
            } else if (inspectorPanel.isMouseOver(event.x(), event.y()) && inspectorPanel.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (mapWidget != null && mapWidget.mouseReleased(event)) {
            return true;
        }
        if (inspectorPanel != null && inspectorPanel.mouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (mapWidget != null && mapWidget.mouseDragged(event, dragX, dragY)) {
            return true;
        }
        if (inspectorPanel != null && inspectorPanel.mouseDragged(event, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        if (inspectorPanel != null) {
            Optional<GuiEventListener> inspectorHit = inspectorPanel.getChildAt(mouseX, mouseY);
            if (inspectorHit.isPresent()) {
                return inspectorHit;
            }
            if (inspectorPanel.isMouseOver(mouseX, mouseY)) {
                return Optional.of(inspectorPanel);
            }
        }

        List<? extends GuiEventListener> widgets = this.children();
        for (int index = widgets.size() - 1; index >= 0; index--) {
            GuiEventListener widget = widgets.get(index);
            if (widget == inspectorPanel) {
                continue;
            }
            if (widget.isMouseOver(mouseX, mouseY)) {
                return Optional.of(widget);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inspectorPanel != null && inspectorPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            inspectorScroll = inspectorPanel.scroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (inspectorPanel != null && inspectorPanel.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (inspectorPanel != null && inspectorPanel.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.requestCursor(CursorTypes.ARROW);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.requestCursor(CursorTypes.ARROW);
    }

    @Override
    public void removed() {
        if (activeScreen == this) {
            activeScreen = null;
        }
        textureCache.close();
        super.removed();
    }

    public void onTilesResponse(MapTilesResponsePayload payload) {
        if (!openPayload.dimension().equals(payload.dimension())) {
            return;
        }
        mapController.handleTilesResponse(payload);
    }

    public void onContentSnapshot(ContentSnapshotPayload payload) {
        contentController.handleSnapshot(payload);
    }

    public void onContentMutation(ContentMutationResultPayload payload) {
        contentController.handleMutation(payload);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ScreenLayout layout = layout();

        graphics.fill(0, 0, width, height, AdminUiTheme.FRAME);
        graphics.fill(0, 0, width, AdminUiTheme.HEADER_HEIGHT, AdminUiTheme.HEADER);
        graphics.fill(AdminUiTheme.NAV_WIDTH, AdminUiTheme.HEADER_HEIGHT, width, AdminUiTheme.HEADER_HEIGHT + AdminUiTheme.TOOLBAR_HEIGHT, AdminUiTheme.FRAME_LIGHT);
        graphics.fill(0, AdminUiTheme.HEADER_HEIGHT + AdminUiTheme.TOOLBAR_HEIGHT, AdminUiTheme.NAV_WIDTH, height - AdminUiTheme.STATUS_HEIGHT, AdminUiTheme.NAV);
        graphics.fill(AdminUiTheme.NAV_WIDTH, height - AdminUiTheme.STATUS_HEIGHT, width, height, AdminUiTheme.STATUS);

        graphics.text(font, getTitle(), AdminUiTheme.PANEL_PADDING, 10, AdminUiTheme.TEXT, false);

        int navY = AdminUiTheme.HEADER_HEIGHT + 112;
        for (String key : new String[]{
                "foolsadventure.admin.nav.protection",
                "foolsadventure.admin.nav.dungeons"
        }) {
            graphics.text(font, Component.translatable(key), AdminUiTheme.PANEL_PADDING, navY, AdminUiTheme.NAV_DISABLED, false);
            navY += 16;
        }

        if (contentController.activeTab() == AdminTab.MAP) {
            graphics.text(
                    font,
                    Component.translatable("foolsadventure.admin.inspector.map_hint"),
                    AdminUiTheme.PANEL_PADDING,
                    navY + 8,
                    AdminUiTheme.TEXT_MUTED,
                    false
            );
        }

        if (contentController.lastError() != null) {
            int errorX = contentController.activeTab() == AdminTab.MAP
                    ? layout.mapX()
                    : layout.inspectorX();
            graphics.text(
                    font,
                    Component.translatable("foolsadventure.admin.error." + contentController.lastError().toLowerCase()),
                    errorX,
                    height - AdminUiTheme.STATUS_HEIGHT - 28,
                    AdminUiTheme.PLAYER_MARKER,
                    false
            );
        }

        renderStatusBar(graphics, layout);
    }

    private static final int STATUS_SEGMENT_GAP = 16;

    private void renderStatusBar(GuiGraphicsExtractor graphics, ScreenLayout layout) {
        int y = height - AdminUiTheme.STATUS_HEIGHT + 7;
        int x = AdminUiTheme.NAV_WIDTH + AdminUiTheme.PANEL_PADDING;

        int zoomPercent = (int) Math.round(100.0D / viewState.blocksPerPixel());
        Component zoomText = Component.translatable("foolsadventure.admin.status.zoom", zoomPercent);
        int zoomWidth = font.width(zoomText);
        int zoomX = width - zoomWidth - AdminUiTheme.PANEL_PADDING;
        int maxX = contentController.activeTab() == AdminTab.MAP
                ? zoomX - AdminUiTheme.PANEL_PADDING
                : layout.dividerX() - AdminUiTheme.PANEL_PADDING;

        ResourceKey<Level> dimension = openPayload.dimension();
        Component dimensionText = Component.translatable("foolsadventure.admin.status.dimension", dimension.identifier().toString());
        x = drawStatusSegment(graphics, dimensionText, x, y, maxX);

        String cursor = viewState.cursorBlockX() == Integer.MIN_VALUE
                ? "-"
                : viewState.cursorBlockX() + ", " + viewState.cursorBlockZ();
        Component cursorText = Component.translatable("foolsadventure.admin.status.cursor", cursor);
        x = drawStatusSegment(graphics, cursorText, x, y, maxX);

        Component playerText = Component.translatable(
                "foolsadventure.admin.status.player",
                viewState.playerBlockX(),
                viewState.playerBlockZ()
        );
        drawStatusSegment(graphics, playerText, x, y, maxX);

        graphics.text(font, zoomText, zoomX, y, AdminUiTheme.TEXT_MUTED, false);
    }

    private int drawStatusSegment(GuiGraphicsExtractor graphics, Component text, int x, int y, int maxX) {
        int segmentWidth = font.width(text);
        if (x + segmentWidth > maxX) {
            return x;
        }
        graphics.text(font, text, x, y, AdminUiTheme.TEXT_MUTED, false);
        return x + segmentWidth + STATUS_SEGMENT_GAP;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
