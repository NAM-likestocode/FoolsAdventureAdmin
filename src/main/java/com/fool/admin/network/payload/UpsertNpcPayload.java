package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import com.fool.admin.content.NpcDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpsertNpcPayload(
        NpcDefinition draft,
        int expectedRevision,
        boolean spawnEntity
) implements CustomPacketPayload {
    public static final Type<UpsertNpcPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "upsert_npc"));

    public static final StreamCodec<ByteBuf, UpsertNpcPayload> STREAM_CODEC = StreamCodec.composite(
            ContentPayloadCodecs.NPC_CODEC, UpsertNpcPayload::draft,
            ByteBufCodecs.VAR_INT, UpsertNpcPayload::expectedRevision,
            ByteBufCodecs.BOOL, UpsertNpcPayload::spawnEntity,
            UpsertNpcPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
