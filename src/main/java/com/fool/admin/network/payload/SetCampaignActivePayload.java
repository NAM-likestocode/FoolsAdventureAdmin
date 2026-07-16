package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetCampaignActivePayload(String campaignId, boolean active) implements CustomPacketPayload {
    public static final Type<SetCampaignActivePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "set_campaign_active"));
    public static final StreamCodec<ByteBuf, SetCampaignActivePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetCampaignActivePayload::campaignId,
            ByteBufCodecs.BOOL, SetCampaignActivePayload::active,
            SetCampaignActivePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
