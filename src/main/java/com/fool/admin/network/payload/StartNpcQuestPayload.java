package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StartNpcQuestPayload(String npcId, String campaignId) implements CustomPacketPayload {
    public static final Type<StartNpcQuestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "start_npc_quest"));
    public static final StreamCodec<ByteBuf, StartNpcQuestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, StartNpcQuestPayload::npcId,
            ByteBufCodecs.STRING_UTF8, StartNpcQuestPayload::campaignId,
            StartNpcQuestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
