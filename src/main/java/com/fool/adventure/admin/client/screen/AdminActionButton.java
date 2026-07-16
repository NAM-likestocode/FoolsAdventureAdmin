package com.fool.adventure.admin.client.screen;

import com.fool.adventure.admin.client.AdminUiTheme;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class AdminActionButton extends AbstractWidget {
    private final Runnable onPress;
    private final boolean destructive;
    private boolean selected;

    public AdminActionButton(int x, int y, int width, int height, Component message, boolean destructive, boolean selected, Runnable onPress) {
        super(x, y, width, height, message);
        this.destructive = destructive;
        this.selected = selected;
        this.onPress = onPress;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int fill = selected ? AdminUiTheme.FRAME_LIGHT : isHovered() ? AdminUiTheme.FRAME : AdminUiTheme.HEADER;
        int border = destructive ? AdminUiTheme.PLAYER_MARKER : selected ? AdminUiTheme.ACCENT : AdminUiTheme.MAP_BORDER;
        int text = destructive ? AdminUiTheme.PLAYER_MARKER : selected ? AdminUiTheme.ACCENT : isHovered() ? AdminUiTheme.ACCENT_HOVER : AdminUiTheme.TEXT;

        graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, border);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, border);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);

        var font = Minecraft.getInstance().font;
        int textWidth = font.width(getMessage());
        int textX = getX() + Math.max(AdminUiTheme.PANEL_PADDING / 2, (width - textWidth) / 2);
        graphics.text(font, getMessage(), textX, getY() + (height - 8) / 2, text, false);

        if (isHovered()) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
