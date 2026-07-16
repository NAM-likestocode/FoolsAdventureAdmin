package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CloseDialoguePayload(String npcDefinitionId, boolean completed) implements CustomPacketPayload {
    public static final Type<CloseDialoguePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "close_dialogue"));

    public static final StreamCodec<ByteBuf, CloseDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CloseDialoguePayload::npcDefinitionId,
            ByteBufCodecs.BOOL, CloseDialoguePayload::completed,
            CloseDialoguePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
