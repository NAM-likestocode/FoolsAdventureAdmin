package com.fool.adventure.admin.client.screen;

import com.fool.adventure.admin.client.AdminUiTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminInspectorScrollPanel extends AbstractWidget {
    private static final class Entry {
        private final AbstractWidget widget;
        private final int relativeX;
        private final int relativeY;

        private Entry(AbstractWidget widget, int relativeX, int relativeY) {
            this.widget = widget;
            this.relativeX = relativeX;
            this.relativeY = relativeY;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private double scroll;
    private @Nullable AbstractWidget focusedWidget;

    public AdminInspectorScrollPanel(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public <T extends AbstractWidget> T add(T widget, int relativeY) {
        return add(widget, 0, relativeY);
    }

    public <T extends AbstractWidget> T add(T widget, int relativeX, int relativeY) {
        entries.add(new Entry(widget, relativeX, relativeY));
        return widget;
    }

    public void setScroll(double scroll) {
        this.scroll = Mth.clamp(scroll, 0.0D, maxScroll());
    }

    public double scroll() {
        return scroll;
    }

    public int contentHeight() {
        int height = 0;
        for (Entry entry : entries) {
            height = Math.max(height, entry.relativeY + entry.widget.getHeight());
        }
        return height;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - this.height);
    }

    private void layoutChildren() {
        for (Entry entry : entries) {
            entry.widget.setX(getX() + entry.relativeX);
            entry.widget.setY(getY() + entry.relativeY - (int) scroll);
        }
    }

    private boolean isChildVisible(AbstractWidget widget) {
        return widget.getY() + widget.getHeight() >= getY() && widget.getY() <= getY() + height;
    }

    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return Optional.empty();
        }
        layoutChildren();
        for (int index = entries.size() - 1; index >= 0; index--) {
            AbstractWidget widget = entries.get(index).widget;
            if (isChildVisible(widget) && widget.isMouseOver(mouseX, mouseY)) {
                return Optional.of(widget);
            }
        }
        return Optional.empty();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        layoutChildren();
        graphics.enableScissor(getX(), getY(), getX() + width, getY() + height);
        for (Entry entry : entries) {
            AbstractWidget widget = entry.widget;
            if (isChildVisible(widget)) {
                widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }
        graphics.disableScissor();

        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int trackX = getX() + width - 3;
            int trackHeight = Math.max(16, (int) ((this.height / (double) contentHeight()) * this.height));
            int trackY = getY() + (int) ((scroll / maxScroll) * (this.height - trackHeight));
            graphics.fill(trackX, getY(), trackX + 2, getY() + this.height, AdminUiTheme.HEADER);
            graphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, AdminUiTheme.ACCENT);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) {
            return false;
        }
        layoutChildren();
        for (int index = entries.size() - 1; index >= 0; index--) {
            AbstractWidget widget = entries.get(index).widget;
            if (isChildVisible(widget) && widget.mouseClicked(event, doubleClick)) {
                focusedWidget = widget;
                return true;
            }
        }
        focusedWidget = null;
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        layoutChildren();
        for (int index = entries.size() - 1; index >= 0; index--) {
            AbstractWidget widget = entries.get(index).widget;
            if (widget.mouseReleased(event)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        layoutChildren();
        for (int index = entries.size() - 1; index >= 0; index--) {
            AbstractWidget widget = entries.get(index).widget;
            if (widget.mouseDragged(event, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        setScroll(scroll - scrollY * 16.0D);
        return true;
    }

    public boolean charTyped(CharacterEvent event) {
        layoutChildren();
        if (focusedWidget != null && isChildVisible(focusedWidget) && focusedWidget.charTyped(event)) {
            return true;
        }
        for (int index = entries.size() - 1; index >= 0; index--) {
            AbstractWidget widget = entries.get(index).widget;
            if (widget.charTyped(event)) {
                focusedWidget = widget;
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        layoutChildren();
        if (focusedWidget != null && isChildVisible(focusedWidget) && focusedWidget.keyPressed(event)) {
            return true;
        }
        for (int index = entries.size() - 1; index >= 0; index--) {
            AbstractWidget widget = entries.get(index).widget;
            if (widget.keyPressed(event)) {
                focusedWidget = widget;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        for (Entry entry : entries) {
            entry.widget.updateNarration(output);
        }
    }
}
