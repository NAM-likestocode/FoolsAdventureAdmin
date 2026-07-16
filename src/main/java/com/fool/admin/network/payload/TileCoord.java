package com.fool.admin.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TileCoord(int tileX, int tileZ) {
    public static final StreamCodec<ByteBuf, TileCoord> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TileCoord::tileX,
            ByteBufCodecs.VAR_INT, TileCoord::tileZ,
            TileCoord::new
    );
}
