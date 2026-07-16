package com.fool.adventure.admin;

import com.fool.adventure.admin.content.AdminEntityService;
import com.fool.adventure.admin.content.DialogueService;
import com.fool.adventure.admin.map.AdminMapService;
import com.fool.adventure.admin.network.payload.MapTilesResponsePayload;
import com.fool.adventure.admin.network.payload.OpenAdminScreenPayload;
import com.fool.adventure.admin.network.payload.RequestMapTilesPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class AdminServerHandlers {
    private static final Map<UUID, PlayerRateLimit> RATE_LIMITS = new HashMap<>();

    private AdminServerHandlers() {
    }

    public static void handleRequestMapTiles(RequestMapTilesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!AdminPermissions.hasAdmin(player)) {
                return;
            }
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            if (!payload.dimension().equals(player.level().dimension())) {
                return;
            }
            if (!allowRequest(player)) {
                return;
            }
            if (payload.tiles().isEmpty()) {
                return;
            }

            AdminMapService service = AdminMapService.get(serverLevel);
            MapTilesResponsePayload response = new MapTilesResponsePayload(
                    payload.requestId(),
                    payload.dimension(),
                    service.buildTileResponse(payload.dimension(), payload.tiles())
            );
            PacketDistributor.sendToPlayer(player, response);
        });
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            AdminMapService.get(level).processSamplingBudget();
            AdminEntityService.tick(level);
            DialogueService.tick(level);
        }
    }

    private static boolean allowRequest(ServerPlayer player) {
        long now = System.currentTimeMillis();
        PlayerRateLimit limit = RATE_LIMITS.computeIfAbsent(player.getUUID(), ignored -> new PlayerRateLimit());
        limit.requests.addLast(now);
        while (!limit.requests.isEmpty() && now - limit.requests.peekFirst() > 1000L) {
            limit.requests.removeFirst();
        }
        if (limit.requests.size() > com.fool.adventure.admin.map.MapTileConstants.MAX_REQUESTS_PER_SECOND) {
            return false;
        }
        return true;
    }

    private static final class PlayerRateLimit {
        private final Deque<Long> requests = new ArrayDeque<>();
    }
}
