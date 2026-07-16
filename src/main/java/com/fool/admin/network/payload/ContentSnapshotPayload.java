package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import com.fool.admin.content.BossDefinition;
import com.fool.admin.content.Campaign;
import com.fool.admin.content.NpcDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

public record ContentSnapshotPayload(
        ResourceKey<Level> dimension,
        List<BossDefinition> bosses,
        List<NpcDefinition> npcs,
        List<Campaign> campaigns
) implements CustomPacketPayload {
    public static final Type<ContentSnapshotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "content_snapshot"));

    public static final StreamCodec<ByteBuf, ContentSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            OpenAdminScreenPayload.DIMENSION_CODEC, ContentSnapshotPayload::dimension,
            ContentPayloadCodecs.BOSS_CODEC.apply(ByteBufCodecs.list(256)), ContentSnapshotPayload::bosses,
            ContentPayloadCodecs.NPC_CODEC.apply(ByteBufCodecs.list(256)), ContentSnapshotPayload::npcs,
            ContentPayloadCodecs.CAMPAIGN_CODEC.apply(ByteBufCodecs.list(64)), ContentSnapshotPayload::campaigns,
            ContentSnapshotPayload::new
    );

    public ContentSnapshotPayload {
        bosses = List.copyOf(bosses);
        npcs = List.copyOf(npcs);
        campaigns = List.copyOf(campaigns);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
