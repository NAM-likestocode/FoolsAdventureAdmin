package com.fool.admin.client.screen;

import com.fool.admin.client.AdminUiTheme;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class AdminFieldLabel extends AbstractWidget {
    public AdminFieldLabel(int x, int y, int width, Component message) {
        super(x, y, width, 12, message);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.text(Minecraft.getInstance().font, getMessage(), getX(), getY() + 2, AdminUiTheme.TEXT_MUTED, false);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
    }
}
