package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import com.fool.admin.content.QuestPoint;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpsertQuestPayload(
        String campaignId,
        QuestPoint draft,
        int expectedRevision
) implements CustomPacketPayload {
    public static final Type<UpsertQuestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "upsert_quest"));

    public static final StreamCodec<ByteBuf, UpsertQuestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, UpsertQuestPayload::campaignId,
            ContentPayloadCodecs.QUEST_CODEC, UpsertQuestPayload::draft,
            ByteBufCodecs.VAR_INT, UpsertQuestPayload::expectedRevision,
            UpsertQuestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
