package com.fool.admin.client.screen;

import com.fool.admin.client.AdminUiTheme;
import com.fool.admin.content.DialogueChoice;
import com.fool.admin.content.DialogueNode;
import com.fool.admin.content.DialogueScript;
import com.fool.admin.content.DialogueSpeaker;
import com.fool.admin.network.payload.CloseDialoguePayload;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DialogueOverlayScreen extends Screen {
    private final String npcDefinitionId;
    private final String npcDisplayName;
    private final Identifier entityTypeId;
    private final DialogueScript script;
    private final List<LogEntry> logEntries = new ArrayList<>();
    private @Nullable LivingEntity previewEntity;
    private @Nullable String currentNodeId;
    private int delayTicksRemaining;
    private boolean awaitingAdvance;
    private boolean finished;

    public DialogueOverlayScreen(
            String npcDefinitionId,
            String npcDisplayName,
            Identifier entityTypeId,
            DialogueScript script
    ) {
        super(Component.empty());
        this.npcDefinitionId = npcDefinitionId;
        this.npcDisplayName = npcDisplayName;
        this.entityTypeId = entityTypeId;
        this.script = script;
    }

    @Override
    protected void init() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.mouseHandler.releaseMouse();
        KeyMapping.releaseAll();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId).orElse(null);
        if (type != null && minecraft.level != null) {
            Entity entity = type.create(minecraft.level, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
            if (entity instanceof LivingEntity living) {
                // InventoryScreen's renderer resolves held-item state through the entity ID.
                // This detached client-only entity is never added to the level, so assign one here.
                living.setId(-1);
                living.setCustomName(Component.literal(npcDisplayName));
                living.setCustomNameVisible(false);
                previewEntity = living;
            }
        }
        enterNode(script.entryNodeId());
    }

    @Override
    public void tick() {
        KeyMapping.releaseAll();
        if (finished || currentNodeId == null) {
            return;
        }
        DialogueNode node = script.node(currentNodeId);
        if (node.isChoiceNode() || !awaitingAdvance) {
            return;
        }
        if (delayTicksRemaining > 0) {
            delayTicksRemaining--;
            if (delayTicksRemaining == 0) {
                awaitingAdvance = false;
                advanceFromCurrentNode();
            }
            return;
        }
    }

    private void enterNode(String nodeId) {
        currentNodeId = nodeId;
        DialogueNode node = script.node(nodeId);
        if (node.isChoiceNode()) {
            awaitingAdvance = false;
            delayTicksRemaining = 0;
            return;
        }
        if (node.speaker() == DialogueSpeaker.NPC) {
            logEntries.add(new LogEntry(npcDisplayName + ":", node.text(), AdminUiTheme.DIALOGUE_NPC_TEXT));
        } else if (!node.text().isBlank()) {
            logEntries.add(new LogEntry(minecraft.player.getPlainTextName() + ":", node.text(), AdminUiTheme.DIALOGUE_PLAYER_TEXT));
        }
        delayTicksRemaining = node.delayTicks();
        awaitingAdvance = true;
    }

    private void advanceFromCurrentNode() {
        if (currentNodeId == null || finished) {
            return;
        }
        DialogueNode node = script.node(currentNodeId);
        if (node.isChoiceNode()) {
            return;
        }
        if (delayTicksRemaining > 0) {
            delayTicksRemaining = 0;
            return;
        }
        if (node.nextNodeId() == null) {
            closeOverlay(true);
            return;
        }
        enterNode(node.nextNodeId());
    }

    private void choose(int index) {
        if (currentNodeId == null || finished) {
            return;
        }
        DialogueNode node = script.node(currentNodeId);
        if (!node.isChoiceNode() || index < 0 || index >= node.choices().size()) {
            return;
        }
        DialogueChoice choice = node.choices().get(index);
        logEntries.add(new LogEntry(">", "[" + choice.label() + "]", AdminUiTheme.DIALOGUE_CHOICE_TEXT));
        if (choice.targetNodeId() == null) {
            closeOverlay(true);
            return;
        }
        enterNode(choice.targetNodeId());
    }

    private void closeOverlay(boolean completed) {
        if (finished) {
            return;
        }
        finished = true;
        var connection = minecraft.getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(new CloseDialoguePayload(npcDefinitionId, completed)));
        }
        onClose();
    }

    @Override
    public void onClose() {
        if (previewEntity != null) {
            previewEntity.discard();
            previewEntity = null;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (currentNodeId == null) {
            return super.keyPressed(event);
        }
        DialogueNode node = script.node(currentNodeId);
        if (node.isChoiceNode()) {
            int index = event.getDigit() - 1;
            if (index >= 0 && index < node.choices().size()) {
                choose(index);
                return true;
            }
            if (event.isSelection() && !node.choices().isEmpty()) {
                choose(0);
                return true;
            }
        } else if (event.isSelection()) {
            advanceFromCurrentNode();
            return true;
        }
        if (event.isEscape()) {
            closeOverlay(false);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (currentNodeId == null) {
            return super.mouseClicked(event, doubleClick);
        }
        DialogueNode node = script.node(currentNodeId);
        if (node.isChoiceNode()) {
            int choiceY = height - 80;
            int index = 0;
            for (DialogueChoice ignored : node.choices()) {
                int rowTop = choiceY + index * 18 - 2;
                if (event.x() >= 20 && event.x() < width * 0.68F - 12
                        && event.y() >= rowTop && event.y() < rowTop + 16) {
                    choose(index);
                    return true;
                }
                index++;
            }
            return true;
        }
        advanceFromCurrentNode();
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, AdminUiTheme.DIALOGUE_DIM);
        int panelRight = (int) (width * 0.68F);
        graphics.fill(16, 24, panelRight - 8, height - 72, AdminUiTheme.DIALOGUE_PANEL);
        graphics.fill(16, height - 68, panelRight - 8, height - 64, AdminUiTheme.DIALOGUE_DIVIDER);

        int y = 36;
        int maxLines = Math.max(1, (height - 120) / 14);
        int start = Math.max(0, logEntries.size() - maxLines);
        for (int index = start; index < logEntries.size(); index++) {
            LogEntry entry = logEntries.get(index);
            graphics.text(font, Component.literal(entry.prefix() + " " + entry.text()), 24, y, entry.color(), false);
            y += 14;
        }

        if (currentNodeId != null) {
            DialogueNode node = script.node(currentNodeId);
            if (node.isChoiceNode()) {
                int choiceY = height - 80;
                int choiceIndex = 0;
                for (DialogueChoice choice : node.choices()) {
                    int rowY = choiceY + choiceIndex * 18;
                    graphics.fill(20, rowY - 2, panelRight - 12, rowY + 14, AdminUiTheme.FRAME);
                    Component label = Component.literal("> [" + choice.label() + "]");
                    graphics.text(font, label, 24, rowY, AdminUiTheme.DIALOGUE_CHOICE_TEXT, false);
                    choiceIndex++;
                }
            }
        }

        if (previewEntity != null) {
            int portraitLeft = panelRight + 16;
            int portraitRight = width - 16;
            int portraitTop = Math.max(32, height / 2 - 110);
            int portraitBottom = Math.min(height - 32, height / 2 + 110);
            graphics.fill(portraitLeft, portraitTop, portraitRight, portraitBottom, AdminUiTheme.DIALOGUE_PANEL);
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics,
                    portraitLeft,
                    portraitTop,
                    portraitRight,
                    portraitBottom,
                    72,
                    0.0F,
                    mouseX,
                    mouseY,
                    previewEntity
            );
            graphics.text(
                    font,
                    Component.literal(npcDisplayName),
                    panelRight + 24,
                    portraitBottom + 6,
                    AdminUiTheme.DIALOGUE_NPC_TEXT,
                    false
            );
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.requestCursor(CursorTypes.ARROW);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private record LogEntry(String prefix, String text, int color) {
    }
}
