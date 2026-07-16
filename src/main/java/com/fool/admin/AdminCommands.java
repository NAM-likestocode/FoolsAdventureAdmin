package com.fool.admin;

import com.fool.admin.network.payload.OpenAdminScreenPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
        );
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
}
