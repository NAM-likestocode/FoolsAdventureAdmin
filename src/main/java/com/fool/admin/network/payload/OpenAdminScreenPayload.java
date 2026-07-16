package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public record OpenAdminScreenPayload(
        ResourceKey<Level> dimension,
        int playerBlockX,
        int playerBlockZ,
        float playerYaw,
        int sessionId
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenAdminScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "open_admin_screen"));

    public static final StreamCodec<ByteBuf, ResourceKey<Level>> DIMENSION_CODEC = ByteBufCodecs.STRING_UTF8.map(
            id -> ResourceKey.create(Registries.DIMENSION, Identifier.parse(id)),
            key -> key.identifier().toString()
    );

    public static final StreamCodec<ByteBuf, OpenAdminScreenPayload> STREAM_CODEC = StreamCodec.composite(
            DIMENSION_CODEC, OpenAdminScreenPayload::dimension,
            ByteBufCodecs.VAR_INT, OpenAdminScreenPayload::playerBlockX,
            ByteBufCodecs.VAR_INT, OpenAdminScreenPayload::playerBlockZ,
            ByteBufCodecs.FLOAT, OpenAdminScreenPayload::playerYaw,
            ByteBufCodecs.VAR_INT, OpenAdminScreenPayload::sessionId,
            OpenAdminScreenPayload::new
    );

    public static OpenAdminScreenPayload from(ServerPlayer player, int sessionId) {
        return new OpenAdminScreenPayload(
                player.level().dimension(),
                player.blockPosition().getX(),
                player.blockPosition().getZ(),
                player.getYRot(),
                sessionId
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
