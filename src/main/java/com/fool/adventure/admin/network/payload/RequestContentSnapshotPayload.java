package com.fool.adventure.admin.network.payload;

import com.fool.adventure.FoolsAdventure;
import com.fool.adventure.admin.network.payload.OpenAdminScreenPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record RequestContentSnapshotPayload(ResourceKey<Level> dimension) implements CustomPacketPayload {
    public static final Type<RequestContentSnapshotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdventure.MODID, "request_content_snapshot"));

    public static final StreamCodec<ByteBuf, RequestContentSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            OpenAdminScreenPayload.DIMENSION_CODEC, RequestContentSnapshotPayload::dimension,
            RequestContentSnapshotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
