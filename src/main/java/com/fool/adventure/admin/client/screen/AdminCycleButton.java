package com.fool.adventure.admin.client.screen;

import com.fool.adventure.admin.client.AdminUiTheme;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class AdminCycleButton<T> extends AbstractWidget {
    private final List<T> values;
    private final Function<T, Component> label;
    private final Consumer<T> onChange;
    private int index;

    public AdminCycleButton(
            int x,
            int y,
            int width,
            int height,
            List<T> values,
            T initial,
            Function<T, Component> label,
            Consumer<T> onChange
    ) {
        super(x, y, width, height, Component.empty());
        this.values = List.copyOf(values);
        this.label = label;
        this.onChange = onChange;
        this.index = values.indexOf(initial);
        if (this.index < 0) {
            this.index = 0;
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int fill = isHovered() ? AdminUiTheme.FRAME : AdminUiTheme.HEADER;
        int border = AdminUiTheme.MAP_BORDER;
        int textColor = isHovered() ? AdminUiTheme.ACCENT_HOVER : AdminUiTheme.TEXT;

        graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, border);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, border);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);

        var font = Minecraft.getInstance().font;
        Component value = label.apply(values.get(index));
        int textWidth = font.width(value);
        int textX = getX() + Math.max(AdminUiTheme.PANEL_PADDING / 2, (width - textWidth) / 2);
        graphics.text(font, value, textX, getY() + (height - 8) / 2, textColor, false);

        if (isHovered()) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        index = (index + 1) % values.size();
        onChange.accept(values.get(index));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
