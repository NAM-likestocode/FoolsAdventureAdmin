package com.fool.admin.client.screen;

import com.fool.admin.client.AdminUiTheme;
import com.fool.admin.client.content.AdminMapTool;
import com.fool.admin.client.content.AdminTab;
import com.fool.admin.client.content.ClientAdminContentController;
import com.fool.admin.client.map.AdminMapTextureCache;
import com.fool.admin.client.map.ClientAdminMapController;
import com.fool.admin.content.BossDefinition;
import com.fool.admin.content.NpcDefinition;
import com.fool.admin.content.Waypoint;
import com.fool.admin.content.ZoneMask;
import com.fool.admin.map.MapTileConstants;
import com.fool.admin.map.MapTileMath;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class AdminMapWidget extends AbstractWidget {
    private final ClientAdminMapController mapController;
    private final ClientAdminContentController contentController;
    private final Consumer<MapViewState> stateListener;

    private double centerX;
    private double centerZ;
    private double blocksPerPixel = 1.0D;
    private float playerYaw;
    private int playerBlockX;
    private int playerBlockZ;
    private boolean dragging;
    private boolean painting;
    private double lastDragMouseX;
    private double lastDragMouseY;
    private int cursorBlockX = Integer.MIN_VALUE;
    private int cursorBlockZ = Integer.MIN_VALUE;
    private int lastPaintBlockX = Integer.MIN_VALUE;
    private int lastPaintBlockZ = Integer.MIN_VALUE;

    public AdminMapWidget(
            int x,
            int y,
            int width,
            int height,
            ClientAdminMapController mapController,
            ClientAdminContentController contentController,
            double centerX,
            double centerZ,
            Consumer<MapViewState> stateListener
    ) {
        super(x, y, width, height, Component.empty());
        this.mapController = mapController;
        this.contentController = contentController;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.stateListener = stateListener;
    }

    public void setPlayerPosition(int blockX, int blockZ, float yaw) {
        this.playerBlockX = blockX;
        this.playerBlockZ = blockZ;
        this.playerYaw = yaw;
    }

    public void recenterOnPlayer() {
        this.centerX = playerBlockX + 0.5D;
        this.centerZ = playerBlockZ + 0.5D;
        notifyState();
    }

    public void applyViewState(MapViewState state) {
        this.centerX = state.centerX();
        this.centerZ = state.centerZ();
        this.blocksPerPixel = state.blocksPerPixel();
    }

    public void zoomBy(double factor, double anchorX, double anchorY) {
        MapTileMath.ZoomAnchorResult result = MapTileMath.zoomAroundPoint(
                centerX,
                centerZ,
                blocksPerPixel,
                factor,
                anchorX - getX(),
                anchorY - getY(),
                width,
                height
        );
        this.centerX = result.centerX();
        this.centerZ = result.centerZ();
        this.blocksPerPixel = result.blocksPerPixel();
        notifyState();
    }

    public double blocksPerPixel() {
        return blocksPerPixel;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.enableScissor(getX(), getY(), getX() + width, getY() + height);
        graphics.fill(getX(), getY(), getX() + width, getY() + height, AdminUiTheme.MAP_BACKGROUND);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, AdminUiTheme.MAP_BORDER);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, AdminUiTheme.MAP_BORDER);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, AdminUiTheme.MAP_BORDER);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, AdminUiTheme.MAP_BORDER);

        renderTiles(graphics);
        renderContentOverlays(graphics);
        renderBrushPreview(graphics, mouseX, mouseY);
        renderPlayerMarker(graphics);
        graphics.disableScissor();

        if (isMouseOver(mouseX, mouseY)) {
            graphics.requestCursor(dragging || painting ? CursorTypes.RESIZE_ALL : CursorTypes.ARROW);
        }

        updateCursor(mouseX, mouseY);
        mapController.requestVisibleTiles(centerX, centerZ, blocksPerPixel, width, height);
        notifyState();
    }

    private void renderTiles(GuiGraphicsExtractor graphics) {
        ResourceKey<Level> dimension = mapController.dimension();
        double halfWidthBlocks = width * blocksPerPixel / 2.0D;
        double halfHeightBlocks = height * blocksPerPixel / 2.0D;
        int minTileX = MapTileMath.blockToTile((int) Math.floor(centerX - halfWidthBlocks) - MapTileConstants.TILE_BLOCKS);
        int maxTileX = MapTileMath.blockToTile((int) Math.ceil(centerX + halfWidthBlocks) + MapTileConstants.TILE_BLOCKS);
        int minTileZ = MapTileMath.blockToTile((int) Math.floor(centerZ - halfHeightBlocks) - MapTileConstants.TILE_BLOCKS);
        int maxTileZ = MapTileMath.blockToTile((int) Math.ceil(centerZ + halfHeightBlocks) + MapTileConstants.TILE_BLOCKS);
        double tileScreenSize = MapTileConstants.TILE_BLOCKS / blocksPerPixel;

        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for (int tileZ = minTileZ; tileZ <= maxTileZ; tileZ++) {
                int tileOriginX = MapTileMath.tileOrigin(tileX);
                int tileOriginZ = MapTileMath.tileOrigin(tileZ);
                double screenLeft = getX() + (tileOriginX - centerX) / blocksPerPixel + width / 2.0D;
                double screenTop = getY() + (tileOriginZ - centerZ) / blocksPerPixel + height / 2.0D;
                int x0 = Mth.floor(screenLeft);
                int y0 = Mth.floor(screenTop);
                int x1 = Mth.ceil(screenLeft + tileScreenSize);
                int y1 = Mth.ceil(screenTop + tileScreenSize);
                if (x1 <= x0) {
                    x1 = x0 + 1;
                }
                if (y1 <= y0) {
                    y1 = y0 + 1;
                }

                if (x1 < getX() || y1 < getY() || x0 > getX() + width || y0 > getY() + height) {
                    continue;
                }

                AdminMapTextureCache.TileTextureEntry entry = mapController.textureCache().get(dimension, tileX, tileZ);
                if (entry == null) {
                    graphics.fill(x0, y0, x1, y1, AdminUiTheme.MAP_UNEXPLORED);
                } else {
                    graphics.blit(entry.location(), x0, y0, x1, y1, 0.0F, 1.0F, 0.0F, 1.0F);
                }
            }
        }
    }

    private void renderContentOverlays(GuiGraphicsExtractor graphics) {
        for (BossDefinition boss : contentController.bosses()) {
            if (contentController.bossDraft() != null && boss.id().equals(contentController.bossDraft().id())) {
                continue;
            }
            renderBossOverlay(graphics, boss, false);
        }
        BossDefinition bossDraft = contentController.bossDraft();
        if (bossDraft != null) {
            renderBossOverlay(graphics, bossDraft, true);
        }

        for (NpcDefinition npc : contentController.npcs()) {
            if (contentController.npcDraft() != null && npc.id().equals(contentController.npcDraft().id())) {
                continue;
            }
            renderNpcOverlay(graphics, npc, false);
        }
        NpcDefinition npcDraft = contentController.npcDraft();
        if (npcDraft != null) {
            renderNpcOverlay(graphics, npcDraft, true);
        }
    }

    private void renderBossOverlay(GuiGraphicsExtractor graphics, BossDefinition boss, boolean draft) {
        int zoneColor = draft ? AdminUiTheme.DRAFT_BOSS_ZONE : AdminUiTheme.INACTIVE_BOSS_ZONE;
        int spawnColor = draft ? AdminUiTheme.BOSS_SPAWN : AdminUiTheme.INACTIVE_BOSS_SPAWN;
        int attractionColor = draft ? AdminUiTheme.BOSS_ATTRACTION : AdminUiTheme.INACTIVE_BOSS_ATTRACTION;
        renderZoneMask(graphics, boss.zone(), zoneColor);
        renderPointMarker(graphics, boss.spawnX(), boss.spawnZ(), spawnColor, AdminUiTheme.SPAWN_MARKER_RADIUS);
        if (boss.hasAttractionPoint()) {
            renderPointMarker(graphics, boss.attractionX(), boss.attractionZ(), attractionColor, AdminUiTheme.ATTRACTION_MARKER_RADIUS);
        }
    }

    private void renderNpcOverlay(GuiGraphicsExtractor graphics, NpcDefinition npc, boolean draft) {
        int pathColor = draft ? AdminUiTheme.DRAFT_NPC_PATH : AdminUiTheme.INACTIVE_NPC_PATH;
        int spawnColor = draft ? AdminUiTheme.NPC_SPAWN : AdminUiTheme.INACTIVE_NPC_SPAWN;
        int waypointColor = draft ? AdminUiTheme.NPC_WAYPOINT : AdminUiTheme.INACTIVE_NPC_WAYPOINT;
        renderPointMarker(graphics, npc.spawnX(), npc.spawnZ(), spawnColor, AdminUiTheme.SPAWN_MARKER_RADIUS);
        List<Waypoint> waypoints = npc.waypoints();
        Waypoint previous = new Waypoint(npc.spawnX(), npc.spawnY(), npc.spawnZ());
        for (Waypoint waypoint : waypoints) {
            renderLine(graphics, previous.x(), previous.z(), waypoint.x(), waypoint.z(), pathColor);
            renderPointMarker(graphics, waypoint.x(), waypoint.z(), waypointColor, AdminUiTheme.WAYPOINT_MARKER_RADIUS);
            previous = waypoint;
        }
        if (npc.repeatPath() && !waypoints.isEmpty()) {
            Waypoint first = waypoints.getFirst();
            Waypoint last = waypoints.getLast();
            renderLine(graphics, last.x(), last.z(), first.x(), first.z(), pathColor);
        }
    }

    private void renderBrushPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY) || cursorBlockX == Integer.MIN_VALUE) {
            return;
        }
        AdminTab tab = contentController.activeTab();
        if (tab == AdminTab.MAP) {
            return;
        }
        if (contentController.activeTool() == AdminMapTool.PAN) {
            return;
        }
        if (tab == AdminTab.BOSSES && contentController.bossDraft() == null) {
            return;
        }
        if (tab == AdminTab.NPCS && contentController.npcDraft() == null) {
            return;
        }

        switch (contentController.activeTool()) {
            case PAINT_ZONE -> renderBrushDisc(graphics, cursorBlockX, cursorBlockZ, contentController.brushRadius(), AdminUiTheme.BRUSH_PAINT);
            case ERASE_ZONE -> renderBrushDisc(graphics, cursorBlockX, cursorBlockZ, contentController.brushRadius(), AdminUiTheme.BRUSH_ERASE);
            case SET_SPAWN -> renderPointMarker(
                    graphics,
                    cursorBlockX,
                    cursorBlockZ,
                    tab == AdminTab.BOSSES ? AdminUiTheme.BOSS_SPAWN : AdminUiTheme.NPC_SPAWN,
                    AdminUiTheme.SPAWN_MARKER_RADIUS
            );
            case SET_ATTRACTION -> renderPointMarker(graphics, cursorBlockX, cursorBlockZ, AdminUiTheme.BRUSH_ATTRACTION, AdminUiTheme.ATTRACTION_MARKER_RADIUS);
            case ADD_WAYPOINT -> renderPointMarker(graphics, cursorBlockX, cursorBlockZ, AdminUiTheme.BRUSH_WAYPOINT, AdminUiTheme.WAYPOINT_MARKER_RADIUS);
            default -> {
            }
        }
    }

    private void renderBrushDisc(GuiGraphicsExtractor graphics, int centerBlockX, int centerBlockZ, int radius, int color) {
        int radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                renderBlockCell(graphics, centerBlockX + dx, centerBlockZ + dz, color);
            }
        }
    }

    private void renderZoneMask(GuiGraphicsExtractor graphics, ZoneMask zone, int color) {
        double halfWidthBlocks = width * blocksPerPixel / 2.0D;
        double halfHeightBlocks = height * blocksPerPixel / 2.0D;
        int minBlockX = (int) Math.floor(centerX - halfWidthBlocks) - 16;
        int maxBlockX = (int) Math.ceil(centerX + halfWidthBlocks) + 16;
        int minBlockZ = (int) Math.floor(centerZ - halfHeightBlocks) - 16;
        int maxBlockZ = (int) Math.ceil(centerZ + halfHeightBlocks) + 16;

        int minChunkX = minBlockX >> 4;
        int maxChunkX = maxBlockX >> 4;
        int minChunkZ = minBlockZ >> 4;
        int maxChunkZ = maxBlockZ >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                int baseX = chunkX << 4;
                int baseZ = chunkZ << 4;
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int blockX = baseX + localX;
                        int blockZ = baseZ + localZ;
                        if (!zone.contains(blockX, blockZ)) {
                            continue;
                        }
                        renderBlockCell(graphics, blockX, blockZ, color);
                    }
                }
            }
        }
    }

    private void renderBlockCell(GuiGraphicsExtractor graphics, int blockX, int blockZ, int color) {
        double screenLeft = getX() + (blockX - centerX) / blocksPerPixel + width / 2.0D;
        double screenTop = getY() + (blockZ - centerZ) / blocksPerPixel + height / 2.0D;
        double screenRight = getX() + (blockX + 1.0D - centerX) / blocksPerPixel + width / 2.0D;
        double screenBottom = getY() + (blockZ + 1.0D - centerZ) / blocksPerPixel + height / 2.0D;
        int x0 = Mth.floor(screenLeft);
        int y0 = Mth.floor(screenTop);
        int x1 = Math.max(x0 + 1, Mth.ceil(screenRight));
        int y1 = Math.max(y0 + 1, Mth.ceil(screenBottom));
        graphics.fill(x0, y0, x1, y1, color);
    }

    private void renderPointMarker(GuiGraphicsExtractor graphics, int blockX, int blockZ, int color, int radius) {
        double screenX = getX() + (blockX + 0.5D - centerX) / blocksPerPixel + width / 2.0D;
        double screenZ = getY() + (blockZ + 0.5D - centerZ) / blocksPerPixel + height / 2.0D;
        int markerX = Mth.floor(screenX);
        int markerZ = Mth.floor(screenZ);
        graphics.fill(markerX - radius, markerZ - radius, markerX + radius + 1, markerZ + radius + 1, color);
    }

    private void renderLine(GuiGraphicsExtractor graphics, int fromX, int fromZ, int toX, int toZ, int color) {
        double screenX1 = getX() + (fromX + 0.5D - centerX) / blocksPerPixel + width / 2.0D;
        double screenZ1 = getY() + (fromZ + 0.5D - centerZ) / blocksPerPixel + height / 2.0D;
        double screenX2 = getX() + (toX + 0.5D - centerX) / blocksPerPixel + width / 2.0D;
        double screenZ2 = getY() + (toZ + 0.5D - centerZ) / blocksPerPixel + height / 2.0D;
        int steps = (int) Math.max(Math.abs(screenX2 - screenX1), Math.abs(screenZ2 - screenZ1));
        steps = Math.max(steps, 1);
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            int x = Mth.floor(Mth.lerp(t, screenX1, screenX2));
            int z = Mth.floor(Mth.lerp(t, screenZ1, screenZ2));
            graphics.fill(x, z, x + 1, z + 1, color);
        }
    }

    private void renderPlayerMarker(GuiGraphicsExtractor graphics) {
        double screenX = getX() + (playerBlockX + 0.5D - centerX) / blocksPerPixel + width / 2.0D;
        double screenZ = getY() + (playerBlockZ + 0.5D - centerZ) / blocksPerPixel + height / 2.0D;
        if (screenX < getX() || screenZ < getY() || screenX > getX() + width || screenZ > getY() + height) {
            return;
        }

        int markerX = Mth.floor(screenX);
        int markerZ = Mth.floor(screenZ);
        float yawRadians = (float) Math.toRadians(playerYaw);
        float sin = Mth.sin(yawRadians);
        float cos = Mth.cos(yawRadians);

        graphics.fill(markerX - 3, markerZ - 3, markerX + 4, markerZ - 2, AdminUiTheme.PLAYER_OUTLINE);
        graphics.fill(markerX - 3, markerZ + 3, markerX + 4, markerZ + 4, AdminUiTheme.PLAYER_OUTLINE);
        graphics.fill(markerX - 3, markerZ - 2, markerX - 2, markerZ + 3, AdminUiTheme.PLAYER_OUTLINE);
        graphics.fill(markerX + 3, markerZ - 2, markerX + 4, markerZ + 3, AdminUiTheme.PLAYER_OUTLINE);
        graphics.fill(markerX - 1, markerZ - 1, markerX + 2, markerZ + 2, AdminUiTheme.PLAYER_MARKER);

        int tipX = markerX - Mth.floor(sin * 8.0F);
        int tipZ = markerZ + Mth.floor(cos * 8.0F);
        renderScreenLine(graphics, markerX, markerZ, tipX, tipZ, AdminUiTheme.PLAYER_FACING, 2);
        graphics.fill(tipX - 1, tipZ - 1, tipX + 2, tipZ + 2, AdminUiTheme.PLAYER_FACING);
    }

    private void renderScreenLine(GuiGraphicsExtractor graphics, int x1, int z1, int x2, int z2, int color, int thickness) {
        int steps = (int) Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        steps = Math.max(steps, 1);
        int half = thickness / 2;
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            int x = Mth.floor(Mth.lerp(t, x1, x2));
            int z = Mth.floor(Mth.lerp(t, z1, z2));
            graphics.fill(x - half, z - half, x + half + 1, z + half + 1, color);
        }
    }

    private void updateCursor(int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY)) {
            cursorBlockX = Integer.MIN_VALUE;
            cursorBlockZ = Integer.MIN_VALUE;
            return;
        }
        cursorBlockX = Mth.floor(worldXAtScreen(mouseX));
        cursorBlockZ = Mth.floor(worldZAtScreen(mouseY));
    }

    private double worldXAtScreen(double mouseX) {
        return centerX + (mouseX - getX() - width / 2.0D) * blocksPerPixel;
    }

    private double worldZAtScreen(double mouseY) {
        return centerZ + (mouseY - getY() - height / 2.0D) * blocksPerPixel;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!active || !visible || !isMouseOver(event.x(), event.y())) {
            return false;
        }
        if (event.button() != 0) {
            return false;
        }

        int blockX = Mth.floor(worldXAtScreen(event.x()));
        int blockZ = Mth.floor(worldZAtScreen(event.y()));
        if (handleToolClick(blockX, blockZ, true)) {
            if (isContinuousPaintTool()) {
                painting = true;
            }
            return true;
        }

        clearPaintStroke();
        dragging = true;
        lastDragMouseX = event.x();
        lastDragMouseY = event.y();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            dragging = false;
            painting = false;
            clearPaintStroke();
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!active || !visible || event.button() != 0) {
            return false;
        }

        if (painting && isContinuousPaintTool()) {
            if (isMouseOver(event.x(), event.y())) {
                int blockX = Mth.floor(worldXAtScreen(event.x()));
                int blockZ = Mth.floor(worldZAtScreen(event.y()));
                boolean erase = contentController.activeTool() == AdminMapTool.ERASE_ZONE;
                applyPaintStroke(blockX, blockZ, erase);
            }
            return true;
        }

        if (!isMouseOver(event.x(), event.y())) {
            return false;
        }

        int blockX = Mth.floor(worldXAtScreen(event.x()));
        int blockZ = Mth.floor(worldZAtScreen(event.y()));
        if (handleToolClick(blockX, blockZ, false)) {
            return true;
        }

        if (!dragging) {
            return false;
        }
        double deltaX = event.x() - lastDragMouseX;
        double deltaZ = event.y() - lastDragMouseY;
        centerX -= deltaX * blocksPerPixel;
        centerZ -= deltaZ * blocksPerPixel;
        lastDragMouseX = event.x();
        lastDragMouseY = event.y();
        notifyState();
        return true;
    }

    private boolean handleToolClick(int blockX, int blockZ, boolean initialClick) {
        AdminTab tab = contentController.activeTab();
        AdminMapTool tool = contentController.activeTool();
        if (tab == AdminTab.MAP || tool == AdminMapTool.PAN) {
            return false;
        }
        if (tab == AdminTab.BOSSES) {
            return handleBossTool(tool, blockX, blockZ, initialClick);
        }
        if (tab == AdminTab.NPCS) {
            return handleNpcTool(tool, blockX, blockZ, initialClick);
        }
        return false;
    }

    private boolean handleBossTool(AdminMapTool tool, int blockX, int blockZ, boolean initialClick) {
        if (contentController.bossDraft() == null) {
            return false;
        }
        return switch (tool) {
            case SET_SPAWN -> {
                if (initialClick) {
                    contentController.setBossSpawn(blockX, blockZ);
                }
                yield true;
            }
            case PAINT_ZONE -> {
                applyPaintStroke(blockX, blockZ, false);
                yield true;
            }
            case ERASE_ZONE -> {
                applyPaintStroke(blockX, blockZ, true);
                yield true;
            }
            case SET_ATTRACTION -> {
                if (initialClick) {
                    contentController.setBossAttraction(blockX, blockZ);
                }
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleNpcTool(AdminMapTool tool, int blockX, int blockZ, boolean initialClick) {
        if (contentController.npcDraft() == null) {
            return false;
        }
        return switch (tool) {
            case SET_SPAWN -> {
                if (initialClick) {
                    contentController.setNpcSpawn(blockX, blockZ);
                }
                yield true;
            }
            case ADD_WAYPOINT -> {
                if (initialClick) {
                    contentController.addNpcWaypoint(blockX, blockZ);
                }
                yield true;
            }
            default -> false;
        };
    }

    private boolean isContinuousPaintTool() {
        AdminMapTool tool = contentController.activeTool();
        return tool == AdminMapTool.PAINT_ZONE || tool == AdminMapTool.ERASE_ZONE;
    }

    private void applyPaintStroke(int blockX, int blockZ, boolean erase) {
        if (lastPaintBlockX == Integer.MIN_VALUE) {
            contentController.paintBossZone(blockX, blockZ, erase);
        } else {
            int steps = Math.max(Math.abs(blockX - lastPaintBlockX), Math.abs(blockZ - lastPaintBlockZ));
            steps = Math.max(steps, 1);
            for (int step = 0; step <= steps; step++) {
                double t = step / (double) steps;
                int x = Mth.floor(Mth.lerp(t, lastPaintBlockX, blockX));
                int z = Mth.floor(Mth.lerp(t, lastPaintBlockZ, blockZ));
                contentController.paintBossZone(x, z, erase);
            }
        }
        lastPaintBlockX = blockX;
        lastPaintBlockZ = blockZ;
    }

    private void clearPaintStroke() {
        lastPaintBlockX = Integer.MIN_VALUE;
        lastPaintBlockZ = Integer.MIN_VALUE;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        double factor = scrollY > 0 ? 0.85D : 1.15D;
        zoomBy(factor, mouseX, mouseY);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, Component.translatable("foolsadmin.admin.map.narration"));
    }

    private void notifyState() {
        stateListener.accept(new MapViewState(centerX, centerZ, blocksPerPixel, cursorBlockX, cursorBlockZ, playerBlockX, playerBlockZ));
    }

    public record MapViewState(
            double centerX,
            double centerZ,
            double blocksPerPixel,
            int cursorBlockX,
            int cursorBlockZ,
            int playerBlockX,
            int playerBlockZ
    ) {
    }
}
