package com.fool.admin.content;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AdminEntityCatalog {
    public record CatalogEntry(Identifier entityTypeId, Component label) {
    }

    private static final Map<Identifier, CatalogEntry> BOSS_DEFAULTS = new LinkedHashMap<>();
    private static final Map<Identifier, CatalogEntry> NPC_DEFAULTS = new LinkedHashMap<>();

    static {
        registerBossDefault(Identifier.withDefaultNamespace("zombie"), "foolsadmin.admin.entity.zombie");
        registerNpcDefault(Identifier.withDefaultNamespace("villager"), "foolsadmin.admin.entity.villager");
    }

    private AdminEntityCatalog() {
    }

    public static void registerBossDefault(EntityType<?> entityType, String translationKey) {
        registerBossDefault(BuiltInRegistries.ENTITY_TYPE.getKey(entityType), translationKey);
    }

    public static void registerNpcDefault(EntityType<?> entityType, String translationKey) {
        registerNpcDefault(BuiltInRegistries.ENTITY_TYPE.getKey(entityType), translationKey);
    }

    public static void registerBossDefault(Identifier entityTypeId, String translationKey) {
        BOSS_DEFAULTS.put(entityTypeId, new CatalogEntry(entityTypeId, Component.translatable(translationKey)));
    }

    public static void registerNpcDefault(Identifier entityTypeId, String translationKey) {
        NPC_DEFAULTS.put(entityTypeId, new CatalogEntry(entityTypeId, Component.translatable(translationKey)));
    }

    public static Optional<CatalogEntry> defaultBoss() {
        return BOSS_DEFAULTS.values().stream().findFirst();
    }

    public static Optional<CatalogEntry> defaultNpc() {
        return NPC_DEFAULTS.values().stream().findFirst();
    }

    public static @Nullable Identifier parseEntityTypeId(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return Identifier.tryParse(trimmed);
    }

    public static boolean isValidMobType(Identifier entityTypeId) {
        EntityType<?> entityType = resolveEntityType(entityTypeId);
        if (entityType == null) {
            return false;
        }
        Class<?> baseClass = entityType.getBaseClass();
        return baseClass != null && Mob.class.isAssignableFrom(baseClass);
    }

    public static boolean isValidMobType(ServerLevel level, Identifier entityTypeId) {
        EntityType<?> entityType = resolveEntityType(entityTypeId);
        if (entityType == null) {
            return false;
        }
        Entity entity = entityType.create(level, EntitySpawnReason.COMMAND);
        return entity instanceof Mob;
    }

    public static EntityType<?> resolveEntityType(Identifier entityTypeId) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId).orElse(null);
    }
}
