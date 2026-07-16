package com.fool.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DialogueLine(String text, int delayTicks) {
    public static final Codec<DialogueLine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("text").forGetter(DialogueLine::text),
            Codec.INT.optionalFieldOf("delay_ticks", AdminContentConstants.DEFAULT_LINE_DELAY_TICKS).forGetter(DialogueLine::delayTicks)
    ).apply(instance, DialogueLine::new));
}
