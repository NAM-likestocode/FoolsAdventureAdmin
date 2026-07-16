package com.fool.admin.network;

import com.fool.admin.AdminContentHandlers;
import com.fool.admin.AdminServerHandlers;
import com.fool.admin.network.payload.CloseDialoguePayload;
import com.fool.admin.network.payload.DeleteContentPayload;
import com.fool.admin.network.payload.MapTilesResponsePayload;
import com.fool.admin.network.payload.OpenAdminScreenPayload;
import com.fool.admin.network.payload.OpenDialoguePayload;
import com.fool.admin.network.payload.OpenPlayerQuestsPayload;
import com.fool.admin.network.payload.OpenNpcQuestCampaignsPayload;
import com.fool.admin.network.payload.SetCampaignActivePayload;
import com.fool.admin.network.payload.StartNpcQuestPayload;
import com.fool.admin.network.payload.RequestContentSnapshotPayload;
import com.fool.admin.network.payload.RequestMapTilesPayload;
import com.fool.admin.network.payload.UpsertBossPayload;
import com.fool.admin.network.payload.UpsertNpcPayload;
import com.fool.admin.network.payload.UpsertQuestPayload;
import com.fool.admin.network.payload.UpsertCampaignPayload;
import com.fool.admin.network.payload.ContentMutationResultPayload;
import com.fool.admin.network.payload.ContentSnapshotPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AdminNetwork {
    public static final String PROTOCOL_VERSION = "4";

    private AdminNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                OpenAdminScreenPayload.TYPE,
                OpenAdminScreenPayload.STREAM_CODEC
        );

        registrar.playToClient(
                OpenDialoguePayload.TYPE,
                OpenDialoguePayload.STREAM_CODEC
        );
        registrar.playToClient(
                OpenPlayerQuestsPayload.TYPE,
                OpenPlayerQuestsPayload.STREAM_CODEC
        );
        registrar.playToClient(
                OpenNpcQuestCampaignsPayload.TYPE,
                OpenNpcQuestCampaignsPayload.STREAM_CODEC
        );

        registrar.playToServer(
                RequestMapTilesPayload.TYPE,
                RequestMapTilesPayload.STREAM_CODEC,
                AdminServerHandlers::handleRequestMapTiles
        );

        registrar.playToClient(
                MapTilesResponsePayload.TYPE,
                MapTilesResponsePayload.STREAM_CODEC
        );

        registrar.playToClient(
                ContentSnapshotPayload.TYPE,
                ContentSnapshotPayload.STREAM_CODEC
        );

        registrar.playToClient(
                ContentMutationResultPayload.TYPE,
                ContentMutationResultPayload.STREAM_CODEC
        );

        registrar.playToServer(
                RequestContentSnapshotPayload.TYPE,
                RequestContentSnapshotPayload.STREAM_CODEC,
                AdminContentHandlers::handleRequestSnapshot
        );

        registrar.playToServer(
                UpsertBossPayload.TYPE,
                UpsertBossPayload.STREAM_CODEC,
                AdminContentHandlers::handleUpsertBoss
        );

        registrar.playToServer(
                UpsertNpcPayload.TYPE,
                UpsertNpcPayload.STREAM_CODEC,
                AdminContentHandlers::handleUpsertNpc
        );

        registrar.playToServer(
                UpsertQuestPayload.TYPE,
                UpsertQuestPayload.STREAM_CODEC,
                AdminContentHandlers::handleUpsertQuest
        );

        registrar.playToServer(
                UpsertCampaignPayload.TYPE,
                UpsertCampaignPayload.STREAM_CODEC,
                AdminContentHandlers::handleUpsertCampaign
        );

        registrar.playToServer(
                CloseDialoguePayload.TYPE,
                CloseDialoguePayload.STREAM_CODEC,
                AdminContentHandlers::handleCloseDialogue
        );
        registrar.playToServer(
                SetCampaignActivePayload.TYPE,
                SetCampaignActivePayload.STREAM_CODEC,
                AdminContentHandlers::handleSetCampaignActive
        );
        registrar.playToServer(
                StartNpcQuestPayload.TYPE,
                StartNpcQuestPayload.STREAM_CODEC,
                AdminContentHandlers::handleStartNpcQuest
        );

        registrar.playToServer(
                DeleteContentPayload.TYPE,
                DeleteContentPayload.STREAM_CODEC,
                AdminContentHandlers::handleDelete
        );
    }
}
