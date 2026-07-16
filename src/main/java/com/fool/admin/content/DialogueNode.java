package com.fool.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record DialogueNode(
        String id,
        DialogueSpeaker speaker,
        String text,
        int delayTicks,
        @Nullable String nextNodeId,
        List<DialogueChoice> choices
) {
    public static final Codec<DialogueNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(DialogueNode::id),
            DialogueSpeaker.CODEC.fieldOf("speaker").forGetter(DialogueNode::speaker),
            Codec.STRING.fieldOf("text").forGetter(DialogueNode::text),
            Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(DialogueNode::delayTicks),
            Codec.STRING.lenientOptionalFieldOf("next_node_id").forGetter(node -> Optional.ofNullable(node.nextNodeId())),
            DialogueChoice.CODEC.listOf().optionalFieldOf("choices", List.of()).forGetter(DialogueNode::choices)
    ).apply(instance, (id, speaker, text, delayTicks, nextNodeId, choices) -> new DialogueNode(
            id,
            speaker,
            text,
            delayTicks,
            nextNodeId.filter(value -> !value.isBlank()).orElse(null),
            List.copyOf(choices)
    )));

    public boolean isChoiceNode() {
        return !choices.isEmpty();
    }
}
