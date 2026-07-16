package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import com.fool.admin.content.Campaign;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpsertCampaignPayload(Campaign draft, int expectedRevision) implements CustomPacketPayload {
    public static final Type<UpsertCampaignPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "upsert_campaign"));
    public static final StreamCodec<ByteBuf, UpsertCampaignPayload> STREAM_CODEC = StreamCodec.composite(
            ContentPayloadCodecs.CAMPAIGN_CODEC, UpsertCampaignPayload::draft,
            ByteBufCodecs.VAR_INT, UpsertCampaignPayload::expectedRevision,
            UpsertCampaignPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
