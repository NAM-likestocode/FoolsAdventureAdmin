package com.fool.adventure.admin.network.payload;

import com.fool.adventure.FoolsAdventure;
import com.fool.adventure.admin.content.BossDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpsertBossPayload(
        BossDefinition draft,
        int expectedRevision,
        boolean spawnEntity
) implements CustomPacketPayload {
    public static final Type<UpsertBossPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdventure.MODID, "upsert_boss"));

    public static final StreamCodec<ByteBuf, UpsertBossPayload> STREAM_CODEC = StreamCodec.composite(
            ContentPayloadCodecs.BOSS_CODEC, UpsertBossPayload::draft,
            ByteBufCodecs.VAR_INT, UpsertBossPayload::expectedRevision,
            ByteBufCodecs.BOOL, UpsertBossPayload::spawnEntity,
            UpsertBossPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
