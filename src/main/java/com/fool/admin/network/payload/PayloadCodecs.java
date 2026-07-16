package com.fool.admin.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public final class PayloadCodecs {
    public static final StreamCodec<ByteBuf, Identifier> IDENTIFIER = ByteBufCodecs.STRING_UTF8.map(
            Identifier::parse,
            Identifier::toString
    );

    private PayloadCodecs() {
    }
}
