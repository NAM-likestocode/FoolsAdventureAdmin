package com.fool.adventure.admin.network.payload;

import com.fool.adventure.FoolsAdventure;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DeleteContentPayload(ContentKind contentKind, String id) implements CustomPacketPayload {
    public enum ContentKind {
        BOSS,
        NPC,
        DIALOGUE
    }

    public static final Type<DeleteContentPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdventure.MODID, "delete_content"));

    public static final StreamCodec<ByteBuf, DeleteContentPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(
                    name -> ContentKind.valueOf(name.toUpperCase()),
                    kind -> kind.name().toLowerCase()
            ), DeleteContentPayload::contentKind,
            ByteBufCodecs.STRING_UTF8, DeleteContentPayload::id,
            DeleteContentPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
