package com.fool.admin;

import com.fool.admin.map.AdminMapService;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

public final class AdminChunkEvents {
    private AdminChunkEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            AdminMapService.get(serverLevel).markTilesDirtyForChunk(event.getChunk().getPos());
        }
    }
}
