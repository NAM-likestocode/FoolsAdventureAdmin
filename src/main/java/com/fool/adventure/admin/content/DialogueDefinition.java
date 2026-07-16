package com.fool.adventure.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

public record DialogueDefinition(
        String id,
        String name,
        List<DialogueLine> lines,
        int revision
) {
    public static final Codec<DialogueDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(DialogueDefinition::id),
            Codec.STRING.fieldOf("name").forGetter(DialogueDefinition::name),
            DialogueLine.CODEC.listOf().fieldOf("lines").forGetter(DialogueDefinition::lines),
            Codec.INT.fieldOf("revision").forGetter(DialogueDefinition::revision)
    ).apply(instance, (id, name, lines, revision) -> new DialogueDefinition(
            id,
            name,
            List.copyOf(lines),
            revision
    )));

    public DialogueDefinition withRevision(int newRevision) {
        return new DialogueDefinition(id, name, lines, newRevision);
    }

    public DialogueDefinition copyForEdit() {
        return new DialogueDefinition(id, name, new ArrayList<>(lines), revision);
    }
}
