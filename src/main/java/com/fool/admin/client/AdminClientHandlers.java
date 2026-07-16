package com.fool.admin.client;

import com.fool.admin.client.screen.AdminScreen;
import com.fool.admin.network.payload.ContentMutationResultPayload;
import com.fool.admin.network.payload.ContentSnapshotPayload;
import com.fool.admin.network.payload.MapTilesResponsePayload;
import com.fool.admin.network.payload.OpenAdminScreenPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class AdminClientHandlers {
    private AdminClientHandlers() {
    }

    public static void handleOpenScreen(OpenAdminScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }
            AdminScreen screen = new AdminScreen(payload);
            minecraft.gui.pushScreenLayer(screen);
        });
    }

    public static void handleMapTiles(MapTilesResponsePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            AdminScreen screen = AdminScreen.getActive();
            if (screen != null) {
                screen.onTilesResponse(payload);
            }
        });
    }

    public static void handleContentSnapshot(ContentSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            AdminScreen screen = AdminScreen.getActive();
            if (screen != null) {
                screen.onContentSnapshot(payload);
            }
        });
    }

    public static void handleContentMutation(ContentMutationResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            AdminScreen screen = AdminScreen.getActive();
            if (screen != null) {
                screen.onContentMutation(payload);
            }
        });
    }
}
