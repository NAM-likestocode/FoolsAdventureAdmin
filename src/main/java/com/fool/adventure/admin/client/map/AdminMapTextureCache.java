package com.fool.adventure.admin.client.map;

import com.fool.adventure.admin.client.AdminUiTheme;
import com.fool.adventure.admin.map.MapTileConstants;
import com.fool.adventure.admin.network.payload.TileCoord;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AdminMapTextureCache implements AutoCloseable {
    private static final int MAX_ENTRIES = 256;

    private final TextureManager textureManager;
    private final Map<TileTextureKey, TileTextureEntry> cache = new LinkedHashMap<>(128, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<TileTextureKey, TileTextureEntry> eldest) {
            if (size() > MAX_ENTRIES) {
                eldest.getValue().close(textureManager);
                return true;
            }
            return false;
        }
    };

    public AdminMapTextureCache(TextureManager textureManager) {
        this.textureManager = textureManager;
    }

    public void updateTile(ResourceKey<Level> dimension, int tileX, int tileZ, byte[] colors, boolean complete) {
        TileTextureKey key = new TileTextureKey(dimension, tileX, tileZ);
        TileTextureEntry entry = cache.computeIfAbsent(key, ignored -> createEntry(key));
        entry.upload(colors, complete);
    }

    public TileTextureEntry get(ResourceKey<Level> dimension, int tileX, int tileZ) {
        return cache.get(new TileTextureKey(dimension, tileX, tileZ));
    }

    public boolean hasComplete(ResourceKey<Level> dimension, int tileX, int tileZ) {
        TileTextureEntry entry = get(dimension, tileX, tileZ);
        return entry != null && entry.complete();
    }

    public List<TileCoord> findMissingTiles(ResourceKey<Level> dimension, Set<TileCoord> visible, int limit) {
        List<TileCoord> missing = new ArrayList<>();
        for (TileCoord coord : visible) {
            TileTextureEntry entry = get(dimension, coord.tileX(), coord.tileZ());
            if (entry == null || !entry.complete()) {
                missing.add(coord);
                if (missing.size() >= limit) {
                    break;
                }
            }
        }
        return missing;
    }

    public void clearDimension(ResourceKey<Level> dimension) {
        Iterator<Map.Entry<TileTextureKey, TileTextureEntry>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<TileTextureKey, TileTextureEntry> entry = iterator.next();
            if (entry.getKey().dimension().equals(dimension)) {
                entry.getValue().close(textureManager);
                iterator.remove();
            }
        }
    }

    private TileTextureEntry createEntry(TileTextureKey key) {
        DynamicTexture texture = new DynamicTexture(
                () -> "Admin map " + key.tileX() + "," + key.tileZ(),
                MapTileConstants.TILE_PIXELS,
                MapTileConstants.TILE_PIXELS,
                true
        );
        Identifier location = Identifier.fromNamespaceAndPath(
                "foolsadventure",
                "admin_map/" + key.dimension().identifier().getPath() + "/" + key.tileX() + "_" + key.tileZ()
        );
        textureManager.register(location, texture);
        return new TileTextureEntry(texture, location);
    }

    @Override
    public void close() {
        for (TileTextureEntry entry : cache.values()) {
            entry.close(textureManager);
        }
        cache.clear();
    }

    public record TileTextureKey(ResourceKey<Level> dimension, int tileX, int tileZ) {
    }

    public static final class TileTextureEntry implements AutoCloseable {
        private final DynamicTexture texture;
        private final Identifier location;
        private boolean complete;
        private boolean dirty = true;

        private TileTextureEntry(DynamicTexture texture, Identifier location) {
            this.texture = texture;
            this.location = location;
        }

        public Identifier location() {
            return location;
        }

        public boolean complete() {
            return complete;
        }

        public void upload(byte[] colors, boolean complete) {
            this.complete = complete;
            NativeImage pixels = texture.getPixels();
            for (int z = 0; z < MapTileConstants.TILE_PIXELS; z++) {
                for (int x = 0; x < MapTileConstants.TILE_PIXELS; x++) {
                    int index = x + z * MapTileConstants.TILE_PIXELS;
                    int packed = colors[index] & 0xFF;
                    int argb;
                    if (packed == 0) {
                        argb = complete ? MapColor.getColorFromPackedId((byte) 0) : AdminUiTheme.MAP_UNEXPLORED;
                    } else {
                        argb = MapColor.getColorFromPackedId((byte) packed);
                    }
                    pixels.setPixel(x, z, argb | 0xFF000000);
                }
            }
            texture.upload();
            dirty = false;
        }

        @Override
        public void close() {
            texture.close();
        }

        public void close(TextureManager textureManager) {
            textureManager.release(location);
            close();
        }
    }
}
