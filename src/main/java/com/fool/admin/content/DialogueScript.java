package com.fool.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record DialogueScript(String entryNodeId, List<DialogueNode> nodes) {
    public static final Codec<DialogueScript> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("entry_node_id").forGetter(DialogueScript::entryNodeId),
            DialogueNode.CODEC.listOf().fieldOf("nodes").forGetter(DialogueScript::nodes)
    ).apply(instance, (entryNodeId, nodes) -> new DialogueScript(entryNodeId, List.copyOf(nodes))));

    public DialogueNode node(String nodeId) {
        for (DialogueNode node : nodes) {
            if (node.id().equals(nodeId)) {
                return node;
            }
        }
        throw new IllegalArgumentException("Unknown dialogue node: " + nodeId);
    }
}
