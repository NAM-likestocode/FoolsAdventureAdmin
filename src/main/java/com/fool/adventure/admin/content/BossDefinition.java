package com.fool.adventure.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record BossDefinition(
        String id,
        String displayName,
        Identifier entityTypeId,
        int spawnX,
        int spawnY,
        int spawnZ,
        ZoneMask zone,
        boolean hasAttractionPoint,
        int attractionX,
        int attractionY,
        int attractionZ,
        @Nullable UUID boundEntityUuid,
        int revision
) {
    public static final Codec<BossDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(BossDefinition::id),
            Codec.STRING.fieldOf("display_name").forGetter(BossDefinition::displayName),
            Identifier.CODEC.fieldOf("entity_type").forGetter(BossDefinition::entityTypeId),
            Codec.INT.fieldOf("spawn_x").forGetter(BossDefinition::spawnX),
            Codec.INT.fieldOf("spawn_y").forGetter(BossDefinition::spawnY),
            Codec.INT.fieldOf("spawn_z").forGetter(BossDefinition::spawnZ),
            ZoneMask.CODEC.fieldOf("zone").forGetter(BossDefinition::zone),
            Codec.BOOL.optionalFieldOf("has_attraction_point", false).forGetter(BossDefinition::hasAttractionPoint),
            Codec.INT.optionalFieldOf("attraction_x", 0).forGetter(BossDefinition::attractionX),
            Codec.INT.optionalFieldOf("attraction_y", 0).forGetter(BossDefinition::attractionY),
            Codec.INT.optionalFieldOf("attraction_z", 0).forGetter(BossDefinition::attractionZ),
            Codec.STRING.lenientOptionalFieldOf("bound_entity_uuid").forGetter(definition -> Optional.ofNullable(definition.boundEntityUuid()).map(UUID::toString)),
            Codec.INT.fieldOf("revision").forGetter(BossDefinition::revision)
    ).apply(instance, (id, displayName, entityTypeId, spawnX, spawnY, spawnZ, zone, hasAttractionPoint, attractionX, attractionY, attractionZ, boundUuid, revision) -> new BossDefinition(
            id,
            displayName,
            entityTypeId,
            spawnX,
            spawnY,
            spawnZ,
            zone,
            hasAttractionPoint,
            attractionX,
            attractionY,
            attractionZ,
            boundUuid.filter(value -> !value.isBlank()).map(UUID::fromString).orElse(null),
            revision
    )));

    public BlockPos spawnPos() {
        return new BlockPos(spawnX, spawnY, spawnZ);
    }

    public BlockPos attractionPos() {
        return new BlockPos(attractionX, attractionY, attractionZ);
    }

    public BossDefinition withDisplayName(String name) {
        return new BossDefinition(id, name, entityTypeId, spawnX, spawnY, spawnZ, zone, hasAttractionPoint, attractionX, attractionY, attractionZ, boundEntityUuid, revision);
    }

    public BossDefinition withEntityType(Identifier entityTypeId) {
        return new BossDefinition(id, displayName, entityTypeId, spawnX, spawnY, spawnZ, zone, hasAttractionPoint, attractionX, attractionY, attractionZ, boundEntityUuid, revision);
    }

    public BossDefinition withSpawn(int x, int y, int z) {
        return new BossDefinition(id, displayName, entityTypeId, x, y, z, zone, hasAttractionPoint, attractionX, attractionY, attractionZ, boundEntityUuid, revision);
    }

    public BossDefinition withZone(ZoneMask zone) {
        return new BossDefinition(id, displayName, entityTypeId, spawnX, spawnY, spawnZ, zone, hasAttractionPoint, attractionX, attractionY, attractionZ, boundEntityUuid, revision);
    }

    public BossDefinition withAttractionPoint(int x, int y, int z) {
        return new BossDefinition(id, displayName, entityTypeId, spawnX, spawnY, spawnZ, zone, true, x, y, z, boundEntityUuid, revision);
    }

    public BossDefinition withBoundEntity(@Nullable UUID entityUuid) {
        return new BossDefinition(id, displayName, entityTypeId, spawnX, spawnY, spawnZ, zone, hasAttractionPoint, attractionX, attractionY, attractionZ, entityUuid, revision);
    }

    public BossDefinition withRevision(int newRevision) {
        return new BossDefinition(id, displayName, entityTypeId, spawnX, spawnY, spawnZ, zone, hasAttractionPoint, attractionX, attractionY, attractionZ, boundEntityUuid, newRevision);
    }

    public BossDefinition copyForEdit() {
        return new BossDefinition(id, displayName, entityTypeId, spawnX, spawnY, spawnZ, zone.copy(), hasAttractionPoint, attractionX, attractionY, attractionZ, boundEntityUuid, revision);
    }
}
