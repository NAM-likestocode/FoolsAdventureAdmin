package com.fool.adventure.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record NpcDefinition(
        String id,
        String displayName,
        Identifier entityTypeId,
        int spawnX,
        int spawnY,
        int spawnZ,
        List<Waypoint> waypoints,
        boolean repeatPath,
        boolean stationary,
        @Nullable String dialogueId,
        @Nullable UUID boundEntityUuid,
        int revision
) {
    public static final Codec<NpcDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(NpcDefinition::id),
            Codec.STRING.fieldOf("display_name").forGetter(NpcDefinition::displayName),
            Identifier.CODEC.fieldOf("entity_type").forGetter(NpcDefinition::entityTypeId),
            Codec.INT.fieldOf("spawn_x").forGetter(NpcDefinition::spawnX),
            Codec.INT.fieldOf("spawn_y").forGetter(NpcDefinition::spawnY),
            Codec.INT.fieldOf("spawn_z").forGetter(NpcDefinition::spawnZ),
            Waypoint.CODEC.listOf().fieldOf("waypoints").forGetter(NpcDefinition::waypoints),
            Codec.BOOL.optionalFieldOf("repeat_path", true).forGetter(NpcDefinition::repeatPath),
            Codec.BOOL.optionalFieldOf("stationary", false).forGetter(NpcDefinition::stationary),
            Codec.STRING.lenientOptionalFieldOf("dialogue_id").forGetter(definition -> Optional.ofNullable(definition.dialogueId())),
            Codec.STRING.lenientOptionalFieldOf("bound_entity_uuid").forGetter(definition -> Optional.ofNullable(definition.boundEntityUuid()).map(UUID::toString)),
            Codec.INT.fieldOf("revision").forGetter(NpcDefinition::revision)
    ).apply(instance, (id, displayName, entityTypeId, spawnX, spawnY, spawnZ, waypoints, repeatPath, stationary, dialogueId, boundUuid, revision) -> new NpcDefinition(
            id,
            displayName,
            entityTypeId,
            spawnX,
            spawnY,
            spawnZ,
            List.copyOf(waypoints),
            repeatPath,
            stationary,
            dialogueId.filter(value -> !value.isBlank()).orElse(null),
            boundUuid.filter(value -> !value.isBlank()).map(UUID::fromString).orElse(null),
            revision
    )));

    public BlockPos spawnPos() {
        return new BlockPos(spawnX, spawnY, spawnZ);
    }

    public NpcDefinition withBoundEntity(@Nullable UUID entityUuid) {
        return new NpcDefinition(id, displayName, entityTypeId, spawnX, spawnY, spawnZ, waypoints, repeatPath, stationary, dialogueId, entityUuid, revision);
    }

    public NpcDefinition withRevision(int newRevision) {
        return new NpcDefinition(id, displayName, entityTypeId, spawnX, spawnY, spawnZ, waypoints, repeatPath, stationary, dialogueId, boundEntityUuid, newRevision);
    }

    public NpcDefinition withStationary(boolean stationary) {
        return new NpcDefinition(id, displayName, entityTypeId, spawnX, spawnY, spawnZ, waypoints, repeatPath, stationary, dialogueId, boundEntityUuid, revision);
    }

    public NpcDefinition withDialogueId(@Nullable String dialogueId) {
        return new NpcDefinition(id, displayName, entityTypeId, spawnX, spawnY, spawnZ, waypoints, repeatPath, stationary, dialogueId, boundEntityUuid, revision);
    }

    public NpcDefinition withWaypoints(List<Waypoint> waypoints) {
        return new NpcDefinition(id, displayName, entityTypeId, spawnX, spawnY, spawnZ, waypoints, repeatPath, stationary, dialogueId, boundEntityUuid, revision);
    }

    public NpcDefinition copyForEdit() {
        return new NpcDefinition(
                id,
                displayName,
                entityTypeId,
                spawnX,
                spawnY,
                spawnZ,
                new ArrayList<>(waypoints),
                repeatPath,
                stationary,
                dialogueId,
                boundEntityUuid,
                revision
        );
    }
}
