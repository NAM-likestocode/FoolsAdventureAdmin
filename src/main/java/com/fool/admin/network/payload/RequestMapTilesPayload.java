package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import com.fool.admin.map.MapTileConstants;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public record RequestMapTilesPayload(
        int requestId,
        ResourceKey<Level> dimension,
        List<TileCoord> tiles
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestMapTilesPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "request_map_tiles"));

    public static final StreamCodec<ByteBuf, RequestMapTilesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestMapTilesPayload::requestId,
            OpenAdminScreenPayload.DIMENSION_CODEC, RequestMapTilesPayload::dimension,
            TileCoord.STREAM_CODEC.apply(ByteBufCodecs.list(MapTileConstants.MAX_TILES_PER_REQUEST)),
            RequestMapTilesPayload::tiles,
            RequestMapTilesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public RequestMapTilesPayload {
        tiles = List.copyOf(tiles);
    }

    public static RequestMapTilesPayload create(int requestId, ResourceKey<Level> dimension, List<TileCoord> tiles) {
        if (tiles.size() > MapTileConstants.MAX_TILES_PER_REQUEST) {
            tiles = new ArrayList<>(tiles.subList(0, MapTileConstants.MAX_TILES_PER_REQUEST));
        }
        return new RequestMapTilesPayload(requestId, dimension, tiles);
    }
}
