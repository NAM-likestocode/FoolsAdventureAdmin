package com.fool.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

public record Campaign(
        String id,
        String name,
        List<QuestPoint> questPoints,
        List<String> prerequisiteCampaignIds,
        List<String> unlockAfterQuestKeys,
        int revision
) {
    public static final Codec<Campaign> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Campaign::id),
            Codec.STRING.fieldOf("name").forGetter(Campaign::name),
            QuestPoint.CODEC.listOf().optionalFieldOf("quest_points", List.of()).forGetter(Campaign::questPoints),
            Codec.STRING.listOf().optionalFieldOf("prerequisite_campaign_ids", List.of()).forGetter(Campaign::prerequisiteCampaignIds),
            Codec.STRING.listOf().optionalFieldOf("unlock_after_quest_keys", List.of()).forGetter(Campaign::unlockAfterQuestKeys),
            Codec.INT.optionalFieldOf("revision", 0).forGetter(Campaign::revision)
    ).apply(instance, Campaign::new));

    public Campaign {
        questPoints = List.copyOf(questPoints);
        prerequisiteCampaignIds = List.copyOf(prerequisiteCampaignIds);
        unlockAfterQuestKeys = List.copyOf(unlockAfterQuestKeys);
    }

    public Campaign(String id, String name, List<QuestPoint> questPoints, int revision) {
        this(id, name, questPoints, List.of(), List.of(), revision);
    }

    public Campaign withName(String value) {
        return new Campaign(id, value, questPoints, prerequisiteCampaignIds, unlockAfterQuestKeys, revision);
    }

    public Campaign withQuestPoints(List<QuestPoint> value) {
        return new Campaign(id, name, value, prerequisiteCampaignIds, unlockAfterQuestKeys, revision);
    }

    public Campaign withPrerequisiteCampaignIds(List<String> value) {
        return new Campaign(id, name, questPoints, value, unlockAfterQuestKeys, revision);
    }

    public Campaign withUnlockAfterQuestKeys(List<String> value) {
        return new Campaign(id, name, questPoints, prerequisiteCampaignIds, value, revision);
    }

    public Campaign withRevision(int value) {
        return new Campaign(id, name, questPoints, prerequisiteCampaignIds, unlockAfterQuestKeys, value);
    }

    public Campaign copyForEdit() {
        return new Campaign(
                id,
                name,
                new ArrayList<>(questPoints),
                new ArrayList<>(prerequisiteCampaignIds),
                new ArrayList<>(unlockAfterQuestKeys),
                revision
        );
    }
}
