package com.fool.adventure.admin.client.screen;

import com.fool.adventure.admin.client.AdminUiTheme;
import com.fool.adventure.admin.client.content.AdminMapTool;
import com.fool.adventure.admin.client.content.AdminTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AdminMapToolDock extends AbstractWidget {
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int LABEL_HEIGHT = 12;
    private static final int INNER_PADDING = 6;

    private static final class Entry {
        private final AdminActionButton widget;
        private final AdminMapTool tool;
        private final int relativeX;
        private final int relativeY;

        private Entry(AdminActionButton widget, AdminMapTool tool, int relativeX, int relativeY) {
            this.widget = widget;
            this.tool = tool;
            this.relativeX = relativeX;
            this.relativeY = relativeY;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    private AdminMapToolDock(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public static AdminMapToolDock create(
            int x,
            int y,
            AdminTab tab,
            AdminMapTool activeTool,
            Consumer<AdminMapTool> onToolSelected
    ) {
        int buttonWidth = 58;
        int dockWidth = buttonWidth * 2 + BUTTON_GAP + INNER_PADDING * 2;
        int dockHeight = INNER_PADDING + LABEL_HEIGHT + 4 + BUTTON_HEIGHT + BUTTON_GAP + BUTTON_HEIGHT + BUTTON_GAP + BUTTON_HEIGHT + INNER_PADDING;

        AdminMapToolDock dock = new AdminMapToolDock(x, y, dockWidth, dockHeight);
        int innerX = INNER_PADDING;
        int innerY = INNER_PADDING + LABEL_HEIGHT + 4;

        if (tab == AdminTab.BOSSES) {
            dock.addTool(innerX, innerY, buttonWidth, "foolsadventure.admin.tool.spawn", AdminMapTool.SET_SPAWN, activeTool, onToolSelected);
            dock.addTool(innerX + buttonWidth + BUTTON_GAP, innerY, buttonWidth, "foolsadventure.admin.tool.paint", AdminMapTool.PAINT_ZONE, activeTool, onToolSelected);
            innerY += BUTTON_HEIGHT + BUTTON_GAP;
            dock.addTool(innerX, innerY, buttonWidth, "foolsadventure.admin.tool.erase", AdminMapTool.ERASE_ZONE, activeTool, onToolSelected);
            dock.addTool(innerX + buttonWidth + BUTTON_GAP, innerY, buttonWidth, "foolsadventure.admin.tool.attract", AdminMapTool.SET_ATTRACTION, activeTool, onToolSelected);
            innerY += BUTTON_HEIGHT + BUTTON_GAP;
            dock.addTool(innerX, innerY, dockWidth - INNER_PADDING * 2, "foolsadventure.admin.tool.pan", AdminMapTool.PAN, activeTool, onToolSelected);
        } else {
            dock.addTool(innerX, innerY, buttonWidth, "foolsadventure.admin.tool.spawn", AdminMapTool.SET_SPAWN, activeTool, onToolSelected);
            dock.addTool(innerX + buttonWidth + BUTTON_GAP, innerY, buttonWidth, "foolsadventure.admin.tool.waypoint", AdminMapTool.ADD_WAYPOINT, activeTool, onToolSelected);
            innerY += BUTTON_HEIGHT + BUTTON_GAP;
            dock.addTool(innerX, innerY, dockWidth - INNER_PADDING * 2, "foolsadventure.admin.tool.pan", AdminMapTool.PAN, activeTool, onToolSelected);
        }

        return dock;
    }

    private void addTool(
            int relativeX,
            int relativeY,
            int width,
            String labelKey,
            AdminMapTool tool,
            AdminMapTool activeTool,
            Consumer<AdminMapTool> onToolSelected
    ) {
        boolean selected = activeTool == tool;
        entries.add(new Entry(
                new AdminActionButton(
                        0,
                        0,
                        width,
                        BUTTON_HEIGHT,
                        Component.translatable(labelKey),
                        false,
                        selected,
                        () -> onToolSelected.accept(tool)
                ),
                tool,
                relativeX,
                relativeY
        ));
    }

    public void setActiveTool(AdminMapTool activeTool) {
        for (Entry entry : entries) {
            entry.widget.setSelected(entry.tool == activeTool);
        }
    }

    private void layoutChildren() {
        for (Entry entry : entries) {
            entry.widget.setX(getX() + entry.relativeX);
            entry.widget.setY(getY() + entry.relativeY);
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height, AdminUiTheme.HEADER);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, AdminUiTheme.MAP_BORDER);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, AdminUiTheme.MAP_BORDER);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, AdminUiTheme.MAP_BORDER);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, AdminUiTheme.MAP_BORDER);

        graphics.text(
                Minecraft.getInstance().font,
                Component.translatable("foolsadventure.admin.inspector.tools"),
                getX() + INNER_PADDING,
                getY() + INNER_PADDING + 2,
                AdminUiTheme.TEXT_MUTED,
                false
        );

        layoutChildren();
        for (Entry entry : entries) {
            entry.widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
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
            if (widget.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, Component.translatable("foolsadventure.admin.inspector.tools"));
    }
}
