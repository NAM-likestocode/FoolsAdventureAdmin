package com.fool.adventure.admin.network.payload;

import com.fool.adventure.admin.map.MapTileConstants;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MapTilePayload(int tileX, int tileZ, byte[] colors, boolean complete) {
    public static final StreamCodec<ByteBuf, MapTilePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MapTilePayload::tileX,
            ByteBufCodecs.VAR_INT, MapTilePayload::tileZ,
            ByteBufCodecs.byteArray(MapTileConstants.TILE_BYTES), MapTilePayload::colors,
            ByteBufCodecs.BOOL, MapTilePayload::complete,
            MapTilePayload::new
    );
}
