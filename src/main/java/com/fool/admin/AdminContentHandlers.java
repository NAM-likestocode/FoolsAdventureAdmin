package com.fool.admin;

import com.fool.admin.content.AdminContentSavedData;
import com.fool.admin.content.AdminContentService;
import com.fool.admin.content.Campaign;
import com.fool.admin.content.AdminEntityRole;
import com.fool.admin.content.AdminEntityService;
import com.fool.admin.content.DialogueScriptParser;
import com.fool.admin.content.DialogueService;
import com.fool.admin.content.DialogueService.ItemReservation;
import com.fool.admin.content.ManagedEntityIdentity;
import com.fool.admin.content.NpcDefinition;
import com.fool.admin.content.QuestObjectiveType;
import com.fool.admin.content.QuestItemDelivery;
import com.fool.admin.content.QuestPoint;
import com.fool.admin.content.QuestProgressService;
import com.fool.admin.network.payload.CloseDialoguePayload;
import com.fool.admin.network.payload.ContentMutationResultPayload;
import com.fool.admin.network.payload.ContentSnapshotPayload;
import com.fool.admin.network.payload.DeleteContentPayload;
import com.fool.admin.network.payload.OpenDialoguePayload;
import com.fool.admin.network.payload.RequestContentSnapshotPayload;
import com.fool.admin.network.payload.UpsertBossPayload;
import com.fool.admin.network.payload.UpsertCampaignPayload;
import com.fool.admin.network.payload.UpsertNpcPayload;
import com.fool.admin.network.payload.UpsertQuestPayload;
import com.fool.admin.network.payload.SetCampaignActivePayload;
import com.fool.admin.network.payload.StartNpcQuestPayload;
import com.fool.admin.network.payload.OpenNpcQuestCampaignsPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.List;
import java.util.Set;

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
                    snapshot.campaigns()
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

    public static void handleUpsertQuest(UpsertQuestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null || !AdminPermissions.hasAdmin(player)) {
                return;
            }
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            AdminContentService.MutationResult result = AdminContentService.upsertQuest(
                    serverLevel,
                    payload.campaignId(),
                    payload.draft(),
                    payload.expectedRevision()
            );
            sendMutationResult(player, result, null);
        });
    }

    public static void handleUpsertCampaign(UpsertCampaignPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null || !AdminPermissions.hasAdmin(player) || !(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            AdminContentService.MutationResult result = AdminContentService.upsertCampaign(serverLevel, payload.draft(), payload.expectedRevision());
            if (!result.success()) {
                sendMutationResult(player, result, null);
                return;
            }
            AdminContentService.ContentSnapshot snapshot = AdminContentService.snapshot(serverLevel);
            PacketDistributor.sendToPlayer(player, new ContentSnapshotPayload(
                    player.level().dimension(), snapshot.bosses(), snapshot.npcs(), snapshot.campaigns()
            ));
        });
    }

    public static void handleCloseDialogue(CloseDialoguePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null) {
                return;
            }
            DialogueService.ActiveSession session = DialogueService.endOverlay(player.getUUID());
            if (session == null) {
                return;
            }
            if (!payload.completed()) {
                refundReservedItems(player, session.itemReservation());
                return;
            }
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            QuestPoint quest = AdminContentService.get(serverLevel).quest(session.campaignId(), session.questId()).orElse(null);
            if (quest == null) {
                return;
            }
            if (quest.objectiveType() == QuestObjectiveType.TALK_TO_NPC
                    || quest.objectiveType() == QuestObjectiveType.ITEM_TO_NPC) {
                QuestProgressService.markCompleted(serverLevel, player.getUUID(), session.campaignId(), quest.id());
            }
        });
    }

    public static void handleSetCampaignActive(SetCampaignActivePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            AdminContentSavedData content = AdminContentService.get(level);
            content.campaign(payload.campaignId()).ifPresent(campaign ->
                    QuestProgressService.setCampaignActive(level, player.getUUID(), campaign, content, payload.active()));
        });
    }

    public static void handleStartNpcQuest(StartNpcQuestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = player(context);
            if (player == null || !(player.level() instanceof ServerLevel level)
                    || !DialogueService.consumeCampaignSelection(player.getUUID(), payload.npcId(), payload.campaignId())) {
                return;
            }
            NpcDefinition npc = AdminContentService.get(level).npc(payload.npcId()).orElse(null);
            if (npc == null) {
                return;
            }
            QuestProgressService.AvailableQuest availableQuest = QuestProgressService.availableQuestsForNpc(level, player.getUUID(), npc.id())
                    .stream()
                    .filter(quest -> quest.campaignId().equals(payload.campaignId()))
                    .findFirst()
                    .orElseGet(() -> QuestProgressService.availableInactiveQuestsForNpc(level, player.getUUID(), npc.id())
                            .stream()
                            .filter(quest -> quest.campaignId().equals(payload.campaignId()))
                            .findFirst()
                            .orElse(null));
            if (availableQuest == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("That quest is no longer available."));
                return;
            }
            if (!ensureCampaignActive(player, level, availableQuest.campaignId())) {
                return;
            }
            startNpcQuest(player, level, npc, availableQuest);
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
                case QUEST -> AdminContentService.deleteQuest(serverLevel, payload.id());
                case CAMPAIGN -> AdminContentService.deleteCampaign(serverLevel, payload.id());
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

        if (DialogueService.hasActiveOverlay(player.getUUID())) {
            return true;
        }
        List<QuestProgressService.AvailableQuest> availableQuests =
                QuestProgressService.availableQuestsForNpc(serverLevel, player.getUUID(), npc.id());
        if (availableQuests.isEmpty()) {
            availableQuests = QuestProgressService.availableInactiveQuestsForNpc(serverLevel, player.getUUID(), npc.id());
            if (availableQuests.isEmpty()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("I have no quests at the moment."));
                return true;
            }
        }
        if (availableQuests.size() == 1) {
            QuestProgressService.AvailableQuest availableQuest = availableQuests.getFirst();
            if (ensureCampaignActive(player, serverLevel, availableQuest.campaignId())) {
                startNpcQuest(player, serverLevel, npc, availableQuest);
            }
            return true;
        }

        Set<String> campaignIds = availableQuests.stream()
                .map(QuestProgressService.AvailableQuest::campaignId)
                .collect(java.util.stream.Collectors.toSet());
        DialogueService.beginCampaignSelection(player.getUUID(), npc.id(), campaignIds);
        PacketDistributor.sendToPlayer(player, new OpenNpcQuestCampaignsPayload(
                npc.id(),
                npc.displayName(),
                availableQuests.stream()
                        .map(quest -> new OpenNpcQuestCampaignsPayload.CampaignOption(
                                quest.campaignId(),
                                AdminContentService.get(serverLevel).campaign(quest.campaignId()).orElseThrow().name()
                        ))
                        .distinct()
                        .toList()
        ));
        return true;
    }

    private static boolean ensureCampaignActive(ServerPlayer player, ServerLevel level, String campaignId) {
        if (QuestProgressService.activeCampaigns(level, player.getUUID()).contains(campaignId)) {
            return true;
        }
        AdminContentSavedData content = AdminContentService.get(level);
        Campaign campaign = content.campaign(campaignId).orElse(null);
        if (campaign == null || !QuestProgressService.setCampaignActive(level, player.getUUID(), campaign, content, true)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("That campaign is locked."));
            return false;
        }
        return true;
    }

    private static void startNpcQuest(
            ServerPlayer player,
            ServerLevel serverLevel,
            NpcDefinition npc,
            QuestProgressService.AvailableQuest availableQuest
    ) {
        QuestPoint quest = availableQuest.quest();
        if (quest.dialogueScript().isBlank()) {
            if (quest.objectiveType() == QuestObjectiveType.ITEM_TO_NPC && consumeRequiredItems(player, quest) == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "You need " + quest.requiredCount() + " " + quest.requiredItem() + " to continue."
                ));
                return;
            }
            QuestProgressService.markCompleted(serverLevel, player.getUUID(), availableQuest.campaignId(), quest.id());
            return;
        }
        DialogueScriptParser.ParseResult parseResult = DialogueScriptParser.parse(quest.dialogueScript());
        if (!parseResult.success() || parseResult.script() == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("This quest cannot be started right now."));
            return;
        }
        ItemReservation reservation = null;
        if (quest.objectiveType() == QuestObjectiveType.ITEM_TO_NPC) {
            reservation = consumeRequiredItems(player, quest);
            if (reservation == null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "You need " + quest.requiredCount() + " " + quest.requiredItem() + " to continue."
                ));
                return;
            }
        }
        if (!DialogueService.beginOverlay(player.getUUID(), npc.id(), availableQuest.campaignId(), quest.id(), reservation)) {
            refundReservedItems(player, reservation);
            return;
        }
        PacketDistributor.sendToPlayer(player, new OpenDialoguePayload(
                npc.id(),
                npc.displayName(),
                npc.entityTypeId(),
                parseResult.script()
        ));
    }

    private static @Nullable ItemReservation consumeRequiredItems(ServerPlayer player, QuestPoint quest) {
        if (quest.requiredItem() == null) {
            return null;
        }
        net.minecraft.world.item.Item requiredItem = BuiltInRegistries.ITEM.get(quest.requiredItem())
                .map(net.minecraft.core.Holder.Reference::value)
                .orElse(null);
        if (requiredItem == null) {
            return null;
        }
        return QuestItemDelivery.tryConsume(player.getInventory(), requiredItem, quest.requiredCount())
                ? new ItemReservation(quest.requiredItem(), quest.requiredCount())
                : null;
    }

    private static void refundReservedItems(ServerPlayer player, @Nullable ItemReservation reservation) {
        if (reservation == null) {
            return;
        }
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(reservation.itemId())
                .map(net.minecraft.core.Holder.Reference::value)
                .orElse(null);
        if (item == null) {
            return;
        }
        net.minecraft.world.item.ItemStack refund = new net.minecraft.world.item.ItemStack(item, reservation.count());
        if (!player.getInventory().add(refund)) {
            player.drop(refund, false);
        }
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
                result.quest(),
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
