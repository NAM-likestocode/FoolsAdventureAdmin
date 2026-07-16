package com.fool.admin.network.payload;

import com.fool.admin.FoolsAdmin;
import com.fool.admin.content.BossDefinition;
import com.fool.admin.content.NpcDefinition;
import com.fool.admin.content.QuestPoint;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public record ContentMutationResultPayload(
        boolean success,
        @Nullable String errorCode,
        @Nullable BossDefinition boss,
        @Nullable NpcDefinition npc,
        @Nullable QuestPoint quest,
        @Nullable String deletedId
) implements CustomPacketPayload {
    public static final Type<ContentMutationResultPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FoolsAdmin.MODID, "content_mutation_result"));

    public static final StreamCodec<ByteBuf, BossDefinition> OPTIONAL_BOSS_CODEC = ByteBufCodecs.optional(ContentPayloadCodecs.BOSS_CODEC)
            .map(optional -> optional.orElse(null), value -> java.util.Optional.ofNullable(value));

    public static final StreamCodec<ByteBuf, NpcDefinition> OPTIONAL_NPC_CODEC = ByteBufCodecs.optional(ContentPayloadCodecs.NPC_CODEC)
            .map(optional -> optional.orElse(null), value -> java.util.Optional.ofNullable(value));

    public static final StreamCodec<ByteBuf, QuestPoint> OPTIONAL_QUEST_CODEC = ByteBufCodecs.optional(ContentPayloadCodecs.QUEST_CODEC)
            .map(optional -> optional.orElse(null), value -> java.util.Optional.ofNullable(value));

    public static final StreamCodec<ByteBuf, ContentMutationResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ContentMutationResultPayload::success,
            ByteBufCodecs.STRING_UTF8, payload -> payload.errorCode() == null ? "" : payload.errorCode(),
            OPTIONAL_BOSS_CODEC, ContentMutationResultPayload::boss,
            OPTIONAL_NPC_CODEC, ContentMutationResultPayload::npc,
            OPTIONAL_QUEST_CODEC, ContentMutationResultPayload::quest,
            ByteBufCodecs.STRING_UTF8, payload -> payload.deletedId() == null ? "" : payload.deletedId(),
            (success, errorCode, boss, npc, quest, deletedId) -> new ContentMutationResultPayload(
                    success,
                    errorCode.isBlank() ? null : errorCode,
                    boss,
                    npc,
                    quest,
                    deletedId.isBlank() ? null : deletedId
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
