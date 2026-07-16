package com.fool.admin.network;

import com.fool.admin.AdminContentHandlers;
import com.fool.admin.AdminServerHandlers;
import com.fool.admin.network.payload.DeleteContentPayload;
import com.fool.admin.network.payload.MapTilesResponsePayload;
import com.fool.admin.network.payload.OpenAdminScreenPayload;
import com.fool.admin.network.payload.RequestContentSnapshotPayload;
import com.fool.admin.network.payload.RequestMapTilesPayload;
import com.fool.admin.network.payload.UpsertBossPayload;
import com.fool.admin.network.payload.UpsertDialoguePayload;
import com.fool.admin.network.payload.UpsertNpcPayload;
import com.fool.admin.network.payload.ContentMutationResultPayload;
import com.fool.admin.network.payload.ContentSnapshotPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AdminNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private AdminNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                OpenAdminScreenPayload.TYPE,
                OpenAdminScreenPayload.STREAM_CODEC
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
                UpsertDialoguePayload.TYPE,
                UpsertDialoguePayload.STREAM_CODEC,
                AdminContentHandlers::handleUpsertDialogue
        );

        registrar.playToServer(
                DeleteContentPayload.TYPE,
                DeleteContentPayload.STREAM_CODEC,
                AdminContentHandlers::handleDelete
        );
    }
}
