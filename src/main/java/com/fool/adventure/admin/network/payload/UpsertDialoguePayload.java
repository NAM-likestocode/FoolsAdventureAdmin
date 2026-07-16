package com.fool.adventure.admin.network.payload;

import com.fool.adventure.FoolsAdventure;
import com.fool.adventure.admin.content.DialogueDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record UpsertDialoguePayload(
        DialogueDefinition draft,
        int expectedRevision,
        java.util.List<String> assignedNpcIds
) implements CustomPacketPayload {
    public static final Type<UpsertDialoguePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdventure.MODID, "upsert_dialogue"));

    public static final StreamCodec<ByteBuf, UpsertDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ContentPayloadCodecs.DIALOGUE_CODEC, UpsertDialoguePayload::draft,
            ByteBufCodecs.VAR_INT, UpsertDialoguePayload::expectedRevision,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)), UpsertDialoguePayload::assignedNpcIds,
            UpsertDialoguePayload::new
    );

    public UpsertDialoguePayload {
        assignedNpcIds = List.copyOf(assignedNpcIds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
