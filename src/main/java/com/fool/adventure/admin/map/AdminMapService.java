package com.fool.adventure.admin.map;

import com.fool.adventure.admin.network.payload.MapTilePayload;
import com.fool.adventure.admin.network.payload.TileCoord;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AdminMapService {
    private static final ConcurrentHashMap<ResourceKey<Level>, AdminMapService> SERVICES = new ConcurrentHashMap<>();

    private final ServerLevel level;
    private final MapTileStore store;
    private final Set<MapTileKey> dirtyTiles = ConcurrentHashMap.newKeySet();

    private AdminMapService(ServerLevel level) {
        this.level = level;
        this.store = new MapTileStore(level);
    }

    public static AdminMapService get(ServerLevel level) {
        return SERVICES.computeIfAbsent(level.dimension(), ignored -> new AdminMapService(level));
    }

    public static void clear(ServerLevel level) {
        SERVICES.remove(level.dimension());
    }

    public List<MapTilePayload> buildTileResponse(ResourceKey<Level> dimension, List<TileCoord> requestedTiles) {
        List<MapTilePayload> response = new ArrayList<>(requestedTiles.size());
        for (TileCoord coord : requestedTiles) {
            MapTileKey key = new MapTileKey(dimension, coord.tileX(), coord.tileZ());
            byte[] colors = store.load(key).orElseGet(MapTileSampler::createEmptyTile);
            MapTileSampler.sampleLoadedPixels(level, coord.tileX(), coord.tileZ(), colors, MapTileConstants.TILE_BYTES);
            boolean complete = MapTileSampler.isTileComplete(level, coord.tileX(), coord.tileZ());
            if (complete) {
                store.save(key, colors);
            } else {
                dirtyTiles.add(key);
            }
            response.add(new MapTilePayload(coord.tileX(), coord.tileZ(), colors, complete));
        }
        return response;
    }

    public void markTilesDirtyForChunk(ChunkPos chunkPos) {
        int blockOriginX = chunkPos.getMinBlockX();
        int blockOriginZ = chunkPos.getMinBlockZ();
        int minTileX = MapTileMath.blockToTile(blockOriginX);
        int maxTileX = MapTileMath.blockToTile(blockOriginX + 15);
        int minTileZ = MapTileMath.blockToTile(blockOriginZ);
        int maxTileZ = MapTileMath.blockToTile(blockOriginZ + 15);

        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for (int tileZ = minTileZ; tileZ <= maxTileZ; tileZ++) {
                dirtyTiles.add(new MapTileKey(level.dimension(), tileX, tileZ));
            }
        }
    }

    public void processSamplingBudget() {
        if (dirtyTiles.isEmpty()) {
            return;
        }

        int budget = MapTileConstants.SAMPLING_BUDGET_PER_TICK;
        Set<MapTileKey> processed = new HashSet<>();
        for (MapTileKey key : dirtyTiles) {
            if (budget <= 0) {
                break;
            }
            if (!processed.add(key)) {
                continue;
            }

            byte[] colors = store.load(key).orElseGet(MapTileSampler::createEmptyTile);
            int used = MapTileSampler.sampleLoadedPixels(level, key.tileX(), key.tileZ(), colors, budget);
            budget -= used;
            if (MapTileSampler.isTileComplete(level, key.tileX(), key.tileZ())) {
                store.save(key, colors);
                dirtyTiles.remove(key);
            } else if (used > 0) {
                store.save(key, colors);
            }
        }
    }
}
