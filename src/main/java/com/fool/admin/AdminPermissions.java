package com.fool.admin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class AdminPermissions {
    private AdminPermissions() {
    }

    public static boolean hasAdmin(CommandSourceStack source) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
    }

    public static boolean hasAdmin(ServerPlayer player) {
        return hasAdmin(player.createCommandSourceStack());
    }
}
