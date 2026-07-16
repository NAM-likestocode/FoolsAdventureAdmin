package com.fool.admin.client.screen;

import com.fool.admin.client.AdminUiTheme;
import com.fool.admin.client.content.AdminQuestTool;
import com.fool.admin.client.content.ClientAdminContentController;
import com.fool.admin.content.AdminContentConstants;
import com.fool.admin.content.QuestObjectiveType;
import com.fool.admin.content.QuestPoint;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class QuestGraphWidget extends AbstractWidget {
    private final ClientAdminContentController contentController;
    private final Consumer<GraphViewState> stateListener;
    private double centerX;
    private double centerY;
    private double scale = 1.0D;
    private boolean draggingCanvas;
    private double lastDragMouseX;
    private double lastDragMouseY;
    private @Nullable String draggingQuestId;
    private float dragOffsetX;
    private float dragOffsetY;

    public QuestGraphWidget(
            int x,
            int y,
            int width,
            int height,
            ClientAdminContentController contentController,
            GraphViewState initialState,
            Consumer<GraphViewState> stateListener
    ) {
        super(x, y, width, height, Component.translatable("foolsadmin.admin.quest_graph.narration"));
        this.contentController = contentController;
        this.centerX = initialState.centerX();
        this.centerY = initialState.centerY();
        this.scale = initialState.scale();
        this.stateListener = stateListener;
    }

    public void applyViewState(GraphViewState state) {
        this.centerX = state.centerX();
        this.centerY = state.centerY();
        this.scale = state.scale();
    }

    public void zoomBy(double factor, double anchorScreenX, double anchorScreenY) {
        double worldX = screenToWorldX(anchorScreenX);
        double worldY = screenToWorldY(anchorScreenY);
        scale = Math.clamp(scale * factor, 0.35D, 2.5D);
        centerX = worldX - (anchorScreenX - getX() - width / 2.0D) / scale;
        centerY = worldY - (anchorScreenY - getY() - height / 2.0D) / scale;
        notifyState();
    }

    public float centerCanvasX() {
        return (float) centerX;
    }

    public float centerCanvasY() {
        return (float) centerY;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height, AdminUiTheme.MAP_BACKGROUND);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, AdminUiTheme.MAP_BORDER);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, AdminUiTheme.MAP_BORDER);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, AdminUiTheme.MAP_BORDER);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, AdminUiTheme.MAP_BORDER);

        renderLinks(graphics);
        renderNodes(graphics, mouseX, mouseY);
        graphics.requestCursor(draggingCanvas || draggingQuestId != null ? CursorTypes.RESIZE_ALL : CursorTypes.ARROW);
    }

    private void renderLinks(GuiGraphicsExtractor graphics) {
        List<QuestPoint> quests = contentController.quests();
        for (QuestPoint quest : quests) {
            for (String prerequisiteId : quest.prerequisiteIds()) {
                QuestPoint prerequisite = contentController.questForEditing(prerequisiteId);
                if (prerequisite == null) {
                    continue;
                }
                int fromX = worldToScreenX(prerequisite.canvasX() + AdminContentConstants.QUEST_NODE_SIZE / 2.0F);
                int fromY = worldToScreenY(prerequisite.canvasY() + AdminContentConstants.QUEST_NODE_SIZE / 2.0F);
                int toX = worldToScreenX(quest.canvasX() + AdminContentConstants.QUEST_NODE_SIZE / 2.0F);
                int toY = worldToScreenY(quest.canvasY() + AdminContentConstants.QUEST_NODE_SIZE / 2.0F);
                drawLine(graphics, fromX, fromY, toX, toY, 0xFFFFFFFF);
            }
        }
    }

    private void renderNodes(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (QuestPoint quest : contentController.quests()) {
            boolean selected = quest.id().equals(contentController.selectedQuestId());
            boolean linkSource = quest.id().equals(contentController.linkSourceQuestId());
            int nodeX = worldToScreenX(quest.canvasX());
            int nodeY = worldToScreenY(quest.canvasY());
            int size = (int) Math.round(AdminContentConstants.QUEST_NODE_SIZE * scale);
            size = Math.max(18, size);
            int fill = selected ? AdminUiTheme.DRAFT_HIGHLIGHT : objectiveColor(quest.objectiveType());
            if (linkSource) {
                fill = AdminUiTheme.PLAYER_MARKER;
            }
            graphics.fill(nodeX, nodeY, nodeX + size, nodeY + size, 0xFF000000);
            graphics.fill(nodeX + 1, nodeY + 1, nodeX + size - 1, nodeY + size - 1, fill);
            var font = Minecraft.getInstance().font;
            String label = truncate(quest.name(), Math.max(4, size / 6));
            graphics.text(
                    font,
                    Component.literal(label),
                    nodeX + 3,
                    nodeY + size / 2 - 4,
                    0xFF1A1714,
                    false
            );
        }
    }

    private static int objectiveColor(QuestObjectiveType type) {
        return switch (type) {
            case TALK_TO_NPC -> 0xFFC9A227;
            case ITEM_TO_NPC -> 0xFF81C784;
            case KILL_BOSS -> 0xFFE63946;
            case CLEAR_DUNGEON -> 0xFF6A5E52;
        };
    }

    private static String truncate(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    private void drawLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int x = x1;
        int y = y1;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, color);
            if (x == x2 && y == y2) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        double factor = scrollY > 0.0D ? 0.9D : 1.1D;
        zoomBy(factor, mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!active || !visible || !isMouseOver(event.x(), event.y())) {
            return false;
        }
        @Nullable String questId = questAt(event.x(), event.y());
        if (questId != null) {
            if (contentController.activeQuestTool() == AdminQuestTool.LINK) {
                if (contentController.linkSourceQuestId() == null) {
                    contentController.setLinkSourceQuest(questId);
                    return true;
                }
                contentController.handleQuestLinkClick(questId);
                return true;
            }
            if (contentController.activeQuestTool() == AdminQuestTool.PAN
                    && contentController.beginQuestCanvasMove(questId)) {
                draggingQuestId = questId;
                QuestPoint quest = contentController.questForEditing(questId);
                if (quest != null) {
                    dragOffsetX = (float) (screenToWorldX(event.x()) - quest.canvasX());
                    dragOffsetY = (float) (screenToWorldY(event.y()) - quest.canvasY());
                }
                return true;
            }
            contentController.selectQuest(questId);
            return true;
        }

        if (contentController.activeQuestTool() == AdminQuestTool.ADD) {
            contentController.createQuestDraft(
                    (float) screenToWorldX(event.x()),
                    (float) screenToWorldY(event.y())
            );
            return true;
        }

        if (contentController.activeQuestTool() == AdminQuestTool.PAN) {
            draggingCanvas = true;
            lastDragMouseX = event.x();
            lastDragMouseY = event.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean movedQuest = draggingQuestId != null;
        draggingCanvas = false;
        draggingQuestId = null;
        if (movedQuest) {
            contentController.finishQuestCanvasMove();
        }
        return isMouseOver(event.x(), event.y());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingQuestId != null) {
            QuestPoint quest = contentController.questForEditing(draggingQuestId);
            if (quest != null) {
                float canvasX = (float) (screenToWorldX(event.x()) - dragOffsetX);
                float canvasY = (float) (screenToWorldY(event.y()) - dragOffsetY);
                contentController.setQuestCanvasPosition(canvasX, canvasY);
            }
            return true;
        }
        if (draggingCanvas) {
            centerX -= (event.x() - lastDragMouseX) / scale;
            centerY -= (event.y() - lastDragMouseY) / scale;
            lastDragMouseX = event.x();
            lastDragMouseY = event.y();
            notifyState();
            return true;
        }
        return false;
    }

    private @Nullable String questAt(double mouseX, double mouseY) {
        float worldX = (float) screenToWorldX(mouseX);
        float worldY = (float) screenToWorldY(mouseY);
        for (QuestPoint quest : contentController.quests()) {
            if (worldX >= quest.canvasX() && worldX <= quest.canvasX() + AdminContentConstants.QUEST_NODE_SIZE
                    && worldY >= quest.canvasY() && worldY <= quest.canvasY() + AdminContentConstants.QUEST_NODE_SIZE) {
                return quest.id();
            }
        }
        return null;
    }

    private double screenToWorldX(double screenX) {
        return centerX + (screenX - getX() - width / 2.0D) / scale;
    }

    private double screenToWorldY(double screenY) {
        return centerY + (screenY - getY() - height / 2.0D) / scale;
    }

    private int worldToScreenX(float worldX) {
        return (int) Math.round(getX() + width / 2.0D + (worldX - centerX) * scale);
    }

    private int worldToScreenY(float worldY) {
        return (int) Math.round(getY() + height / 2.0D + (worldY - centerY) * scale);
    }

    private void notifyState() {
        stateListener.accept(new GraphViewState(centerX, centerY, scale));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    public record GraphViewState(double centerX, double centerY, double scale) {
    }
}
