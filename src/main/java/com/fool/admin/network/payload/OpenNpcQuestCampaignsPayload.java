package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenNpcQuestCampaignsPayload(String npcId, String npcDisplayName, List<CampaignOption> campaigns)
        implements CustomPacketPayload {
    public static final Type<OpenNpcQuestCampaignsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "open_npc_quest_campaigns"));
    private static final StreamCodec<ByteBuf, CampaignOption> OPTION_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CampaignOption::id,
            ByteBufCodecs.STRING_UTF8, CampaignOption::name,
            CampaignOption::new
    );
    public static final StreamCodec<ByteBuf, OpenNpcQuestCampaignsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenNpcQuestCampaignsPayload::npcId,
            ByteBufCodecs.STRING_UTF8, OpenNpcQuestCampaignsPayload::npcDisplayName,
            OPTION_CODEC.apply(ByteBufCodecs.list(64)), OpenNpcQuestCampaignsPayload::campaigns,
            OpenNpcQuestCampaignsPayload::new
    );

    public OpenNpcQuestCampaignsPayload {
        campaigns = List.copyOf(campaigns);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record CampaignOption(String id, String name) {
    }
}
