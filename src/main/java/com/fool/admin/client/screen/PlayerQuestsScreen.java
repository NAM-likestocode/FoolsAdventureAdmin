package com.fool.admin.client.screen;

import com.fool.admin.client.AdminUiTheme;
import com.fool.admin.content.Campaign;
import com.fool.admin.content.QuestPoint;
import com.fool.admin.network.payload.SetCampaignActivePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PlayerQuestsScreen extends Screen {
    private static final int DETAIL_HEIGHT = 72;

    private final List<Campaign> campaigns;
    private final Set<String> activeCampaignIds;
    private final Set<String> completedQuestKeys;
    private @Nullable String selectedCampaignId;
    private @Nullable String selectedQuestId;
    private boolean showFinishedCampaigns;

    public PlayerQuestsScreen(List<Campaign> campaigns, List<String> activeCampaignIds, List<String> completedQuestKeys) {
        super(Component.literal("Quests"));
        this.campaigns = List.copyOf(campaigns);
        this.activeCampaignIds = new HashSet<>(activeCampaignIds);
        this.completedQuestKeys = new HashSet<>(completedQuestKeys);
        if (!campaigns.isEmpty()) {
            this.selectedCampaignId = activeCampaignIds.stream()
                    .filter(id -> campaigns.stream().anyMatch(campaign -> campaign.id().equals(id) && !campaignFinished(campaign)))
                    .findFirst()
                    .orElseGet(() -> campaigns.stream()
                            .filter(campaign -> !campaignFinished(campaign))
                            .findFirst()
                            .map(Campaign::id)
                            .orElse(null));
        }
    }

    @Override
    protected void init() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.mouseHandler.releaseMouse();
        KeyMapping.releaseAll();

        clearWidgets();
        int left = width / 2 - 220;
        int right = width / 2 + 12;
        int listBottom = height - 64 - DETAIL_HEIGHT;
        int y = 52;
        List<Campaign> visibleCampaigns = visibleCampaigns();
        ensureSelectedCampaign(visibleCampaigns);

        for (Campaign campaign : visibleCampaigns) {
            if (y + 16 > listBottom) {
                break;
            }
            boolean selected = campaign.id().equals(selectedCampaignId);
            boolean active = activeCampaignIds.contains(campaign.id());
            boolean available = campaignAvailable(campaign);
            addRenderableWidget(new AdminTextButton(
                    left,
                    y,
                    180,
                    16,
                    Component.literal(campaign.name() + (active ? " [ON]" : available ? "" : " [LOCKED]")),
                    selected,
                    false,
                    () -> {
                        selectedCampaignId = campaign.id();
                        selectedQuestId = null;
                        rebuildWidgets();
                    }
            ));
            y += 18;
        }

        y = 52;
        Campaign selected = selectedCampaign();
        if (selected != null) {
            boolean active = activeCampaignIds.contains(selected.id());
            boolean available = campaignAvailable(selected);
            if (!campaignFinished(selected)) {
                addRenderableWidget(new AdminActionButton(
                        left,
                        height - 56,
                        180,
                        20,
                        Component.literal(active ? "Deactivate Campaign" : available ? "Activate Campaign" : "Campaign Locked"),
                        false,
                        active,
                        () -> {
                            if (active || available) {
                                toggleCampaign(selected);
                            }
                        }
                ));
            }

            for (QuestPoint quest : selected.questPoints()) {
                if (y + 16 > listBottom) {
                    break;
                }
                String status = questStatus(quest, selected);
                boolean selectedQuest = quest.id().equals(selectedQuestId);
                addRenderableWidget(new AdminTextButton(
                        right,
                        y,
                        200,
                        16,
                        Component.literal(status + " " + quest.name()),
                        selectedQuest,
                        false,
                        () -> {
                            selectedQuestId = quest.id();
                            rebuildWidgets();
                        }
                ));
                y += 18;
            }
        }

        addRenderableWidget(new AdminActionButton(
                12,
                height - 32,
                180,
                20,
                Component.literal(showFinishedCampaigns ? "Back to active quests" : "Finished quests"),
                false,
                showFinishedCampaigns,
                () -> {
                    showFinishedCampaigns = !showFinishedCampaigns;
                    selectedQuestId = null;
                    selectedCampaignId = null;
                    rebuildWidgets();
                }
        ));
        addRenderableWidget(new AdminActionButton(
                width / 2 + 120,
                height - 56,
                80,
                20,
                Component.literal("Close"),
                false,
                false,
                this::onClose
        ));
    }

    private void toggleCampaign(Campaign campaign) {
        boolean active = !activeCampaignIds.contains(campaign.id());
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(new SetCampaignActivePayload(campaign.id(), active)));
        }
        if (active) {
            activeCampaignIds.add(campaign.id());
        } else {
            activeCampaignIds.remove(campaign.id());
        }
        rebuildWidgets();
    }

    private @Nullable Campaign selectedCampaign() {
        if (selectedCampaignId == null) {
            return null;
        }
        return campaigns.stream().filter(campaign -> campaign.id().equals(selectedCampaignId)).findFirst().orElse(null);
    }

    private @Nullable QuestPoint selectedQuest() {
        Campaign campaign = selectedCampaign();
        if (campaign == null || selectedQuestId == null) {
            return null;
        }
        return campaign.questPoints().stream().filter(quest -> quest.id().equals(selectedQuestId)).findFirst().orElse(null);
    }

    private String questStatus(QuestPoint quest, Campaign campaign) {
        if (completedQuestKeys.contains(questKey(campaign, quest))) {
            return "[Done]";
        }
        if (!activeCampaignIds.contains(campaign.id())) {
            return "[Inactive]";
        }
        if (!prerequisitesMet(quest)) {
            return "[Locked]";
        }
        return "[Available]";
    }

    private boolean prerequisitesMet(QuestPoint quest) {
        Campaign campaign = selectedCampaign();
        if (campaign == null) {
            return false;
        }
        for (String prerequisiteId : quest.prerequisiteIds()) {
            if (!completedQuestKeys.contains(questKey(campaign, prerequisiteId))) {
                return false;
            }
        }
        return true;
    }

    private boolean campaignAvailable(Campaign campaign) {
        for (String prerequisiteCampaignId : campaign.prerequisiteCampaignIds()) {
            Campaign prerequisite = campaigns.stream()
                    .filter(other -> other.id().equals(prerequisiteCampaignId))
                    .findFirst()
                    .orElse(null);
            if (prerequisite == null || prerequisite.questPoints().stream()
                    .anyMatch(quest -> !completedQuestKeys.contains(questKey(prerequisite, quest)))) {
                return false;
            }
        }
        return completedQuestKeys.containsAll(campaign.unlockAfterQuestKeys());
    }

    private boolean campaignFinished(Campaign campaign) {
        return !campaign.questPoints().isEmpty()
                && campaign.questPoints().stream().allMatch(quest -> completedQuestKeys.contains(questKey(campaign, quest)));
    }

    private List<Campaign> visibleCampaigns() {
        return campaigns.stream()
                .filter(campaign -> campaignFinished(campaign) == showFinishedCampaigns)
                .toList();
    }

    private void ensureSelectedCampaign(List<Campaign> visibleCampaigns) {
        if (selectedCampaignId != null && visibleCampaigns.stream().anyMatch(campaign -> campaign.id().equals(selectedCampaignId))) {
            return;
        }
        selectedCampaignId = visibleCampaigns.isEmpty() ? null : visibleCampaigns.getFirst().id();
        selectedQuestId = null;
    }

    private static String questKey(Campaign campaign, QuestPoint quest) {
        return questKey(campaign, quest.id());
    }

    private static String questKey(Campaign campaign, String questId) {
        return campaign.id() + "/" + questId;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, AdminUiTheme.DIALOGUE_DIM);
        int left = width / 2 - 230;
        int right = width / 2 + 230;
        graphics.fill(left, 20, right, height - 20, AdminUiTheme.DIALOGUE_PANEL);
        graphics.fill(width / 2 - 8, 44, width / 2 - 7, height - 64, AdminUiTheme.DIALOGUE_DIVIDER);

        graphics.text(font, Component.literal(showFinishedCampaigns ? "Finished Campaigns" : "Campaigns"), left + 12, 28, AdminUiTheme.ACCENT, false);
        graphics.text(font, Component.literal("Quests"), width / 2 + 12, 28, AdminUiTheme.ACCENT, false);

        if (visibleCampaigns().isEmpty()) {
            String emptyMessage = showFinishedCampaigns
                    ? "No campaigns have been finished yet."
                    : "No active campaigns are available.";
            graphics.text(font, Component.literal(emptyMessage), left + 12, 56, AdminUiTheme.TEXT_MUTED, false);
            return;
        }

        QuestPoint quest = selectedQuest();
        if (quest != null) {
            Campaign campaign = selectedCampaign();
            String status = campaign == null ? "" : questStatus(quest, campaign);
            int detailY = height - 64 - DETAIL_HEIGHT + 8;
            graphics.text(font, Component.literal(quest.name()), width / 2 + 12, detailY, AdminUiTheme.TEXT, false);
            graphics.text(font, Component.literal("Status: " + status), width / 2 + 12, detailY + 14, AdminUiTheme.TEXT_MUTED, false);
            graphics.text(
                    font,
                    Component.literal("Objective: " + quest.objectiveType().name().toLowerCase().replace('_', ' ')),
                    width / 2 + 12,
                    detailY + 28,
                    AdminUiTheme.TEXT_MUTED,
                    false
            );
            if (quest.targetNpcId() != null) {
                String instruction = quest.objectiveType() == com.fool.admin.content.QuestObjectiveType.ITEM_TO_NPC
                        ? "Bring " + quest.requiredCount() + " " + quest.requiredItem() + " to the assigned NPC."
                        : "Talk to the assigned NPC in the world.";
                graphics.text(font, Component.literal(instruction), width / 2 + 12, detailY + 42, AdminUiTheme.TEXT_MUTED, false);
            }
        } else if (selectedCampaign() != null && selectedCampaign().questPoints().isEmpty()) {
            graphics.text(font, Component.literal("This campaign has no quest points yet."), width / 2 + 12, 56, AdminUiTheme.TEXT_MUTED, false);
        } else if (selectedCampaign() != null) {
            graphics.text(font, Component.literal("Select a quest to view details."), width / 2 + 12, height - 64 - DETAIL_HEIGHT + 22, AdminUiTheme.TEXT_MUTED, false);
        }
        Campaign selectedCampaign = selectedCampaign();
        if (selectedCampaign != null && !campaignAvailable(selectedCampaign)) {
            graphics.text(font, Component.literal(campaignLockReason(selectedCampaign)), left + 12, height - 82, AdminUiTheme.TEXT_MUTED, false);
        }
    }

    @Override
    public void tick() {
        KeyMapping.releaseAll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String campaignLockReason(Campaign campaign) {
        if (!campaign.prerequisiteCampaignIds().isEmpty()) {
            return "Finish a prerequisite campaign to unlock this story.";
        }
        return "Complete the required quest to unlock this story.";
    }
}
