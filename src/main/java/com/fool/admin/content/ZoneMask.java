package com.fool.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public final class ZoneMask {
    private static final int WORDS_PER_CHUNK = 4;
    public static final Codec<ZoneMask> CODEC = ChunkMask.CODEC.listOf().xmap(ZoneMask::fromChunks, ZoneMask::toChunks);

    private final Long2ObjectMap<long[]> chunks = new Long2ObjectOpenHashMap<>();

    public ZoneMask() {
    }

    private ZoneMask(List<ChunkMask> chunkMasks) {
        for (ChunkMask chunkMask : chunkMasks) {
            chunks.put(ChunkPos.pack(chunkMask.chunkX(), chunkMask.chunkZ()), chunkMask.words());
        }
    }

    public static ZoneMask fromChunks(List<ChunkMask> chunkMasks) {
        return new ZoneMask(chunkMasks);
    }

    public List<ChunkMask> toChunks() {
        List<ChunkMask> result = new ArrayList<>(chunks.size());
        for (Long2ObjectMap.Entry<long[]> entry : chunks.long2ObjectEntrySet()) {
            ChunkPos chunkPos = ChunkPos.unpack(entry.getLongKey());
            result.add(new ChunkMask(chunkPos.x(), chunkPos.z(), entry.getValue().clone()));
        }
        return result;
    }

    public boolean isEmpty() {
        return chunks.isEmpty();
    }

    public int paintedBlockCount() {
        int count = 0;
        for (long[] words : chunks.values()) {
            for (long word : words) {
                count += Long.bitCount(word);
            }
        }
        return count;
    }

    public boolean contains(int blockX, int blockZ) {
        int localX = blockX & 15;
        int localZ = blockZ & 15;
        long[] words = chunks.get(ChunkPos.pack(blockX >> 4, blockZ >> 4));
        if (words == null) {
            return false;
        }
        int bit = localZ * 16 + localX;
        return (words[bit >> 6] & (1L << (bit & 63))) != 0L;
    }

    public void paintDisc(int centerX, int centerZ, int radius, boolean erase) {
        int radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radiusSq) {
                    setBlock(centerX + dx, centerZ + dz, !erase);
                }
            }
        }
    }

    public void setBlock(int blockX, int blockZ, boolean painted) {
        long chunkKey = ChunkPos.pack(blockX >> 4, blockZ >> 4);
        long[] words = chunks.computeIfAbsent(chunkKey, ignored -> new long[WORDS_PER_CHUNK]);
        int localX = blockX & 15;
        int localZ = blockZ & 15;
        int bit = localZ * 16 + localX;
        int wordIndex = bit >> 6;
        long mask = 1L << (bit & 63);
        if (painted) {
            words[wordIndex] |= mask;
        } else {
            words[wordIndex] &= ~mask;
            if (isWordsEmpty(words)) {
                chunks.remove(chunkKey);
            }
        }
    }

    public ZoneMask copy() {
        ZoneMask copy = new ZoneMask();
        for (Long2ObjectMap.Entry<long[]> entry : chunks.long2ObjectEntrySet()) {
            copy.chunks.put(entry.getLongKey(), entry.getValue().clone());
        }
        return copy;
    }

    private static boolean isWordsEmpty(long[] words) {
        for (long word : words) {
            if (word != 0L) {
                return false;
            }
        }
        return true;
    }

    public record ChunkMask(int chunkX, int chunkZ, long[] words) {
        public static final Codec<ChunkMask> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("chunk_x").forGetter(ChunkMask::chunkX),
                Codec.INT.fieldOf("chunk_z").forGetter(ChunkMask::chunkZ),
                Codec.LONG.fieldOf("w0").forGetter(mask -> mask.words()[0]),
                Codec.LONG.fieldOf("w1").forGetter(mask -> mask.words()[1]),
                Codec.LONG.fieldOf("w2").forGetter(mask -> mask.words()[2]),
                Codec.LONG.fieldOf("w3").forGetter(mask -> mask.words()[3])
        ).apply(instance, (chunkX, chunkZ, w0, w1, w2, w3) -> new ChunkMask(chunkX, chunkZ, new long[]{w0, w1, w2, w3})));
    }
}
