package com.fool.adventure.admin.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class WorldPositionResolver {
    private WorldPositionResolver() {
    }

    public static BlockPos resolveSurface(ServerLevel level, int blockX, int blockZ) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
        BlockPos pos = new BlockPos(blockX, y, blockZ);
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && state.getFluidState().isEmpty()) {
            return pos;
        }
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(blockX, 0, blockZ));
    }

    public static BlockPos resolveSurface(Level level, int blockX, int blockZ, int fallbackY) {
        if (level instanceof ServerLevel serverLevel) {
            return resolveSurface(serverLevel, blockX, blockZ);
        }
        return new BlockPos(blockX, fallbackY, blockZ);
    }
}
