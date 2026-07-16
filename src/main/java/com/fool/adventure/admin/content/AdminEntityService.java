package com.fool.adventure.admin.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AdminEntityService {
    private static final Map<String, BossRuntimeState> BOSS_RUNTIME = new HashMap<>();
    private static final Map<String, NpcRuntimeState> NPC_RUNTIME = new HashMap<>();

    private AdminEntityService() {
    }

    public static BossDefinition spawnBoss(ServerLevel level, BossDefinition definition) {
        removeBoundEntity(level, definition.boundEntityUuid());
        EntityType<?> entityType = AdminEntityCatalog.resolveEntityType(definition.entityTypeId());
        if (entityType == null) {
            return definition;
        }

        Entity entity = entityType.create(level, EntitySpawnReason.COMMAND);
        if (!(entity instanceof Mob mob)) {
            return definition;
        }

        BlockPos spawn = definition.spawnPos();
        mob.snapTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, mob.getYRot(), mob.getYHeadRot());
        mob.setPersistenceRequired();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), EntitySpawnReason.COMMAND, null);
        mob.setCustomName(Component.literal(definition.displayName()));
        mob.setCustomNameVisible(true);

        if (mob instanceof Zombie zombie) {
            zombie.setBaby(false);
            if (zombie.getAttribute(Attributes.MAX_HEALTH) != null) {
                zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0D);
                zombie.setHealth(40.0F);
            }
        }

        ManagedEntityIdentity.apply(mob, new ManagedEntityIdentity(AdminEntityRole.BOSS, definition.id()));
        level.addFreshEntity(mob);
        BOSS_RUNTIME.put(definition.id(), new BossRuntimeState(spawn.getX(), spawn.getY(), spawn.getZ()));
        return definition.withBoundEntity(mob.getUUID());
    }

    public static NpcDefinition spawnNpc(ServerLevel level, NpcDefinition definition) {
        removeBoundEntity(level, definition.boundEntityUuid());
        EntityType<?> entityType = AdminEntityCatalog.resolveEntityType(definition.entityTypeId());
        if (entityType == null) {
            return definition;
        }

        Entity entity = entityType.create(level, EntitySpawnReason.COMMAND);
        if (!(entity instanceof Mob mob)) {
            return definition;
        }

        BlockPos spawn = definition.spawnPos();
        mob.snapTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, mob.getYRot(), mob.getYHeadRot());
        mob.setPersistenceRequired();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), EntitySpawnReason.COMMAND, null);
        mob.setCustomName(Component.literal(definition.displayName()));
        mob.setCustomNameVisible(true);

        if (mob instanceof Villager villager) {
            villager.setOffers(new MerchantOffers());
        }

        ManagedEntityIdentity.apply(mob, new ManagedEntityIdentity(AdminEntityRole.NPC, definition.id()));
        level.addFreshEntity(mob);
        NPC_RUNTIME.put(definition.id(), new NpcRuntimeState(0, 0));
        return definition.withBoundEntity(mob.getUUID());
    }

    public static void removeBoundEntity(ServerLevel level, @Nullable UUID entityUuid) {
        if (entityUuid == null) {
            return;
        }
        Entity entity = level.getEntity(entityUuid);
        if (entity != null) {
            entity.discard();
        }
    }

    public static void clearBossRuntime(String id) {
        BOSS_RUNTIME.remove(id);
    }

    public static void clearNpcRuntime(String id) {
        NPC_RUNTIME.remove(id);
    }

    public static void tick(ServerLevel level) {
        AdminContentSavedData data = AdminContentService.get(level);
        for (BossDefinition boss : data.bosses()) {
            tickBoss(level, boss);
        }
        for (NpcDefinition npc : data.npcs()) {
            tickNpc(level, npc);
        }
    }

    public static void onEntityJoin(ServerLevel level, Entity entity) {
        ManagedEntityIdentity identity = ManagedEntityIdentity.read(entity);
        if (identity == null) {
            identity = resolveIdentityFromBoundEntity(level, entity);
            if (identity != null) {
                ManagedEntityIdentity.apply(entity, identity);
            }
        }
        if (identity == null) {
            return;
        }
        if (identity.role() == AdminEntityRole.BOSS) {
            AdminContentService.get(level).boss(identity.definitionId()).ifPresent(boss -> {
                BlockPos spawn = boss.spawnPos();
                BOSS_RUNTIME.putIfAbsent(boss.id(), new BossRuntimeState(spawn.getX(), spawn.getY(), spawn.getZ()));
            });
        } else {
            NPC_RUNTIME.putIfAbsent(identity.definitionId(), new NpcRuntimeState(0, 0));
        }
    }

    private static @Nullable ManagedEntityIdentity resolveIdentityFromBoundEntity(ServerLevel level, Entity entity) {
        UUID entityUuid = entity.getUUID();
        AdminContentSavedData data = AdminContentService.get(level);
        for (NpcDefinition npc : data.npcs()) {
            if (entityUuid.equals(npc.boundEntityUuid())) {
                return new ManagedEntityIdentity(AdminEntityRole.NPC, npc.id());
            }
        }
        for (BossDefinition boss : data.bosses()) {
            if (entityUuid.equals(boss.boundEntityUuid())) {
                return new ManagedEntityIdentity(AdminEntityRole.BOSS, boss.id());
            }
        }
        return null;
    }

    private static void tickBoss(ServerLevel level, BossDefinition definition) {
        if (definition.boundEntityUuid() == null) {
            return;
        }
        Entity entity = level.getEntity(definition.boundEntityUuid());
        if (entity == null) {
            return;
        }
        if (!(entity instanceof Mob mob)) {
            return;
        }

        BossRuntimeState runtime = BOSS_RUNTIME.computeIfAbsent(
                definition.id(),
                ignored -> new BossRuntimeState(definition.spawnX(), definition.spawnY(), definition.spawnZ())
        );

        BlockPos current = mob.blockPosition();
        if (definition.zone().contains(current.getX(), current.getZ())) {
            runtime.lastValidX = current.getX();
            runtime.lastValidY = current.getY();
            runtime.lastValidZ = current.getZ();
        } else {
            mob.teleportTo(runtime.lastValidX + 0.5D, runtime.lastValidY, runtime.lastValidZ + 0.5D);
            mob.getNavigation().stop();
            return;
        }

        if (!definition.hasAttractionPoint()) {
            return;
        }

        if (isBossEngaged(mob, level)) {
            return;
        }

        guideBossToAttraction(mob, definition, runtime);
    }

    private static void guideBossToAttraction(Mob mob, BossDefinition definition, BossRuntimeState runtime) {
        BlockPos attraction = definition.attractionPos();
        double dx = mob.getX() - (attraction.getX() + 0.5D);
        double dz = mob.getZ() - (attraction.getZ() + 0.5D);
        if (dx * dx + dz * dz <= AdminContentConstants.WAYPOINT_ARRIVAL_DISTANCE_SQ) {
            mob.getNavigation().stop();
            runtime.attractionRepathCooldown = AdminContentConstants.BOSS_ATTRACTION_REPATH_TICKS;
            return;
        }

        if (runtime.attractionRepathCooldown > 0) {
            runtime.attractionRepathCooldown--;
            if (!mob.getNavigation().isDone()) {
                return;
            }
        }

        runtime.attractionRepathCooldown = AdminContentConstants.BOSS_ATTRACTION_REPATH_TICKS;
        mob.getNavigation().moveTo(
                attraction.getX() + 0.5D,
                attraction.getY(),
                attraction.getZ() + 0.5D,
                AdminContentConstants.BOSS_IDLE_MOVE_SPEED
        );
    }

    private static boolean isBossEngaged(Mob mob, ServerLevel level) {
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) {
            if (shouldKeepEngagedWith(mob, target)) {
                return true;
            }
            mob.setTarget(null);
        }

        LivingEntity lastHurtBy = mob.getLastHurtByMob();
        if (lastHurtBy != null
                && lastHurtBy.isAlive()
                && level.getGameTime() - mob.getLastHurtByMobTimestamp() < AdminContentConstants.BOSS_COMBAT_MEMORY_TICKS
                && shouldKeepEngagedWith(mob, lastHurtBy)) {
            return true;
        }
        return false;
    }

    private static boolean shouldKeepEngagedWith(Mob mob, LivingEntity entity) {
        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (mob.distanceToSqr(entity) > followRange * followRange) {
            return false;
        }
        return mob.getSensing().hasLineOfSight(entity);
    }

    private static void tickNpc(ServerLevel level, NpcDefinition definition) {
        if (definition.boundEntityUuid() == null) {
            return;
        }
        Entity entity = level.getEntity(definition.boundEntityUuid());
        if (entity == null) {
            return;
        }
        if (!(entity instanceof Mob mob)) {
            return;
        }
        if (mob instanceof Villager villager && villager.isTrading()) {
            return;
        }

        NpcRuntimeState runtime = NPC_RUNTIME.computeIfAbsent(definition.id(), ignored -> new NpcRuntimeState(0, 0));

        if (DialogueService.isNpcInDialogue(definition.id())) {
            mob.getNavigation().stop();
            DialogueService.facePlayer(mob, level);
            return;
        }

        if (definition.stationary()) {
            mob.getNavigation().stop();
            DialogueService.facePlayer(mob, level);
            return;
        }

        if (definition.waypoints().isEmpty()) {
            return;
        }

        if (runtime.dwellRemaining > 0) {
            runtime.dwellRemaining--;
            mob.getNavigation().stop();
            return;
        }

        if (runtime.waypointIndex >= definition.waypoints().size()) {
            if (definition.repeatPath()) {
                runtime.waypointIndex = 0;
            } else {
                return;
            }
        }

        Waypoint target = definition.waypoints().get(runtime.waypointIndex);
        if (!level.isLoaded(target.toBlockPos())) {
            return;
        }

        double dx = mob.getX() - (target.x() + 0.5D);
        double dz = mob.getZ() - (target.z() + 0.5D);
        double distanceSq = dx * dx + dz * dz;
        if (distanceSq <= AdminContentConstants.WAYPOINT_ARRIVAL_DISTANCE_SQ) {
            runtime.dwellRemaining = target.dwellTicks();
            runtime.waypointIndex++;
            if (runtime.waypointIndex >= definition.waypoints().size() && definition.repeatPath()) {
                runtime.waypointIndex = 0;
            }
            mob.getNavigation().stop();
            return;
        }

        if (mob.getNavigation().isDone()) {
            mob.getNavigation().moveTo(target.x() + 0.5D, target.y(), target.z() + 0.5D, AdminContentConstants.NPC_MOVE_SPEED);
        }
    }

    public static boolean isManagedNpc(Entity entity) {
        ManagedEntityIdentity identity = ManagedEntityIdentity.read(entity);
        return identity != null && identity.role() == AdminEntityRole.NPC;
    }

    public static void ensureManagedTag(ServerLevel level, NpcDefinition definition) {
        if (definition.boundEntityUuid() == null) {
            return;
        }
        Entity entity = level.getEntity(definition.boundEntityUuid());
        if (entity == null) {
            return;
        }
        ManagedEntityIdentity.apply(entity, new ManagedEntityIdentity(AdminEntityRole.NPC, definition.id()));
    }

    private static final class BossRuntimeState {
        private int lastValidX;
        private int lastValidY;
        private int lastValidZ;
        private int attractionRepathCooldown;

        private BossRuntimeState(int lastValidX, int lastValidY, int lastValidZ) {
            this.lastValidX = lastValidX;
            this.lastValidY = lastValidY;
            this.lastValidZ = lastValidZ;
        }
    }

    private static final class NpcRuntimeState {
        private int waypointIndex;
        private int dwellRemaining;

        private NpcRuntimeState(int waypointIndex, int dwellRemaining) {
            this.waypointIndex = waypointIndex;
            this.dwellRemaining = dwellRemaining;
        }
    }
}
