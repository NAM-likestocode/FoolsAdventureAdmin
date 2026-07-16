package com.fool.admin.client.screen;

import com.fool.admin.client.AdminUiTheme;
import com.fool.admin.client.content.AdminQuestTool;
import com.fool.admin.client.content.AdminTab;
import com.fool.admin.client.content.ClientAdminContentController;
import com.fool.admin.client.map.AdminMapTextureCache;
import com.fool.admin.client.map.ClientAdminMapController;
import com.fool.admin.content.AdminContentConstants;
import com.fool.admin.content.BossDefinition;
import com.fool.admin.content.Campaign;
import com.fool.admin.content.NpcDefinition;
import com.fool.admin.content.QuestObjectiveType;
import com.fool.admin.content.QuestPoint;
import com.fool.admin.content.Waypoint;
import com.fool.admin.network.payload.ContentMutationResultPayload;
import com.fool.admin.network.payload.ContentSnapshotPayload;
import com.fool.admin.network.payload.MapTilesResponsePayload;
import com.fool.admin.network.payload.OpenAdminScreenPayload;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
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
    private @Nullable AdminMapWidget mapWidget;
    private @Nullable QuestGraphWidget questGraphWidget;
    private AdminMapWidget.MapViewState viewState;
    private QuestGraphWidget.GraphViewState questViewState = new QuestGraphWidget.GraphViewState(0.0D, 0.0D, 1.0D);
    private final java.util.Map<String, QuestGraphWidget.GraphViewState> campaignViewStates = new java.util.HashMap<>();
    private @Nullable AdminInspectorScrollPanel inspectorPanel;
    private @Nullable EditBox nameBox;
    private @Nullable EditBox entityTypeBox;
    private @Nullable EditBox requiredItemBox;
    private @Nullable EditBox campaignNameBox;
    private @Nullable MultiLineEditBox questScriptBox;
    private @Nullable AdminMapToolDock mapToolDock;
    private double inspectorScroll;
    private boolean showCampaignUnlockChooser;

    public AdminScreen(OpenAdminScreenPayload openPayload) {
        super(Component.translatable("foolsadmin.admin.title"));
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
            int campaignX,
            int campaignWidth,
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
        boolean campaignsOpen = contentController.activeTab() == AdminTab.QUESTS;
        int campaignX = mapX();
        int campaignWidth = campaignsOpen ? AdminUiTheme.CAMPAIGN_SIDEBAR_WIDTH : 0;
        int mapX = campaignX + campaignWidth + (campaignsOpen ? AdminUiTheme.INSPECTOR_GAP : 0);

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
                campaignX,
                campaignWidth,
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
        requiredItemBox = null;
        campaignNameBox = null;
        questScriptBox = null;
        inspectorPanel = null;
        mapToolDock = null;
        questGraphWidget = null;

        ScreenLayout layout = layout();

        if (contentController.activeTab() == AdminTab.QUESTS) {
            String campaignId = contentController.selectedCampaignId();
            if (campaignId != null) {
                questViewState = campaignViewStates.getOrDefault(campaignId, questViewState);
            }
            questGraphWidget = new QuestGraphWidget(
                    layout.mapX(),
                    layout.mapY(),
                    layout.mapWidth(),
                    layout.mapHeight(),
                    contentController,
                    questViewState,
                    state -> {
                        questViewState = state;
                        if (contentController.selectedCampaignId() != null) {
                            campaignViewStates.put(contentController.selectedCampaignId(), state);
                        }
                    }
            );
            questGraphWidget.applyViewState(questViewState);
            addRenderableWidget(questGraphWidget);
            addCampaignSidebar(layout);
            Campaign campaign = contentController.selectedCampaign();
            if (campaign != null) {
                campaignNameBox = new EditBox(font, layout.mapX(), AdminUiTheme.HEADER_HEIGHT + 3, 150, 18, Component.empty());
                campaignNameBox.setValue(campaign.name());
                campaignNameBox.setResponder(contentController::setCampaignName);
                addRenderableWidget(campaignNameBox);
                addRenderableWidget(new AdminActionButton(
                        layout.mapX() + 154,
                        AdminUiTheme.HEADER_HEIGHT + 3,
                        48,
                        18,
                        Component.literal("Save"),
                        false,
                        false,
                        contentController::saveSelectedCampaign
                ));
            }
        } else {
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
        }

        addNavButton(AdminUiTheme.HEADER_HEIGHT + 40, "foolsadmin.admin.nav.map", AdminTab.MAP);
        addNavButton(AdminUiTheme.HEADER_HEIGHT + 58, "foolsadmin.admin.nav.bosses", AdminTab.BOSSES);
        addNavButton(AdminUiTheme.HEADER_HEIGHT + 76, "foolsadmin.admin.nav.npcs", AdminTab.NPCS);
        addNavButton(AdminUiTheme.HEADER_HEIGHT + 94, "foolsadmin.admin.nav.quests", AdminTab.QUESTS);

        addToolbarButtons();

        if (contentController.activeTab() == AdminTab.BOSSES || contentController.activeTab() == AdminTab.NPCS) {
            addMapToolDock(layout);
        }
        if (contentController.activeTab() == AdminTab.QUESTS) {
            addQuestToolButtons(layout);
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

    private void addCampaignSidebar(ScreenLayout layout) {
        int x = layout.campaignX();
        int y = layout.mapY() + 18;
        addRenderableWidget(new AdminActionButton(
                x,
                y,
                layout.campaignWidth(),
                20,
                Component.literal("+ Campaign"),
                false,
                false,
                () -> {
                    contentController.createCampaign();
                    refreshAdminUi();
                }
        ));
        y += 28;
        for (Campaign campaign : contentController.campaigns()) {
            boolean selected = campaign.id().equals(contentController.selectedCampaignId());
            addRenderableWidget(new AdminTextButton(
                    x + 2,
                    y,
                    layout.campaignWidth() - 4,
                    18,
                    Component.literal(campaign.name()),
                    selected,
                    false,
                    () -> {
                        contentController.selectCampaign(campaign.id());
                        showCampaignUnlockChooser = false;
                        refreshAdminUi();
                    }
            ));
            y += 20;
        }
        addRenderableWidget(new AdminActionButton(
                x,
                layout.contentBottom() - 26,
                layout.campaignWidth(),
                20,
                Component.literal("Delete Campaign"),
                true,
                false,
                () -> {
                    contentController.deleteSelectedCampaign();
                    refreshAdminUi();
                }
        ));
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

        addRenderableWidget(new AdminActionButton(plusX, buttonY, zoomButtonWidth, buttonHeight, Component.literal("+"), false, false, () -> {
            if (contentController.activeTab() == AdminTab.QUESTS && questGraphWidget != null) {
                questGraphWidget.zoomBy(0.85D, questGraphWidget.getX() + questGraphWidget.getWidth() / 2.0D, questGraphWidget.getY() + questGraphWidget.getHeight() / 2.0D);
            } else if (mapWidget != null) {
                mapWidget.zoomBy(0.85D, mapWidget.getX() + mapWidget.getWidth() / 2.0D, mapWidget.getY() + mapWidget.getHeight() / 2.0D);
            }
        }));
        addRenderableWidget(new AdminActionButton(minusX, buttonY, zoomButtonWidth, buttonHeight, Component.literal("-"), false, false, () -> {
            if (contentController.activeTab() == AdminTab.QUESTS && questGraphWidget != null) {
                questGraphWidget.zoomBy(1.15D, questGraphWidget.getX() + questGraphWidget.getWidth() / 2.0D, questGraphWidget.getY() + questGraphWidget.getHeight() / 2.0D);
            } else if (mapWidget != null) {
                mapWidget.zoomBy(1.15D, mapWidget.getX() + mapWidget.getWidth() / 2.0D, mapWidget.getY() + mapWidget.getHeight() / 2.0D);
            }
        }));
        addRenderableWidget(new AdminActionButton(recenterX, buttonY, recenterWidth, buttonHeight, Component.translatable("foolsadmin.admin.recenter"), false, false, () -> {
            if (contentController.activeTab() == AdminTab.QUESTS && questGraphWidget != null) {
                questViewState = new QuestGraphWidget.GraphViewState(0.0D, 0.0D, 1.0D);
                questGraphWidget.applyViewState(questViewState);
            } else if (mapWidget != null) {
                mapWidget.recenterOnPlayer();
            }
        }));
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

    private void addQuestToolButtons(ScreenLayout layout) {
        int buttonY = layout.contentBottom() - AdminUiTheme.STATUS_HEIGHT - 28;
        int x = layout.mapX() + AdminUiTheme.PANEL_PADDING;
        addRenderableWidget(new AdminActionButton(x, buttonY, 52, 20, Component.translatable("foolsadmin.admin.quest.tool.add"), false, contentController.activeQuestTool() == AdminQuestTool.ADD, () -> {
            contentController.setActiveQuestTool(AdminQuestTool.ADD);
            refreshAdminUi();
        }));
        addRenderableWidget(new AdminActionButton(x + 56, buttonY, 52, 20, Component.translatable("foolsadmin.admin.quest.tool.pan"), false, contentController.activeQuestTool() == AdminQuestTool.PAN, () -> {
            contentController.setActiveQuestTool(AdminQuestTool.PAN);
            refreshAdminUi();
        }));
        addRenderableWidget(new AdminActionButton(x + 112, buttonY, 52, 20, Component.translatable("foolsadmin.admin.quest.tool.link"), false, contentController.activeQuestTool() == AdminQuestTool.LINK, () -> {
            contentController.setActiveQuestTool(AdminQuestTool.LINK);
            refreshAdminUi();
        }));
    }

    private void buildInspector(AdminInspectorScrollPanel panel, int panelWidth) {
        AdminTab tab = contentController.activeTab();
        int y = 0;

        panel.add(new AdminActionButton(
                0,
                y,
                panelWidth,
                20,
                Component.translatable("foolsadmin.admin.inspector.new"),
                false,
                false,
                () -> {
                    if (tab == AdminTab.BOSSES) {
                        contentController.createBossDraft(openPayload.playerBlockX(), openPayload.playerBlockZ());
                    } else if (tab == AdminTab.NPCS) {
                        contentController.createNpcDraft(openPayload.playerBlockX(), openPayload.playerBlockZ());
                    } else if (tab == AdminTab.QUESTS) {
                        float canvasX = questGraphWidget != null ? questGraphWidget.centerCanvasX() : 0.0F;
                        float canvasY = questGraphWidget != null ? questGraphWidget.centerCanvasY() : 0.0F;
                        if (contentController.selectedCampaignId() == null) {
                            contentController.createCampaign();
                        } else {
                            contentController.createQuestDraft(canvasX, canvasY);
                        }
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
                panel.add(new AdminFieldLabel(0, y, panelWidth, Component.translatable("foolsadmin.admin.inspector.properties")), y);
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
                panel.add(new AdminFieldLabel(0, y, panelWidth, Component.translatable("foolsadmin.admin.inspector.properties")), y);
                y += 14;
                y = buildNpcEditor(panel, y, panelWidth);
            }
        } else if (tab == AdminTab.QUESTS) {
            if (contentController.selectedCampaignId() == null) {
                panel.add(new AdminFieldLabel(0, y, panelWidth, Component.literal("Create or select a campaign to edit its quest points.")), y);
                return;
            }
            Campaign campaign = contentController.selectedCampaign();
            if (campaign != null) {
                panel.add(new AdminFieldLabel(0, y, panelWidth, Component.literal("Campaign unlock requirements")), y);
                y += 16;
                int requirementCount = campaign.prerequisiteCampaignIds().size() + campaign.unlockAfterQuestKeys().size();
                panel.add(new AdminActionButton(
                        0,
                        y,
                        panelWidth,
                        20,
                        Component.literal(showCampaignUnlockChooser
                                ? "Done choosing requirements"
                                : "Choose requirements" + (requirementCount == 0 ? "" : " (" + requirementCount + ")")),
                        false,
                        showCampaignUnlockChooser,
                        () -> {
                            showCampaignUnlockChooser = !showCampaignUnlockChooser;
                            refreshAdminUi();
                        }
                ), y);
                y += 24;
                if (showCampaignUnlockChooser) {
                    int choiceCount = 0;
                    for (Campaign candidate : contentController.campaigns()) {
                        if (candidate.id().equals(campaign.id())) {
                            continue;
                        }
                        choiceCount++;
                        boolean required = campaign.prerequisiteCampaignIds().contains(candidate.id());
                        panel.add(new AdminActionButton(
                                0,
                                y,
                                panelWidth,
                                20,
                                Component.literal((required ? "[x] Finish " : "[ ] Finish ") + candidate.name()),
                                false,
                                required,
                                () -> {
                                    contentController.toggleCampaignPrerequisite(candidate.id());
                                    refreshAdminUi();
                                }
                        ), y);
                        y += 22;
                    }
                    for (Campaign questCampaign : contentController.campaigns()) {
                        if (questCampaign.id().equals(campaign.id())) {
                            continue;
                        }
                        for (QuestPoint unlockQuest : questCampaign.questPoints()) {
                        choiceCount++;
                        String key = questCampaign.id() + "/" + unlockQuest.id();
                        boolean required = campaign.unlockAfterQuestKeys().contains(key);
                        panel.add(new AdminActionButton(
                                0,
                                y,
                                panelWidth,
                                20,
                                Component.literal((required ? "[x] Complete " : "[ ] Complete ") + unlockQuest.name()),
                                false,
                                required,
                                () -> {
                                    contentController.toggleCampaignUnlockQuest(questCampaign.id(), unlockQuest.id());
                                    refreshAdminUi();
                                }
                        ), y);
                        y += 22;
                    }
                    }
                    if (choiceCount == 0) {
                        panel.add(new AdminFieldLabel(
                                0,
                                y,
                                panelWidth,
                                Component.literal("Add another campaign or a quest point to choose an unlock requirement.")
                        ), y);
                        y += 18;
                    }
                }
                y += 4;
            }
            for (QuestPoint quest : contentController.quests()) {
                if (contentController.questDraft() != null && quest.id().equals(contentController.selectedQuestId())) {
                    continue;
                }
                addInspectorEntry(panel, y, panelWidth, quest.name(), () -> {
                    contentController.selectQuest(quest.id());
                    refreshAdminUi();
                });
                y += 18;
            }
            if (contentController.questDraft() != null) {
                if (y > 26) {
                    y += 4;
                }
                panel.add(new AdminFieldLabel(0, y, panelWidth, Component.translatable("foolsadmin.admin.inspector.properties")), y);
                y += 14;
                y = buildQuestEditor(panel, y, panelWidth);
            }
        }
    }

    private int buildBossEditor(AdminInspectorScrollPanel panel, int y, int width) {
        BossDefinition draft = contentController.bossDraft();
        if (draft == null) {
            return y;
        }

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.name")), y);
        y += 14;
        nameBox = new EditBox(font, 0, y, width, 18, Component.empty());
        nameBox.setMaxLength(64);
        nameBox.setValue(draft.displayName());
        nameBox.setResponder(contentController::setBossName);
        panel.add(nameBox, y);
        y += 24;

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.entity_type")), y);
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
        panel.add(new AdminActionButton(0, y, brushHalf, 20, Component.translatable("foolsadmin.admin.inspector.brush_minus"), false, false, () -> {
            contentController.setBrushRadius(contentController.brushRadius() - 1);
            refreshAdminUi();
        }), 0, y);
        panel.add(new AdminActionButton(0, y, brushHalf, 20, Component.translatable("foolsadmin.admin.inspector.brush_plus"), false, false, () -> {
            contentController.setBrushRadius(contentController.brushRadius() + 1);
            refreshAdminUi();
        }), brushHalf + 4, y);
        y += 24;
        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.brush_size", contentController.brushRadius())), y);
        y += 18;

        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadmin.admin.inspector.save"), false, false, this::saveBossDraft), y);
        y += 26;
        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadmin.admin.inspector.delete"), true, false, contentController::deleteSelectedBoss), y);
        return y + 26;
    }

    private int buildNpcEditor(AdminInspectorScrollPanel panel, int y, int width) {
        NpcDefinition draft = contentController.npcDraft();
        if (draft == null) {
            return y;
        }

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.name")), y);
        y += 14;
        nameBox = new EditBox(font, 0, y, width, 18, Component.empty());
        nameBox.setMaxLength(64);
        nameBox.setValue(draft.displayName());
        nameBox.setResponder(contentController::setNpcName);
        panel.add(nameBox, y);
        y += 24;

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.entity_type")), y);
        y += 14;
        entityTypeBox = new EditBox(font, 0, y, width, 18, Component.empty());
        entityTypeBox.setMaxLength(128);
        entityTypeBox.setValue(contentController.npcEntityTypeInput() != null
                ? contentController.npcEntityTypeInput()
                : draft.entityTypeId().toString());
        entityTypeBox.setResponder(contentController::setNpcEntityTypeInput);
        panel.add(entityTypeBox, y);
        y += 26;

        Checkbox repeatCheckbox = Checkbox.builder(Component.translatable("foolsadmin.admin.inspector.repeat"), font)
                .pos(0, y)
                .selected(draft.repeatPath())
                .onValueChange((box, selected) -> contentController.setNpcRepeatPath(selected))
                .build();
        panel.add(repeatCheckbox, y);
        y += 24;

        Checkbox stationaryCheckbox = Checkbox.builder(Component.translatable("foolsadmin.admin.inspector.stationary"), font)
                .pos(0, y)
                .selected(draft.stationary())
                .onValueChange((box, selected) -> contentController.setNpcStationary(selected))
                .build();
        panel.add(stationaryCheckbox, y);
        y += 24;

        if (!draft.waypoints().isEmpty()) {
            panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.waypoint_dwell")), y);
            y += 14;
            for (int index = 0; index < draft.waypoints().size(); index++) {
                Waypoint waypoint = draft.waypoints().get(index);
                int dwellSeconds = waypoint.dwellTicks() / 20;
                panel.add(new AdminFieldLabel(0, y, width, Component.translatable(
                        "foolsadmin.admin.inspector.waypoint_dwell_entry",
                        index + 1,
                        dwellSeconds
                )), y);
                y += 14;
                int buttonHalf = (width - 4) / 2;
                final int waypointIndex = index;
                panel.add(new AdminActionButton(0, y, buttonHalf, 20, Component.translatable("foolsadmin.admin.inspector.dwell_minus"), false, false, () -> {
                    Waypoint current = contentController.npcDraft().waypoints().get(waypointIndex);
                    contentController.setWaypointDwell(waypointIndex, current.dwellTicks() - 20);
                    refreshAdminUi();
                }), 0, y);
                panel.add(new AdminActionButton(0, y, buttonHalf, 20, Component.translatable("foolsadmin.admin.inspector.dwell_plus"), false, false, () -> {
                    Waypoint current = contentController.npcDraft().waypoints().get(waypointIndex);
                    contentController.setWaypointDwell(waypointIndex, current.dwellTicks() + 20);
                    refreshAdminUi();
                }), buttonHalf + 4, y);
                y += 24;
            }
        }

        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadmin.admin.inspector.undo_waypoint"), false, false, contentController::removeLastNpcWaypoint), y);
        y += 24;
        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadmin.admin.inspector.clear_waypoints"), false, false, contentController::clearNpcWaypoints), y);
        y += 28;

        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadmin.admin.inspector.save"), false, false, this::saveNpcDraft), y);
        y += 26;
        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadmin.admin.inspector.delete"), true, false, contentController::deleteSelectedNpc), y);
        return y + 26;
    }

    private int buildQuestEditor(AdminInspectorScrollPanel panel, int y, int width) {
        QuestPoint draft = contentController.questDraft();
        if (draft == null) {
            return y;
        }

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.name")), y);
        y += 14;
        nameBox = new EditBox(font, 0, y, width, 18, Component.empty());
        nameBox.setMaxLength(AdminContentConstants.MAX_DISPLAY_NAME_LENGTH);
        nameBox.setValue(draft.name());
        nameBox.setResponder(contentController::setQuestName);
        panel.add(nameBox, y);
        y += 24;

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.quest_objective")), y);
        y += 14;
        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable(
                "foolsadmin.admin.inspector.quest_objective_value",
                Component.translatable("foolsadmin.admin.objective." + draft.objectiveType().name().toLowerCase())
        ), false, false, contentController::cycleQuestObjectiveType), y);
        y += 26;

        switch (draft.objectiveType()) {
            case TALK_TO_NPC, ITEM_TO_NPC -> {
                String npcName = draft.targetNpcId() == null
                        ? "-"
                        : contentController.npcs().stream()
                        .filter(npc -> npc.id().equals(draft.targetNpcId()))
                        .map(NpcDefinition::displayName)
                        .findFirst()
                        .orElse(draft.targetNpcId());
                panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadmin.admin.inspector.quest_target_npc", npcName), false, false, contentController::cycleQuestTargetNpc), y);
                y += 26;
            }
            case KILL_BOSS -> {
                String bossName = draft.targetBossId() == null
                        ? "-"
                        : contentController.bosses().stream()
                        .filter(boss -> boss.id().equals(draft.targetBossId()))
                        .map(BossDefinition::displayName)
                        .findFirst()
                        .orElse(draft.targetBossId());
                panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadmin.admin.inspector.quest_target_boss", bossName), false, false, contentController::cycleQuestTargetBoss), y);
                y += 26;
            }
            case CLEAR_DUNGEON -> {
                panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.quest_dungeon_disabled")), y);
                y += 18;
            }
        }

        if (draft.objectiveType() == QuestObjectiveType.ITEM_TO_NPC) {
            panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.quest_required_item")), y);
            y += 14;
            requiredItemBox = new EditBox(font, 0, y, width, 18, Component.empty());
            requiredItemBox.setMaxLength(128);
            requiredItemBox.setValue(contentController.requiredItemInput() != null ? contentController.requiredItemInput() : "");
            requiredItemBox.setResponder(contentController::setRequiredItemInput);
            panel.add(requiredItemBox, y);
            y += 24;
            panel.add(new AdminActionButton(0, y, (width - 4) / 2, 20, Component.literal("-"), false, false, () -> {
                contentController.setRequiredItemCount(draft.requiredCount() - 1);
                refreshAdminUi();
            }), 0, y);
            panel.add(new AdminActionButton(0, y, (width - 4) / 2, 20, Component.literal("+"), false, false, () -> {
                contentController.setRequiredItemCount(draft.requiredCount() + 1);
                refreshAdminUi();
            }), (width - 4) / 2 + 4, y);
            y += 24;
            panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.quest_required_count", draft.requiredCount())), y);
            y += 18;
        }

        if (!draft.prerequisiteIds().isEmpty()) {
            panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.quest_prerequisites")), y);
            y += 14;
            for (String prerequisiteId : draft.prerequisiteIds()) {
                String label = contentController.quests().stream()
                        .filter(quest -> quest.id().equals(prerequisiteId))
                        .map(QuestPoint::name)
                        .findFirst()
                        .orElse(prerequisiteId);
                final String removeId = prerequisiteId;
                panel.add(new AdminActionButton(0, y, width, 20, Component.literal("x " + label), false, false, () -> {
                    contentController.removePrerequisite(removeId);
                    refreshAdminUi();
                }), y);
                y += 22;
            }
        }

        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.dialogue_script")), y);
        y += 14;
        panel.add(new AdminFieldLabel(0, y, width, Component.translatable("foolsadmin.admin.inspector.dialogue_script_hint")), y);
        y += 14;
        questScriptBox = MultiLineEditBox.builder()
                .setPlaceholder(Component.translatable("foolsadmin.admin.inspector.dialogue_script_hint"))
                .build(font, width, 120, Component.empty());
        questScriptBox.setValue(draft.dialogueScript());
        questScriptBox.setValueListener(contentController::setQuestDialogueScript);
        panel.add(questScriptBox, y);
        y += 126;

        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadmin.admin.inspector.save_quest"), false, false, this::saveQuestDraft), y);
        y += 26;
        panel.add(new AdminActionButton(0, y, width, 20, Component.translatable("foolsadmin.admin.inspector.delete"), true, false, contentController::deleteSelectedQuest), y);
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

    private void saveQuestDraft() {
        if (nameBox != null) {
            contentController.setQuestName(nameBox.getValue());
        }
        if (questScriptBox != null) {
            contentController.setQuestDialogueScript(questScriptBox.getValue());
        }
        if (requiredItemBox != null) {
            contentController.setRequiredItemInput(requiredItemBox.getValue());
        }
        contentController.saveQuestDraft();
    }

    private boolean syncInspectorInputs(boolean boss) {
        String name = nameBox != null ? nameBox.getValue() : null;
        String entityType = entityTypeBox != null ? entityTypeBox.getValue() : null;
        return contentController.applyInspectorInputs(name, entityType, boss);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (inspectorPanel != null) {
            if (inspectorPanel.isMouseOver(event.x(), event.y()) && inspectorPanel.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (questGraphWidget != null && questGraphWidget.mouseReleased(event)) {
            return true;
        }
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
        if (questGraphWidget != null && questGraphWidget.mouseDragged(event, dragX, dragY)) {
            return true;
        }
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
        if (questGraphWidget != null && questGraphWidget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (mapWidget != null && mapWidget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
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

        if (contentController.activeTab() == AdminTab.QUESTS) {
            graphics.fill(
                    layout.campaignX() - AdminUiTheme.PANEL_PADDING,
                    layout.mapY() - AdminUiTheme.PANEL_PADDING,
                    layout.campaignX() + layout.campaignWidth() + AdminUiTheme.PANEL_PADDING,
                    layout.contentBottom(),
                    AdminUiTheme.FRAME_LIGHT
            );
            graphics.fill(
                    layout.campaignX() - AdminUiTheme.PANEL_PADDING,
                    layout.mapY() - AdminUiTheme.PANEL_PADDING,
                    layout.campaignX() + layout.campaignWidth() + AdminUiTheme.PANEL_PADDING,
                    layout.mapY() + 14,
                    AdminUiTheme.HEADER
            );
            graphics.text(font, Component.literal("Campaigns"), layout.campaignX(), layout.mapY() - 4, AdminUiTheme.ACCENT, false);
        }

        int navY = AdminUiTheme.HEADER_HEIGHT + 112;
        for (String key : new String[]{
                "foolsadmin.admin.nav.protection",
                "foolsadmin.admin.nav.dungeons"
        }) {
            graphics.text(font, Component.translatable(key), AdminUiTheme.PANEL_PADDING, navY, AdminUiTheme.NAV_DISABLED, false);
            navY += 16;
        }

        if (contentController.activeTab() == AdminTab.MAP) {
            graphics.text(
                    font,
                    Component.translatable("foolsadmin.admin.inspector.map_hint"),
                    AdminUiTheme.PANEL_PADDING,
                    navY + 8,
                    AdminUiTheme.TEXT_MUTED,
                    false
            );
        } else if (contentController.activeTab() == AdminTab.QUESTS) {
            String hintKey = switch (contentController.activeQuestTool()) {
                case ADD -> "foolsadmin.admin.quest.hint.add";
                case PAN -> "foolsadmin.admin.quest.hint.pan";
                case LINK -> "foolsadmin.admin.quest.hint.link";
            };
            graphics.text(
                    font,
                    Component.translatable(hintKey),
                    layout.mapX(),
                    layout.contentBottom() - AdminUiTheme.STATUS_HEIGHT - 46,
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
                    Component.translatable("foolsadmin.admin.error." + contentController.lastError().toLowerCase()),
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

        int zoomPercent = contentController.activeTab() == AdminTab.QUESTS
                ? (int) Math.round(100.0D / questViewState.scale())
                : (int) Math.round(100.0D / viewState.blocksPerPixel());
        Component zoomText = Component.translatable("foolsadmin.admin.status.zoom", zoomPercent);
        int zoomWidth = font.width(zoomText);
        int zoomX = width - zoomWidth - AdminUiTheme.PANEL_PADDING;
        int maxX = contentController.activeTab() == AdminTab.MAP
                ? zoomX - AdminUiTheme.PANEL_PADDING
                : layout.dividerX() - AdminUiTheme.PANEL_PADDING;

        ResourceKey<Level> dimension = openPayload.dimension();
        Component dimensionText = Component.translatable("foolsadmin.admin.status.dimension", dimension.identifier().toString());
        x = drawStatusSegment(graphics, dimensionText, x, y, maxX);

        String cursor = viewState.cursorBlockX() == Integer.MIN_VALUE
                ? "-"
                : viewState.cursorBlockX() + ", " + viewState.cursorBlockZ();
        Component cursorText = Component.translatable("foolsadmin.admin.status.cursor", cursor);
        x = drawStatusSegment(graphics, cursorText, x, y, maxX);

        Component playerText = Component.translatable(
                "foolsadmin.admin.status.player",
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
