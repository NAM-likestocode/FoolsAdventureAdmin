package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DeleteContentPayload(ContentKind contentKind, String id) implements CustomPacketPayload {
    public enum ContentKind {
        BOSS,
        NPC,
        QUEST,
        CAMPAIGN
    }

    public static final Type<DeleteContentPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "delete_content"));

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
