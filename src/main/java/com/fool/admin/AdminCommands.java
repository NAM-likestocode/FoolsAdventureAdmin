package com.fool.admin;

import com.fool.admin.network.payload.OpenAdminScreenPayload;
import com.fool.admin.network.payload.OpenPlayerQuestsPayload;
import com.fool.admin.content.AdminContentService;
import com.fool.admin.content.QuestProgressService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicInteger;

public final class AdminCommands {
    private static final AtomicInteger SESSION_COUNTER = new AtomicInteger();

    private AdminCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("admin")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> openAdminGui(context.getSource()))
                        .then(Commands.literal("quests")
                                .then(Commands.literal("reset")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.literal("all").executes(context ->
                                                        resetAllQuests(context.getSource(), EntityArgument.getPlayer(context, "player"))))
                                                .then(Commands.literal("campaign")
                                                        .then(Commands.argument("campaignId", StringArgumentType.word())
                                                                .executes(context -> resetCampaignQuests(
                                                                        context.getSource(),
                                                                        EntityArgument.getPlayer(context, "player"),
                                                                        StringArgumentType.getString(context, "campaignId")
                                                                )))))))
        );
        dispatcher.register(Commands.literal("quests").executes(context -> openQuestMenu(context.getSource())));
    }

    private static int openAdminGui(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!AdminPermissions.hasAdmin(player)) {
            source.sendFailure(Component.translatable("foolsadmin.admin.denied"));
            return 0;
        }

        int sessionId = SESSION_COUNTER.incrementAndGet();
        PacketDistributor.sendToPlayer(player, OpenAdminScreenPayload.from(player, sessionId));
        source.sendSuccess(() -> Component.translatable("foolsadmin.admin.opened"), false);
        return 1;
    }

    private static int openQuestMenu(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var data = AdminContentService.get((net.minecraft.server.level.ServerLevel) player.level());
        PacketDistributor.sendToPlayer(player, new OpenPlayerQuestsPayload(
                data.campaigns(),
                QuestProgressService.activeCampaigns((net.minecraft.server.level.ServerLevel) player.level(), player.getUUID()).stream().toList(),
                QuestProgressService.completedQuestKeys((net.minecraft.server.level.ServerLevel) player.level(), player.getUUID()).stream().toList()
        ));
        return 1;
    }

    private static int resetAllQuests(CommandSourceStack source, ServerPlayer target) {
        QuestProgressService.resetAll((net.minecraft.server.level.ServerLevel) target.level(), target.getUUID());
        source.sendSuccess(() -> Component.literal("Reset all quest progress for " + target.getName().getString() + "."), true);
        return 1;
    }

    private static int resetCampaignQuests(CommandSourceStack source, ServerPlayer target, String campaignId) {
        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) target.level();
        if (AdminContentService.get(level).campaign(campaignId).isEmpty()) {
            source.sendFailure(Component.literal("Unknown campaign: " + campaignId));
            return 0;
        }
        QuestProgressService.resetCampaign(level, target.getUUID(), campaignId);
        source.sendSuccess(() -> Component.literal("Reset campaign quest progress for " + target.getName().getString() + "."), true);
        return 1;
    }
}
