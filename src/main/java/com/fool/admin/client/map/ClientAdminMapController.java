package com.fool.admin.client.map;

import com.fool.admin.map.MapTileConstants;
import com.fool.admin.map.MapTileMath;
import com.fool.admin.network.payload.MapTilePayload;
import com.fool.admin.network.payload.MapTilesResponsePayload;
import com.fool.admin.network.payload.RequestMapTilesPayload;
import com.fool.admin.network.payload.TileCoord;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientAdminMapController {
    private final AdminMapTextureCache textureCache;
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private int nextRequestId = 1;
    private final Set<Integer> pendingRequests = new HashSet<>();
    private final Set<TileCoord> inFlightTiles = new HashSet<>();
    private long lastRequestTime;

    public ClientAdminMapController(AdminMapTextureCache textureCache) {
        this.textureCache = textureCache;
    }

    public void beginSession(ResourceKey<Level> dimension, int sessionId) {
        if (!this.dimension.equals(dimension)) {
            textureCache.clearDimension(this.dimension);
        }
        this.dimension = dimension;
        this.pendingRequests.clear();
        this.inFlightTiles.clear();
        this.nextRequestId = 1;
    }

    public void requestVisibleTiles(double centerX, double centerZ, double blocksPerPixel, int viewportWidth, int viewportHeight) {
        long now = System.currentTimeMillis();
        if (now - lastRequestTime < 200L) {
            return;
        }

        Set<TileCoord> visible = visibleTiles(centerX, centerZ, blocksPerPixel, viewportWidth, viewportHeight);
        List<TileCoord> missing = textureCache.findMissingTiles(dimension, visible, MapTileConstants.MAX_TILES_PER_REQUEST);
        if (missing.isEmpty()) {
            return;
        }

        missing.removeIf(inFlightTiles::contains);
        if (missing.isEmpty()) {
            return;
        }

        int requestId = nextRequestId++;
        pendingRequests.add(requestId);
        inFlightTiles.addAll(missing);
        lastRequestTime = now;
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(RequestMapTilesPayload.create(requestId, dimension, missing)));
        }
    }

    public void handleTilesResponse(MapTilesResponsePayload payload) {
        if (!dimension.equals(payload.dimension())) {
            return;
        }
        if (!pendingRequests.remove(payload.requestId())) {
            return;
        }
        for (MapTilePayload tile : payload.tiles()) {
            TileCoord coord = new TileCoord(tile.tileX(), tile.tileZ());
            inFlightTiles.remove(coord);
            textureCache.updateTile(payload.dimension(), tile.tileX(), tile.tileZ(), tile.colors(), tile.complete());
        }
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public AdminMapTextureCache textureCache() {
        return textureCache;
    }

    private Set<TileCoord> visibleTiles(double centerX, double centerZ, double blocksPerPixel, int viewportWidth, int viewportHeight) {
        double halfWidthBlocks = viewportWidth * blocksPerPixel / 2.0D;
        double halfHeightBlocks = viewportHeight * blocksPerPixel / 2.0D;
        int minTileX = MapTileMath.blockToTile((int) Math.floor(centerX - halfWidthBlocks) - MapTileConstants.TILE_BLOCKS);
        int maxTileX = MapTileMath.blockToTile((int) Math.ceil(centerX + halfWidthBlocks) + MapTileConstants.TILE_BLOCKS);
        int minTileZ = MapTileMath.blockToTile((int) Math.floor(centerZ - halfHeightBlocks) - MapTileConstants.TILE_BLOCKS);
        int maxTileZ = MapTileMath.blockToTile((int) Math.ceil(centerZ + halfHeightBlocks) + MapTileConstants.TILE_BLOCKS);

        Set<TileCoord> tiles = new HashSet<>();
        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for (int tileZ = minTileZ; tileZ <= maxTileZ; tileZ++) {
                tiles.add(new TileCoord(tileX, tileZ));
            }
        }
        return tiles;
    }
}
