package com.fool.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record QuestPoint(
        String id,
        String name,
        float canvasX,
        float canvasY,
        QuestObjectiveType objectiveType,
        @Nullable String targetNpcId,
        @Nullable String targetBossId,
        @Nullable Identifier requiredItem,
        int requiredCount,
        List<String> prerequisiteIds,
        String dialogueScript,
        int revision
) {
    public static final Codec<QuestPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(QuestPoint::id),
            Codec.STRING.fieldOf("name").forGetter(QuestPoint::name),
            Codec.FLOAT.optionalFieldOf("canvas_x", 0.0F).forGetter(QuestPoint::canvasX),
            Codec.FLOAT.optionalFieldOf("canvas_y", 0.0F).forGetter(QuestPoint::canvasY),
            QuestObjectiveType.CODEC.fieldOf("objective_type").forGetter(QuestPoint::objectiveType),
            Codec.STRING.lenientOptionalFieldOf("target_npc_id").forGetter(quest -> Optional.ofNullable(quest.targetNpcId())),
            Codec.STRING.lenientOptionalFieldOf("target_boss_id").forGetter(quest -> Optional.ofNullable(quest.targetBossId())),
            Identifier.CODEC.lenientOptionalFieldOf("required_item").forGetter(quest -> Optional.ofNullable(quest.requiredItem())),
            Codec.INT.optionalFieldOf("required_count", 1).forGetter(QuestPoint::requiredCount),
            Codec.STRING.listOf().optionalFieldOf("prerequisite_ids", List.of()).forGetter(QuestPoint::prerequisiteIds),
            Codec.STRING.optionalFieldOf("dialogue_script", "").forGetter(QuestPoint::dialogueScript),
            Codec.INT.fieldOf("revision").forGetter(QuestPoint::revision)
    ).apply(instance, (id, name, canvasX, canvasY, objectiveType, targetNpcId, targetBossId, requiredItem, requiredCount, prerequisiteIds, dialogueScript, revision) -> new QuestPoint(
            id,
            name,
            canvasX,
            canvasY,
            objectiveType,
            targetNpcId.filter(value -> !value.isBlank()).orElse(null),
            targetBossId.filter(value -> !value.isBlank()).orElse(null),
            requiredItem.orElse(null),
            requiredCount,
            List.copyOf(prerequisiteIds),
            dialogueScript,
            revision
    )));

    public QuestPoint withName(String newName) {
        return new QuestPoint(id, newName, canvasX, canvasY, objectiveType, targetNpcId, targetBossId, requiredItem, requiredCount, prerequisiteIds, dialogueScript, revision);
    }

    public QuestPoint withCanvasPosition(float x, float y) {
        return new QuestPoint(id, name, x, y, objectiveType, targetNpcId, targetBossId, requiredItem, requiredCount, prerequisiteIds, dialogueScript, revision);
    }

    public QuestPoint withObjectiveType(QuestObjectiveType type) {
        return new QuestPoint(id, name, canvasX, canvasY, type, targetNpcId, targetBossId, requiredItem, requiredCount, prerequisiteIds, dialogueScript, revision);
    }

    public QuestPoint withTargetNpcId(@Nullable String npcId) {
        return new QuestPoint(id, name, canvasX, canvasY, objectiveType, npcId, targetBossId, requiredItem, requiredCount, prerequisiteIds, dialogueScript, revision);
    }

    public QuestPoint withTargetBossId(@Nullable String bossId) {
        return new QuestPoint(id, name, canvasX, canvasY, objectiveType, targetNpcId, bossId, requiredItem, requiredCount, prerequisiteIds, dialogueScript, revision);
    }

    public QuestPoint withRequiredItem(@Nullable Identifier item, int count) {
        return new QuestPoint(id, name, canvasX, canvasY, objectiveType, targetNpcId, targetBossId, item, count, prerequisiteIds, dialogueScript, revision);
    }

    public QuestPoint withPrerequisiteIds(List<String> prerequisites) {
        return new QuestPoint(id, name, canvasX, canvasY, objectiveType, targetNpcId, targetBossId, requiredItem, requiredCount, List.copyOf(prerequisites), dialogueScript, revision);
    }

    public QuestPoint withDialogueScript(String script) {
        return new QuestPoint(id, name, canvasX, canvasY, objectiveType, targetNpcId, targetBossId, requiredItem, requiredCount, prerequisiteIds, script, revision);
    }

    public QuestPoint withRevision(int newRevision) {
        return new QuestPoint(id, name, canvasX, canvasY, objectiveType, targetNpcId, targetBossId, requiredItem, requiredCount, prerequisiteIds, dialogueScript, newRevision);
    }

    public QuestPoint copyForEdit() {
        return new QuestPoint(
                id,
                name,
                canvasX,
                canvasY,
                objectiveType,
                targetNpcId,
                targetBossId,
                requiredItem,
                requiredCount,
                new ArrayList<>(prerequisiteIds),
                dialogueScript,
                revision
        );
    }
}
