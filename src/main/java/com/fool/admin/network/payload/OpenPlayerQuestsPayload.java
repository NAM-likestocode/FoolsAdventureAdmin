package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import com.fool.admin.content.Campaign;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenPlayerQuestsPayload(
        List<Campaign> campaigns,
        List<String> activeCampaignIds,
        List<String> completedQuestIds
) implements CustomPacketPayload {
    public static final Type<OpenPlayerQuestsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "open_player_quests"));

    public static final StreamCodec<ByteBuf, OpenPlayerQuestsPayload> STREAM_CODEC = StreamCodec.composite(
            ContentPayloadCodecs.CAMPAIGN_CODEC.apply(ByteBufCodecs.list(64)), OpenPlayerQuestsPayload::campaigns,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64)), OpenPlayerQuestsPayload::activeCampaignIds,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)), OpenPlayerQuestsPayload::completedQuestIds,
            OpenPlayerQuestsPayload::new
    );

    public OpenPlayerQuestsPayload {
        campaigns = List.copyOf(campaigns);
        activeCampaignIds = List.copyOf(activeCampaignIds);
        completedQuestIds = List.copyOf(completedQuestIds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
