package com.fool.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminContentSavedData extends SavedData {
    public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("foolsadmin", "admin_content");
    public static final Codec<AdminContentSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BossDefinition.CODEC.listOf().optionalFieldOf("bosses", List.of()).forGetter(data -> List.copyOf(data.bosses.values())),
            NpcDefinition.CODEC.listOf().optionalFieldOf("npcs", List.of()).forGetter(data -> List.copyOf(data.npcs.values())),
            Campaign.CODEC.listOf().optionalFieldOf("campaigns", List.of()).forGetter(data -> List.copyOf(data.campaigns.values()))
    ).apply(instance, AdminContentSavedData::fromLists));

    public static final SavedDataType<AdminContentSavedData> TYPE = new SavedDataType<>(DATA_ID, AdminContentSavedData::new, CODEC);

    private final Map<String, BossDefinition> bosses = new LinkedHashMap<>();
    private final Map<String, NpcDefinition> npcs = new LinkedHashMap<>();
    private final Map<String, Campaign> campaigns = new LinkedHashMap<>();

    public AdminContentSavedData() {
        setDirty();
    }

    private AdminContentSavedData(List<BossDefinition> bossList, List<NpcDefinition> npcList, List<Campaign> campaignList) {
        for (BossDefinition boss : bossList) {
            bosses.put(boss.id(), boss);
        }
        for (NpcDefinition npc : npcList) {
            npcs.put(npc.id(), npc);
        }
        for (Campaign campaign : campaignList) {
            campaigns.put(campaign.id(), campaign);
        }
    }

    private static AdminContentSavedData fromLists(List<BossDefinition> bossList, List<NpcDefinition> npcList, List<Campaign> campaignList) {
        return new AdminContentSavedData(bossList, npcList, campaignList);
    }

    public List<BossDefinition> bosses() {
        return List.copyOf(bosses.values());
    }

    public List<NpcDefinition> npcs() {
        return List.copyOf(npcs.values());
    }

    public List<Campaign> campaigns() {
        return List.copyOf(campaigns.values());
    }

    /**
     * Compatibility view used while campaign-aware callers select their campaign.
     * New authoring and persistence are campaign scoped.
     */
    public List<QuestPoint> quests() {
        return campaigns.values().stream()
                .flatMap(campaign -> campaign.questPoints().stream())
                .toList();
    }

    public Optional<BossDefinition> boss(String id) {
        return Optional.ofNullable(bosses.get(id));
    }

    public Optional<NpcDefinition> npc(String id) {
        return Optional.ofNullable(npcs.get(id));
    }

    public Optional<Campaign> campaign(String id) {
        return Optional.ofNullable(campaigns.get(id));
    }

    public Optional<QuestPoint> quest(String id) {
        return quests().stream().filter(quest -> quest.id().equals(id)).findFirst();
    }

    public void putBoss(BossDefinition boss) {
        bosses.put(boss.id(), boss);
        setDirty();
    }

    public void putNpc(NpcDefinition npc) {
        npcs.put(npc.id(), npc);
        setDirty();
    }

    public void putCampaign(Campaign campaign) {
        campaigns.put(campaign.id(), campaign);
        setDirty();
    }

    public boolean removeBoss(String id) {
        boolean removed = bosses.remove(id) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public boolean removeNpc(String id) {
        boolean removed = npcs.remove(id) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public boolean removeCampaign(String id) {
        boolean removed = campaigns.remove(id) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public List<QuestPoint> questsForNpc(String campaignId, String npcId) {
        Campaign campaign = campaigns.get(campaignId);
        if (campaign == null) {
            return List.of();
        }
        return campaign.questPoints().stream()
                .filter(quest -> npcId.equals(quest.targetNpcId()))
                .toList();
    }

    public Optional<QuestPoint> quest(String campaignId, String questId) {
        Campaign campaign = campaigns.get(campaignId);
        if (campaign == null) {
            return Optional.empty();
        }
        return campaign.questPoints().stream().filter(quest -> quest.id().equals(questId)).findFirst();
    }

    public void putQuest(String campaignId, QuestPoint quest) {
        Campaign campaign = campaigns.get(campaignId);
        if (campaign == null) {
            return;
        }
        List<QuestPoint> points = new java.util.ArrayList<>(campaign.questPoints());
        points.removeIf(existing -> existing.id().equals(quest.id()));
        points.add(quest);
        campaigns.put(campaignId, campaign.withQuestPoints(points));
        setDirty();
    }

    public void putQuest(QuestPoint quest) {
        if (campaigns.isEmpty()) {
            putCampaign(new Campaign("default", "Default Campaign", List.of(), 1));
        }
        putQuest(campaigns.keySet().iterator().next(), quest);
    }

    public boolean removeQuest(String campaignId, String questId) {
        Campaign campaign = campaigns.get(campaignId);
        if (campaign == null) {
            return false;
        }
        List<QuestPoint> points = new java.util.ArrayList<>(campaign.questPoints());
        boolean removed = points.removeIf(quest -> quest.id().equals(questId));
        if (removed) {
            campaigns.put(campaignId, campaign.withQuestPoints(points));
            setDirty();
        }
        return removed;
    }

    public boolean removeQuest(String questId) {
        for (String campaignId : campaigns.keySet()) {
            if (removeQuest(campaignId, questId)) {
                return true;
            }
        }
        return false;
    }

    public List<QuestPoint> allQuestsForNpc(String npcId) {
        List<QuestPoint> result = new java.util.ArrayList<>();
        for (Campaign campaign : campaigns.values()) {
            for (QuestPoint quest : campaign.questPoints()) {
            if (npcId.equals(quest.targetNpcId())) {
                result.add(quest);
            }
        }
        }
        return result;
    }

    public List<QuestPoint> questsForNpc(String npcId) {
        return allQuestsForNpc(npcId);
    }
}
