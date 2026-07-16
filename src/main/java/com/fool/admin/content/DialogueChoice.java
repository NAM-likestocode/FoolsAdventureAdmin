package com.fool.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public record DialogueChoice(String label, @Nullable String targetNodeId) {
    public static final Codec<DialogueChoice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("label").forGetter(DialogueChoice::label),
            Codec.STRING.lenientOptionalFieldOf("target_node_id").forGetter(choice -> Optional.ofNullable(choice.targetNodeId()))
    ).apply(instance, (label, targetNodeId) -> new DialogueChoice(
            label,
            targetNodeId.filter(value -> !value.isBlank()).orElse(null)
    )));
}
