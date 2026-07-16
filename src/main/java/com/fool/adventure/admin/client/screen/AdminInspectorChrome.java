package com.fool.adventure.admin.client.screen;

import com.fool.adventure.admin.client.AdminUiTheme;
import com.fool.adventure.admin.client.content.AdminTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class AdminInspectorChrome extends AbstractWidget {
    private final AdminTab activeTab;
    private final int dividerX;
    private final int contentBottom;

    public AdminInspectorChrome(int inspectorX, int mapY, int inspectorWidth, int dividerX, int contentBottom, AdminTab activeTab) {
        super(inspectorX, mapY, inspectorWidth, contentBottom - mapY, Component.empty());
        this.activeTab = activeTab;
        this.dividerX = dividerX;
        this.contentBottom = contentBottom;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int mapY = getY();
        int panelRight = getX() + width;

        graphics.fill(dividerX, mapY, dividerX + 1, contentBottom, AdminUiTheme.MAP_BORDER);
        graphics.fill(getX() - AdminUiTheme.PANEL_PADDING, mapY - AdminUiTheme.PANEL_PADDING, panelRight + AdminUiTheme.PANEL_PADDING, contentBottom, AdminUiTheme.FRAME_LIGHT);
        graphics.fill(getX() - AdminUiTheme.PANEL_PADDING, mapY - AdminUiTheme.PANEL_PADDING, panelRight + AdminUiTheme.PANEL_PADDING, mapY + AdminUiTheme.INSPECTOR_HEADER_HEIGHT, AdminUiTheme.HEADER);

        var font = Minecraft.getInstance().font;
        if (activeTab == AdminTab.MAP) {
            return;
        }
        graphics.text(
                font,
                Component.translatable("foolsadventure.admin.inspector.title"),
                getX(),
                mapY + 6,
                AdminUiTheme.ACCENT,
                false
        );
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
