package com.fool.admin.content;

import com.mojang.serialization.Codec;

public enum DialogueSpeaker {
    NPC,
    PLAYER;

    public static final Codec<DialogueSpeaker> CODEC = Codec.STRING.xmap(
            DialogueSpeaker::valueOf,
            DialogueSpeaker::name
    );
}
