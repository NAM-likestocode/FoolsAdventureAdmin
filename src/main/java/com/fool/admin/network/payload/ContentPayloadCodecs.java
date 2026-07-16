package com.fool.admin.network.payload;

import com.fool.admin.content.AdminContentConstants;
import com.fool.admin.content.BossDefinition;
import com.fool.admin.content.Campaign;
import com.fool.admin.content.DialogueChoice;
import com.fool.admin.content.DialogueNode;
import com.fool.admin.content.DialogueScript;
import com.fool.admin.content.DialogueSpeaker;
import com.fool.admin.content.NpcDefinition;
import com.fool.admin.content.QuestObjectiveType;
import com.fool.admin.content.QuestPoint;
import com.fool.admin.content.Waypoint;
import com.fool.admin.content.ZoneMask;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

public final class ContentPayloadCodecs {
    public static final StreamCodec<ByteBuf, ZoneMask.ChunkMask> CHUNK_MASK_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ZoneMask.ChunkMask::chunkX,
            ByteBufCodecs.VAR_INT, ZoneMask.ChunkMask::chunkZ,
            ByteBufCodecs.VAR_LONG, mask -> mask.words()[0],
            ByteBufCodecs.VAR_LONG, mask -> mask.words()[1],
            ByteBufCodecs.VAR_LONG, mask -> mask.words()[2],
            ByteBufCodecs.VAR_LONG, mask -> mask.words()[3],
            (chunkX, chunkZ, w0, w1, w2, w3) -> new ZoneMask.ChunkMask(chunkX, chunkZ, new long[]{w0, w1, w2, w3})
    );

    public static final StreamCodec<ByteBuf, ZoneMask> ZONE_MASK_CODEC = CHUNK_MASK_CODEC.apply(
            ByteBufCodecs.list(4096)
    ).map(ZoneMask::fromChunks, ZoneMask::toChunks);

    public static final StreamCodec<ByteBuf, Waypoint> WAYPOINT_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Waypoint::x,
            ByteBufCodecs.VAR_INT, Waypoint::y,
            ByteBufCodecs.VAR_INT, Waypoint::z,
            ByteBufCodecs.VAR_INT, Waypoint::dwellTicks,
            Waypoint::new
    );

    public static final StreamCodec<ByteBuf, UUID> OPTIONAL_UUID_CODEC = ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC)
            .map(optional -> optional.orElse(null), uuid -> Optional.ofNullable(uuid));

    public static final StreamCodec<ByteBuf, String> OPTIONAL_STRING_CODEC = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8)
            .map(optional -> optional.orElse(null), value -> Optional.ofNullable(value));

    public static final StreamCodec<ByteBuf, DialogueChoice> DIALOGUE_CHOICE_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DialogueChoice::label,
            OPTIONAL_STRING_CODEC, DialogueChoice::targetNodeId,
            DialogueChoice::new
    );

    public static final StreamCodec<ByteBuf, DialogueNode> DIALOGUE_NODE_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DialogueNode::id,
            ByteBufCodecs.STRING_UTF8, node -> node.speaker().name(),
            ByteBufCodecs.STRING_UTF8, DialogueNode::text,
            ByteBufCodecs.VAR_INT, DialogueNode::delayTicks,
            OPTIONAL_STRING_CODEC, DialogueNode::nextNodeId,
            DIALOGUE_CHOICE_CODEC.apply(ByteBufCodecs.list(AdminContentConstants.MAX_DIALOGUE_CHOICES)), DialogueNode::choices,
            (id, speakerName, text, delayTicks, nextNodeId, choices) -> new DialogueNode(
                    id,
                    DialogueSpeaker.valueOf(speakerName),
                    text,
                    delayTicks,
                    nextNodeId,
                    choices
            )
    );

    public static final StreamCodec<ByteBuf, DialogueScript> DIALOGUE_SCRIPT_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DialogueScript::entryNodeId,
            DIALOGUE_NODE_CODEC.apply(ByteBufCodecs.list(AdminContentConstants.MAX_DIALOGUE_NODES)), DialogueScript::nodes,
            DialogueScript::new
    );

    public static final StreamCodec<ByteBuf, BossDefinition> BOSS_CODEC = StreamCodec.of(
            ContentPayloadCodecs::encodeBoss,
            ContentPayloadCodecs::decodeBoss
    );

    private static void encodeBoss(ByteBuf buffer, BossDefinition boss) {
        ByteBufCodecs.STRING_UTF8.encode(buffer, boss.id());
        ByteBufCodecs.STRING_UTF8.encode(buffer, boss.displayName());
        PayloadCodecs.IDENTIFIER.encode(buffer, boss.entityTypeId());
        ByteBufCodecs.VAR_INT.encode(buffer, boss.spawnX());
        ByteBufCodecs.VAR_INT.encode(buffer, boss.spawnY());
        ByteBufCodecs.VAR_INT.encode(buffer, boss.spawnZ());
        ZONE_MASK_CODEC.encode(buffer, boss.zone());
        ByteBufCodecs.BOOL.encode(buffer, boss.hasAttractionPoint());
        ByteBufCodecs.VAR_INT.encode(buffer, boss.attractionX());
        ByteBufCodecs.VAR_INT.encode(buffer, boss.attractionY());
        ByteBufCodecs.VAR_INT.encode(buffer, boss.attractionZ());
        OPTIONAL_UUID_CODEC.encode(buffer, boss.boundEntityUuid());
        ByteBufCodecs.VAR_INT.encode(buffer, boss.revision());
    }

    private static BossDefinition decodeBoss(ByteBuf buffer) {
        return new BossDefinition(
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                ByteBufCodecs.STRING_UTF8.decode(buffer),
                PayloadCodecs.IDENTIFIER.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ZONE_MASK_CODEC.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer),
                OPTIONAL_UUID_CODEC.decode(buffer),
                ByteBufCodecs.VAR_INT.decode(buffer)
        );
    }

    public static final StreamCodec<ByteBuf, NpcDefinition> NPC_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, NpcDefinition::id,
            ByteBufCodecs.STRING_UTF8, NpcDefinition::displayName,
            PayloadCodecs.IDENTIFIER, NpcDefinition::entityTypeId,
            ByteBufCodecs.VAR_INT, NpcDefinition::spawnX,
            ByteBufCodecs.VAR_INT, NpcDefinition::spawnY,
            ByteBufCodecs.VAR_INT, NpcDefinition::spawnZ,
            WAYPOINT_CODEC.apply(ByteBufCodecs.list(AdminContentConstants.MAX_WAYPOINTS)), NpcDefinition::waypoints,
            ByteBufCodecs.BOOL, NpcDefinition::repeatPath,
            ByteBufCodecs.BOOL, NpcDefinition::stationary,
            OPTIONAL_UUID_CODEC, NpcDefinition::boundEntityUuid,
            ByteBufCodecs.VAR_INT, NpcDefinition::revision,
            NpcDefinition::new
    );

    public static final StreamCodec<ByteBuf, QuestPoint> QUEST_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, QuestPoint::id,
            ByteBufCodecs.STRING_UTF8, QuestPoint::name,
            ByteBufCodecs.FLOAT, QuestPoint::canvasX,
            ByteBufCodecs.FLOAT, QuestPoint::canvasY,
            ByteBufCodecs.STRING_UTF8, quest -> quest.objectiveType().name(),
            OPTIONAL_STRING_CODEC, QuestPoint::targetNpcId,
            OPTIONAL_STRING_CODEC, QuestPoint::targetBossId,
            ByteBufCodecs.optional(PayloadCodecs.IDENTIFIER).map(optional -> optional.orElse(null), value -> Optional.ofNullable(value)),
            QuestPoint::requiredItem,
            ByteBufCodecs.VAR_INT, QuestPoint::requiredCount,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(AdminContentConstants.MAX_QUEST_PREREQUISITES)),
            QuestPoint::prerequisiteIds,
            ByteBufCodecs.STRING_UTF8, QuestPoint::dialogueScript,
            ByteBufCodecs.VAR_INT, QuestPoint::revision,
            (id, name, canvasX, canvasY, objectiveTypeName, targetNpcId, targetBossId, requiredItem, requiredCount, prerequisiteIds, dialogueScript, revision) -> new QuestPoint(
                    id,
                    name,
                    canvasX,
                    canvasY,
                    QuestObjectiveType.valueOf(objectiveTypeName),
                    targetNpcId,
                    targetBossId,
                    requiredItem,
                    requiredCount,
                    prerequisiteIds,
                    dialogueScript,
                    revision
            )
    );

    public static final StreamCodec<ByteBuf, Campaign> CAMPAIGN_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Campaign::id,
            ByteBufCodecs.STRING_UTF8, Campaign::name,
            QUEST_CODEC.apply(ByteBufCodecs.list(AdminContentConstants.MAX_QUEST_POINTS)), Campaign::questPoints,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64)), Campaign::prerequisiteCampaignIds,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)), Campaign::unlockAfterQuestKeys,
            ByteBufCodecs.VAR_INT, Campaign::revision,
            Campaign::new
    );

    private ContentPayloadCodecs() {
    }
}
