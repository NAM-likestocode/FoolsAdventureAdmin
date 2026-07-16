package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import com.fool.admin.map.MapTileConstants;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

public record MapTilesResponsePayload(
        int requestId,
        ResourceKey<Level> dimension,
        List<MapTilePayload> tiles
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MapTilesResponsePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "map_tiles_response"));

    public static final StreamCodec<ByteBuf, MapTilesResponsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MapTilesResponsePayload::requestId,
            OpenAdminScreenPayload.DIMENSION_CODEC, MapTilesResponsePayload::dimension,
            MapTilePayload.STREAM_CODEC.apply(ByteBufCodecs.list(MapTileConstants.MAX_TILES_PER_REQUEST)),
            MapTilesResponsePayload::tiles,
            MapTilesResponsePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public MapTilesResponsePayload {
        tiles = List.copyOf(tiles);
    }
}
