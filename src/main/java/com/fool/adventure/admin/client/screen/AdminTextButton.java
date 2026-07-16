package com.fool.adventure.admin.client.screen;

import com.fool.adventure.admin.client.AdminUiTheme;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public class AdminTextButton extends AbstractWidget {
    private final Runnable onPress;
    private final boolean selected;
    private final boolean muted;

    public AdminTextButton(int x, int y, int width, int height, Component message, boolean selected, boolean muted, Runnable onPress) {
        super(x, y, width, height, message);
        this.selected = selected;
        this.muted = muted;
        this.onPress = onPress;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int color = muted
                ? AdminUiTheme.NAV_DISABLED
                : selected
                ? AdminUiTheme.ACCENT
                : isHovered()
                ? AdminUiTheme.ACCENT_HOVER
                : AdminUiTheme.TEXT;

        graphics.text(Minecraft.getInstance().font, getMessage(), getX(), getY() + 4, color, false);
        if (isHovered() && !muted) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!muted) {
            onPress.run();
        }
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (!muted) {
            super.playDownSound(soundManager);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
