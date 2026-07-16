package com.fool.admin.client.screen;

import com.fool.admin.client.AdminUiTheme;
import com.fool.admin.network.payload.OpenNpcQuestCampaignsPayload;
import com.fool.admin.network.payload.StartNpcQuestPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

import java.util.List;

/** Lets a player choose the active campaign whose next NPC quest should begin. */
public final class NpcQuestCampaignScreen extends Screen {
    private final String npcId;
    private final String npcDisplayName;
    private final List<OpenNpcQuestCampaignsPayload.CampaignOption> campaigns;

    public NpcQuestCampaignScreen(
            String npcId,
            String npcDisplayName,
            List<OpenNpcQuestCampaignsPayload.CampaignOption> campaigns
    ) {
        super(Component.literal("Choose quest campaign"));
        this.npcId = npcId;
        this.npcDisplayName = npcDisplayName;
        this.campaigns = List.copyOf(campaigns);
    }

    @Override
    protected void init() {
        Minecraft.getInstance().mouseHandler.releaseMouse();
        KeyMapping.releaseAll();
        clearWidgets();

        int panelWidth = 250;
        int panelHeight = Math.max(88, campaigns.size() * 24 + 86);
        int x = width / 2 - panelWidth / 2;
        int y = height / 2 - panelHeight / 2 + 48;
        for (OpenNpcQuestCampaignsPayload.CampaignOption campaign : campaigns) {
            addRenderableWidget(new AdminActionButton(
                    x + 12,
                    y,
                    panelWidth - 24,
                    20,
                    Component.literal(campaign.name()),
                    false,
                    false,
                    () -> selectCampaign(campaign)
            ));
            y += 24;
        }
        addRenderableWidget(new AdminActionButton(
                x + panelWidth - 92,
                y + 4,
                80,
                20,
                Component.literal("Cancel"),
                false,
                false,
                this::onClose
        ));
    }

    private void selectCampaign(OpenNpcQuestCampaignsPayload.CampaignOption campaign) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(new StartNpcQuestPayload(npcId, campaign.id())));
        }
        onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, AdminUiTheme.DIALOGUE_DIM);
        int panelWidth = 250;
        int panelHeight = Math.max(88, campaigns.size() * 24 + 86);
        int x = width / 2 - panelWidth / 2;
        int y = height / 2 - panelHeight / 2;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, AdminUiTheme.DIALOGUE_PANEL);
        graphics.text(font, Component.literal(npcDisplayName), x + 12, y + 12, AdminUiTheme.ACCENT, false);
        graphics.text(font, Component.literal("Choose a campaign"), x + 12, y + 28, AdminUiTheme.TEXT_MUTED, false);
    }

    @Override
    public void tick() {
        KeyMapping.releaseAll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
