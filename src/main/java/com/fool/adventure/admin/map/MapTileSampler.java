package com.fool.adventure.admin.map;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;

public final class MapTileSampler {
    private MapTileSampler() {
    }

    public static byte[] createEmptyTile() {
        return new byte[MapTileConstants.TILE_BYTES];
    }

    public static boolean isTileComplete(ServerLevel level, int tileX, int tileZ) {
        int originX = MapTileMath.tileOrigin(tileX);
        int originZ = MapTileMath.tileOrigin(tileZ);
        int minChunkX = SectionPos.blockToSectionCoord(originX);
        int maxChunkX = SectionPos.blockToSectionCoord(originX + MapTileConstants.TILE_BLOCKS - 1);
        int minChunkZ = SectionPos.blockToSectionCoord(originZ);
        int maxChunkZ = SectionPos.blockToSectionCoord(originZ + MapTileConstants.TILE_BLOCKS - 1);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null || chunk.isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    public static int sampleLoadedPixels(ServerLevel level, int tileX, int tileZ, byte[] colors, int budget) {
        int originX = MapTileMath.tileOrigin(tileX);
        int originZ = MapTileMath.tileOrigin(tileZ);
        int sampled = 0;
        double previousAverageHeight = 0.0D;
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();

        for (int localZ = 0; localZ < MapTileConstants.TILE_PIXELS && sampled < budget; localZ++) {
            for (int localX = 0; localX < MapTileConstants.TILE_PIXELS && sampled < budget; localX++) {
                int blockX = originX + localX;
                int blockZ = originZ + localZ;
                LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ));
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }

                byte packedColor = sampleBlock(level, chunk, blockX, blockZ, blockPos, belowPos, previousAverageHeight);
                int index = MapTileMath.pixelIndex(localX, localZ);
                if (colors[index] != packedColor) {
                    colors[index] = packedColor;
                    sampled++;
                }
            }
        }

        return sampled;
    }

    private static byte sampleBlock(
            ServerLevel level,
            LevelChunk chunk,
            int blockX,
            int blockZ,
            BlockPos.MutableBlockPos blockPos,
            BlockPos.MutableBlockPos belowPos,
            double previousAverageHeight
    ) {
        blockPos.set(blockX, 0, blockZ);
        int columnY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ) + 1;
        BlockState state;
        int waterDepth = 0;
        double averageAreaHeight;

        if (columnY <= level.getMinY()) {
            state = Blocks.BEDROCK.defaultBlockState();
            averageAreaHeight = level.getMinY();
        } else {
            do {
                blockPos.setY(--columnY);
                state = chunk.getBlockState(blockPos);
            } while (state.getMapColor(level, blockPos) == MapColor.NONE && columnY > level.getMinY());

            if (columnY > level.getMinY() && !state.getFluidState().isEmpty()) {
                int solidY = columnY - 1;
                belowPos.set(blockPos);

                BlockState belowBlock;
                do {
                    belowPos.setY(solidY--);
                    belowBlock = chunk.getBlockState(belowPos);
                    waterDepth++;
                } while (solidY > level.getMinY() && !belowBlock.getFluidState().isEmpty());

                state = correctStateForFluidBlock(level, state, blockPos);
            }

            averageAreaHeight = columnY;
        }

        Multiset<MapColor> colorCount = LinkedHashMultiset.create();
        colorCount.add(state.getMapColor(level, blockPos));

        MapColor color = Iterables.getFirst(Multisets.copyHighestCountFirst(colorCount), MapColor.NONE);
        MapColor.Brightness brightness;
        if (color == MapColor.WATER) {
            double diff = waterDepth * 0.1D + ((blockX + blockZ) & 1) * 0.2D;
            if (diff < 0.5D) {
                brightness = MapColor.Brightness.HIGH;
            } else if (diff > 0.9D) {
                brightness = MapColor.Brightness.LOW;
            } else {
                brightness = MapColor.Brightness.NORMAL;
            }
        } else {
            double diff = (averageAreaHeight - previousAverageHeight) * 4.0D / 5.0D + (((blockX + blockZ) & 1) - 0.5D) * 0.4D;
            if (diff > 0.6D) {
                brightness = MapColor.Brightness.HIGH;
            } else if (diff < -0.6D) {
                brightness = MapColor.Brightness.LOW;
            } else {
                brightness = MapColor.Brightness.NORMAL;
            }
        }

        return color.getPackedId(brightness);
    }

    private static BlockState correctStateForFluidBlock(ServerLevel level, BlockState state, BlockPos pos) {
        FluidState fluidState = state.getFluidState();
        return !fluidState.isEmpty() && !state.isFaceSturdy(level, pos, Direction.UP) ? fluidState.createLegacyBlock() : state;
    }
}
