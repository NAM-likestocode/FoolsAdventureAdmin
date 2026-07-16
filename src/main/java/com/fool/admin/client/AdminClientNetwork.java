package com.fool.admin.client;

import com.fool.admin.network.payload.ContentMutationResultPayload;
import com.fool.admin.network.payload.ContentSnapshotPayload;
import com.fool.admin.network.payload.MapTilesResponsePayload;
import com.fool.admin.network.payload.OpenAdminScreenPayload;
import com.fool.admin.network.payload.OpenDialoguePayload;
import com.fool.admin.network.payload.OpenPlayerQuestsPayload;
import com.fool.admin.network.payload.OpenNpcQuestCampaignsPayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public final class AdminClientNetwork {
    private AdminClientNetwork() {
    }

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenAdminScreenPayload.TYPE, AdminClientHandlers::handleOpenScreen);
        event.register(OpenDialoguePayload.TYPE, AdminClientHandlers::handleOpenDialogue);
        event.register(OpenPlayerQuestsPayload.TYPE, AdminClientHandlers::handleOpenPlayerQuests);
        event.register(OpenNpcQuestCampaignsPayload.TYPE, AdminClientHandlers::handleOpenNpcQuestCampaigns);
        event.register(MapTilesResponsePayload.TYPE, AdminClientHandlers::handleMapTiles);
        event.register(ContentSnapshotPayload.TYPE, AdminClientHandlers::handleContentSnapshot);
        event.register(ContentMutationResultPayload.TYPE, AdminClientHandlers::handleContentMutation);
    }
}
