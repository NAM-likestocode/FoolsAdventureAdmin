package com.fool.adventure.admin.network.payload;

import com.fool.adventure.FoolsAdventure;
import com.fool.adventure.admin.content.BossDefinition;
import com.fool.adventure.admin.content.DialogueDefinition;
import com.fool.adventure.admin.content.NpcDefinition;
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
        List<DialogueDefinition> dialogues
) implements CustomPacketPayload {
    public static final Type<ContentSnapshotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdventure.MODID, "content_snapshot"));

    public static final StreamCodec<ByteBuf, ContentSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            OpenAdminScreenPayload.DIMENSION_CODEC, ContentSnapshotPayload::dimension,
            ContentPayloadCodecs.BOSS_CODEC.apply(ByteBufCodecs.list(256)), ContentSnapshotPayload::bosses,
            ContentPayloadCodecs.NPC_CODEC.apply(ByteBufCodecs.list(256)), ContentSnapshotPayload::npcs,
            ContentPayloadCodecs.DIALOGUE_CODEC.apply(ByteBufCodecs.list(256)), ContentSnapshotPayload::dialogues,
            ContentSnapshotPayload::new
    );

    public ContentSnapshotPayload {
        bosses = List.copyOf(bosses);
        npcs = List.copyOf(npcs);
        dialogues = List.copyOf(dialogues);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
