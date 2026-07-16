package com.fool.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-owned, persistent progress for every player in the world. */
public final class PlayerQuestProgressSavedData extends SavedData {
    public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("foolsadmin", "player_quest_progress");
    public static final Codec<PlayerQuestProgressSavedData> CODEC = Codec.unboundedMap(Codec.STRING, PlayerProgress.CODEC)
            .xmap(PlayerQuestProgressSavedData::new, data -> Map.copyOf(data.players));
    public static final SavedDataType<PlayerQuestProgressSavedData> TYPE =
            new SavedDataType<>(DATA_ID, PlayerQuestProgressSavedData::new, CODEC);

    private final Map<String, PlayerProgress> players = new LinkedHashMap<>();

    public PlayerQuestProgressSavedData() {
    }

    private PlayerQuestProgressSavedData(Map<String, PlayerProgress> players) {
        this.players.putAll(players);
    }

    public PlayerProgress progress(UUID playerId) {
        return players.getOrDefault(playerId.toString(), PlayerProgress.EMPTY);
    }

    public void setProgress(UUID playerId, PlayerProgress progress) {
        String key = playerId.toString();
        if (progress.isEmpty()) {
            players.remove(key);
        } else {
            players.put(key, progress);
        }
        setDirty();
    }

    public record PlayerProgress(Set<String> activeCampaignIds, Set<String> completedQuestKeys) {
        private static final PlayerProgress EMPTY = new PlayerProgress(Set.of(), Set.of());
        private static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("active_campaigns", java.util.List.of())
                        .forGetter(progress -> progress.activeCampaignIds.stream().toList()),
                Codec.STRING.listOf().optionalFieldOf("completed_quests", java.util.List.of())
                        .forGetter(progress -> progress.completedQuestKeys.stream().toList())
        ).apply(instance, (activeCampaigns, completedQuests) ->
                new PlayerProgress(Set.copyOf(activeCampaigns), Set.copyOf(completedQuests))));

        public PlayerProgress {
            activeCampaignIds = Set.copyOf(activeCampaignIds);
            completedQuestKeys = Set.copyOf(completedQuestKeys);
        }

        private boolean isEmpty() {
            return activeCampaignIds.isEmpty() && completedQuestKeys.isEmpty();
        }
    }
}
