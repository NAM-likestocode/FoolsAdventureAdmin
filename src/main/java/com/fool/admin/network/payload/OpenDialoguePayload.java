package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import com.fool.admin.content.DialogueScript;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenDialoguePayload(
        String npcDefinitionId,
        String npcDisplayName,
        Identifier entityTypeId,
        DialogueScript script
) implements CustomPacketPayload {
    public static final Type<OpenDialoguePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "open_dialogue"));

    public static final StreamCodec<ByteBuf, OpenDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenDialoguePayload::npcDefinitionId,
            ByteBufCodecs.STRING_UTF8, OpenDialoguePayload::npcDisplayName,
            PayloadCodecs.IDENTIFIER, OpenDialoguePayload::entityTypeId,
            ContentPayloadCodecs.DIALOGUE_SCRIPT_CODEC, OpenDialoguePayload::script,
            OpenDialoguePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
