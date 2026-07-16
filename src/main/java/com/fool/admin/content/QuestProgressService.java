package com.fool.admin.content;

import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Server-owned campaign-scoped player quest progression. */
public final class QuestProgressService {
    private QuestProgressService() {
    }

    public static String questKey(String campaignId, String questId) {
        return campaignId + "/" + questId;
    }

    public static PlayerQuestProgressSavedData progressData(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(PlayerQuestProgressSavedData.TYPE);
    }

    public static boolean isCompleted(ServerLevel level, UUID playerId, String campaignId, String questId) {
        return progressData(level).progress(playerId).completedQuestKeys().contains(questKey(campaignId, questId));
    }

    public static void markCompleted(ServerLevel level, UUID playerId, String campaignId, String questId) {
        PlayerQuestProgressSavedData data = progressData(level);
        PlayerQuestProgressSavedData.PlayerProgress progress = data.progress(playerId);
        Set<String> completed = new HashSet<>(progress.completedQuestKeys());
        completed.add(questKey(campaignId, questId));
        data.setProgress(playerId, new PlayerQuestProgressSavedData.PlayerProgress(progress.activeCampaignIds(), completed));
    }

    public static Set<String> completedQuestKeys(ServerLevel level, UUID playerId) {
        return progressData(level).progress(playerId).completedQuestKeys();
    }

    public static Set<String> activeCampaigns(ServerLevel level, UUID playerId) {
        return progressData(level).progress(playerId).activeCampaignIds();
    }

    public static boolean isCampaignAvailable(ServerLevel level, UUID playerId, Campaign campaign, AdminContentSavedData content) {
        return isCampaignAvailable(progressData(level).progress(playerId), campaign, content);
    }

    static boolean isCampaignAvailable(
            PlayerQuestProgressSavedData.PlayerProgress progress,
            Campaign campaign,
            AdminContentSavedData content
    ) {
        for (String prerequisiteCampaignId : campaign.prerequisiteCampaignIds()) {
            Campaign prerequisite = content.campaign(prerequisiteCampaignId).orElse(null);
            if (prerequisite == null || !isCampaignComplete(progress, prerequisite)) {
                return false;
            }
        }
        for (String requiredQuestKey : campaign.unlockAfterQuestKeys()) {
            if (!progress.completedQuestKeys().contains(requiredQuestKey)) {
                return false;
            }
        }
        return true;
    }

    public static boolean setCampaignActive(
            ServerLevel level,
            UUID playerId,
            Campaign campaign,
            AdminContentSavedData content,
            boolean active
    ) {
        if (active && !isCampaignAvailable(level, playerId, campaign, content)) {
            return false;
        }
        PlayerQuestProgressSavedData data = progressData(level);
        PlayerQuestProgressSavedData.PlayerProgress progress = data.progress(playerId);
        Set<String> activeCampaigns = new HashSet<>(progress.activeCampaignIds());
        if (active) {
            activeCampaigns.add(campaign.id());
        } else {
            activeCampaigns.remove(campaign.id());
        }
        data.setProgress(playerId, new PlayerQuestProgressSavedData.PlayerProgress(activeCampaigns, progress.completedQuestKeys()));
        return true;
    }

    public static void resetAll(ServerLevel level, UUID playerId) {
        progressData(level).setProgress(playerId, new PlayerQuestProgressSavedData.PlayerProgress(Set.of(), Set.of()));
    }

    public static void resetCampaign(ServerLevel level, UUID playerId, String campaignId) {
        PlayerQuestProgressSavedData data = progressData(level);
        PlayerQuestProgressSavedData.PlayerProgress progress = data.progress(playerId);
        Set<String> active = new HashSet<>(progress.activeCampaignIds());
        active.remove(campaignId);
        Set<String> completed = new HashSet<>(progress.completedQuestKeys());
        completed.removeIf(key -> key.startsWith(campaignId + "/"));
        data.setProgress(playerId, new PlayerQuestProgressSavedData.PlayerProgress(active, completed));
    }

    public static Optional<AvailableQuest> nextAvailableQuestForNpc(
            ServerLevel level,
            UUID playerId,
            String npcId
    ) {
        return availableQuestsForNpc(level, playerId, npcId).stream().findFirst();
    }

    public static List<AvailableQuest> availableQuestsForNpc(
            ServerLevel level,
            UUID playerId,
            String npcId
    ) {
        AdminContentSavedData content = AdminContentService.get(level);
        Set<String> active = activeCampaigns(level, playerId);
        List<AvailableQuest> available = new ArrayList<>();
        for (Campaign campaign : content.campaigns()) {
            if (!active.contains(campaign.id())) {
                continue;
            }
            for (QuestPoint quest : content.questsForNpc(campaign.id(), npcId)) {
                if (isCompleted(level, playerId, campaign.id(), quest.id()) || !prerequisitesMet(level, playerId, campaign.id(), quest)) {
                    continue;
                }
                available.add(new AvailableQuest(campaign.id(), quest));
            }
        }
        return List.copyOf(available);
    }

    /**
     * Finds quests in currently inactive campaigns that are already unlocked for this player.
     * Callers may activate the campaign only after confirming there is no active NPC quest.
     */
    public static List<AvailableQuest> availableInactiveQuestsForNpc(
            ServerLevel level,
            UUID playerId,
            String npcId
    ) {
        AdminContentSavedData content = AdminContentService.get(level);
        Set<String> active = activeCampaigns(level, playerId);
        List<AvailableQuest> available = new ArrayList<>();
        for (Campaign campaign : content.campaigns()) {
            if (active.contains(campaign.id()) || !isCampaignAvailable(level, playerId, campaign, content)) {
                continue;
            }
            for (QuestPoint quest : content.questsForNpc(campaign.id(), npcId)) {
                if (!isCompleted(level, playerId, campaign.id(), quest.id())
                        && prerequisitesMet(level, playerId, campaign.id(), quest)) {
                    available.add(new AvailableQuest(campaign.id(), quest));
                }
            }
        }
        return List.copyOf(available);
    }

    private static boolean isCampaignComplete(PlayerQuestProgressSavedData.PlayerProgress progress, Campaign campaign) {
        return campaign.questPoints().stream()
                .allMatch(quest -> progress.completedQuestKeys().contains(questKey(campaign.id(), quest.id())));
    }

    private static boolean prerequisitesMet(ServerLevel level, UUID playerId, String campaignId, QuestPoint quest) {
        for (String prerequisiteId : quest.prerequisiteIds()) {
            if (!isCompleted(level, playerId, campaignId, prerequisiteId)) {
                return false;
            }
        }
        return true;
    }

    public record AvailableQuest(String campaignId, QuestPoint quest) {
    }
}
