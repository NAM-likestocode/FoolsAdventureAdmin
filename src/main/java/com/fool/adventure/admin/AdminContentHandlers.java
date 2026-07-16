package com.fool.adventure.admin;

import com.fool.adventure.admin.content.AdminContentService;
import com.fool.adventure.admin.content.AdminEntityRole;
import com.fool.adventure.admin.content.AdminEntityService;
import com.fool.adventure.admin.content.DialogueDefinition;
import com.fool.adventure.admin.content.DialogueService;
import com.fool.adventure.admin.content.ManagedEntityIdentity;
import com.fool.adventure.admin.content.NpcDefinition;
import com.fool.adventure.admin.network.payload.ContentMutationResultPayload;
import com.fool.adventure.admin.network.payload.ContentSnapshotPayload;
import com.fool.adventure.admin.network.payload.DeleteContentPayload;
import com.fool.adventure.admin.network.payload.RequestContentSnapshotPayload;
import com.fool.adventure.admin.network.payload.UpsertBossPayload;
import com.fool.adventure.admin.network.payload.UpsertDialoguePayload;
import com.fool.adventure.admin.network.payload.UpsertNpcPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class AdminContentHandlers {
    private AdminContentHandlers() {
    }

    public static void handleRequestSnapshot(RequestContentSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null || !AdminPermissions.hasAdmin(player)) {
                return;
            }
            if (!payload.dimension().equals(player.level().dimension())) {
                return;
            }
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            AdminContentService.ContentSnapshot snapshot = AdminContentService.snapshot(serverLevel);
            PacketDistributor.sendToPlayer(player, new ContentSnapshotPayload(
                    payload.dimension(),
                    snapshot.bosses(),
                    snapshot.npcs(),
                    snapshot.dialogues()
            ));
        });
    }

    public static void handleUpsertBoss(UpsertBossPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null || !AdminPermissions.hasAdmin(player)) {
                return;
            }
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            AdminContentService.MutationResult result = AdminContentService.upsertBoss(
                    serverLevel,
                    payload.draft(),
                    payload.expectedRevision(),
                    payload.spawnEntity()
            );
            sendMutationResult(player, result, null);
        });
    }

    public static void handleUpsertNpc(UpsertNpcPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null || !AdminPermissions.hasAdmin(player)) {
                return;
            }
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            AdminContentService.MutationResult result = AdminContentService.upsertNpc(
                    serverLevel,
                    payload.draft(),
                    payload.expectedRevision(),
                    payload.spawnEntity()
            );
            sendMutationResult(player, result, null);
        });
    }

    public static void handleUpsertDialogue(UpsertDialoguePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null || !AdminPermissions.hasAdmin(player)) {
                return;
            }
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            AdminContentService.MutationResult result = AdminContentService.upsertDialogue(
                    serverLevel,
                    payload.draft(),
                    payload.expectedRevision(),
                    payload.assignedNpcIds()
            );
            sendMutationResult(player, result, null);
        });
    }

    public static void handleDelete(DeleteContentPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null || !AdminPermissions.hasAdmin(player)) {
                return;
            }
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            AdminContentService.MutationResult result = switch (payload.contentKind()) {
                case BOSS -> AdminContentService.deleteBoss(serverLevel, payload.id());
                case NPC -> AdminContentService.deleteNpc(serverLevel, payload.id());
                case DIALOGUE -> AdminContentService.deleteDialogue(serverLevel, payload.id());
            };
            sendMutationResult(player, result, payload.id());
        });
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            AdminEntityService.onEntityJoin(serverLevel, event.getEntity());
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (tryHandleNpcDialogueInteract(event, event.getTarget())) {
            consumeInteraction(event);
        }
    }

    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (tryHandleNpcDialogueInteract(event, event.getTarget())) {
            consumeInteraction(event);
        } else if (!event.getLevel().isClientSide()
                && event.getHand() == InteractionHand.MAIN_HAND
                && event.getTarget() instanceof net.minecraft.world.entity.npc.villager.Villager
                && resolveNpc((ServerLevel) event.getLevel(), event.getTarget()) != null) {
            consumeInteraction(event);
        }
    }

    private static boolean tryHandleNpcDialogueInteract(PlayerInteractEvent event, Entity target) {
        if (event.getLevel().isClientSide()) {
            return false;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return false;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return false;
        }

        NpcDefinition npc = resolveNpc(serverLevel, target);
        if (npc == null) {
            return false;
        }

        if (npc.dialogueId() == null) {
            return target instanceof net.minecraft.world.entity.npc.villager.Villager;
        }

        DialogueDefinition dialogue = AdminContentService.get(serverLevel).dialogue(npc.dialogueId()).orElse(null);
        if (dialogue == null || !DialogueService.trigger(player, npc, dialogue)) {
            return target instanceof net.minecraft.world.entity.npc.villager.Villager;
        }

        DialogueService.tick(serverLevel);
        return true;
    }

    private static void consumeInteraction(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
    }

    private static void consumeInteraction(PlayerInteractEvent.EntityInteractSpecific event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
    }

    private static @Nullable NpcDefinition resolveNpc(ServerLevel level, Entity target) {
        ManagedEntityIdentity identity = ManagedEntityIdentity.read(target);
        if (identity != null && identity.role() == AdminEntityRole.NPC) {
            return AdminContentService.get(level).npc(identity.definitionId()).orElse(null);
        }

        UUID entityUuid = target.getUUID();
        for (NpcDefinition npc : AdminContentService.get(level).npcs()) {
            if (entityUuid.equals(npc.boundEntityUuid())) {
                return npc;
            }
        }
        return null;
    }

    private static void sendMutationResult(ServerPlayer player, AdminContentService.MutationResult result, String deletedId) {
        PacketDistributor.sendToPlayer(player, new ContentMutationResultPayload(
                result.success(),
                result.error() == null ? null : result.error().name(),
                result.boss(),
                result.npc(),
                result.dialogue(),
                deletedId
        ));
    }

    private static ServerPlayer player(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        return null;
    }
}
