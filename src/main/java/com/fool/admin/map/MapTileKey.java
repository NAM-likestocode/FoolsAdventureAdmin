package com.fool.admin.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record MapTileKey(ResourceKey<Level> dimension, int tileX, int tileZ) {
    public String storageFileName() {
        return "tile_" + tileX + "_" + tileZ + ".dat";
    }
}
