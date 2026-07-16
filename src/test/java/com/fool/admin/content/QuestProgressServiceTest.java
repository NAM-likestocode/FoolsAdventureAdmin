package com.fool.admin.content;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestProgressServiceTest {
    private final UUID playerId = UUID.randomUUID();

    @Test
    void playerProgressRoundTripsWithCampaignScopedQuestKeys() {
        PlayerQuestProgressSavedData source = new PlayerQuestProgressSavedData();
        source.setProgress(playerId, new PlayerQuestProgressSavedData.PlayerProgress(
                Set.of("tutorial"),
                Set.of(QuestProgressService.questKey("tutorial", "welcome"))
        ));

        var encoded = PlayerQuestProgressSavedData.CODEC.encodeStart(JsonOps.INSTANCE, source).result().orElseThrow();
        PlayerQuestProgressSavedData decoded = PlayerQuestProgressSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();

        assertEquals(Set.of("tutorial"), decoded.progress(playerId).activeCampaignIds());
        assertEquals(
                Set.of(QuestProgressService.questKey("tutorial", "welcome")),
                decoded.progress(playerId).completedQuestKeys()
        );

        decoded.setProgress(playerId, new PlayerQuestProgressSavedData.PlayerProgress(Set.of(), Set.of()));
        assertEquals(Set.of(), decoded.progress(playerId).activeCampaignIds());
        assertEquals(Set.of(), decoded.progress(playerId).completedQuestKeys());
    }

    @Test
    void campaignRequiresBothCompletedCampaignAndUnlockQuest() {
        QuestPoint introduction = new QuestPoint(
                "introduction", "Introduction", 0, 0, QuestObjectiveType.TALK_TO_NPC,
                "guide", null, null, 1, List.of(), "--> Welcome", 1
        );
        Campaign tutorial = new Campaign("tutorial", "Tutorial", List.of(introduction), 1);
        Campaign chapterTwo = new Campaign(
                "chapter-two", "Chapter Two", List.of(), List.of("tutorial"),
                List.of(QuestProgressService.questKey("tutorial", "introduction")), 1
        );
        AdminContentSavedData content = new AdminContentSavedData();
        content.putCampaign(tutorial);
        content.putCampaign(chapterTwo);

        assertEquals(false, QuestProgressService.isCampaignAvailable(
                new PlayerQuestProgressSavedData.PlayerProgress(Set.of(), Set.of()), chapterTwo, content));
        PlayerQuestProgressSavedData.PlayerProgress completed = new PlayerQuestProgressSavedData.PlayerProgress(
                Set.of(), Set.of(QuestProgressService.questKey("tutorial", "introduction"))
        );
        assertEquals(true, QuestProgressService.isCampaignAvailable(completed, chapterTwo, content));
    }
}
