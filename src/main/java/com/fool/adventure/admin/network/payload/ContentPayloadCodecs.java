package com.fool.adventure.admin.network.payload;

import com.fool.adventure.admin.content.AdminContentConstants;
import com.fool.adventure.admin.content.BossDefinition;
import com.fool.adventure.admin.content.DialogueDefinition;
import com.fool.adventure.admin.content.DialogueLine;
import com.fool.adventure.admin.content.NpcDefinition;
import com.fool.adventure.admin.content.Waypoint;
import com.fool.adventure.admin.content.ZoneMask;
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

    public static final StreamCodec<ByteBuf, DialogueLine> DIALOGUE_LINE_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DialogueLine::text,
            ByteBufCodecs.VAR_INT, DialogueLine::delayTicks,
            DialogueLine::new
    );

    public static final StreamCodec<ByteBuf, DialogueDefinition> DIALOGUE_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DialogueDefinition::id,
            ByteBufCodecs.STRING_UTF8, DialogueDefinition::name,
            DIALOGUE_LINE_CODEC.apply(ByteBufCodecs.list(AdminContentConstants.MAX_DIALOGUE_LINES)), DialogueDefinition::lines,
            ByteBufCodecs.VAR_INT, DialogueDefinition::revision,
            DialogueDefinition::new
    );

    public static final StreamCodec<ByteBuf, UUID> OPTIONAL_UUID_CODEC = ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC)
            .map(optional -> optional.orElse(null), uuid -> Optional.ofNullable(uuid));

    public static final StreamCodec<ByteBuf, String> OPTIONAL_STRING_CODEC = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8)
            .map(optional -> optional.orElse(null), value -> Optional.ofNullable(value));

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
            OPTIONAL_STRING_CODEC, NpcDefinition::dialogueId,
            OPTIONAL_UUID_CODEC, NpcDefinition::boundEntityUuid,
            ByteBufCodecs.VAR_INT, NpcDefinition::revision,
            NpcDefinition::new
    );

    private ContentPayloadCodecs() {
    }
}
