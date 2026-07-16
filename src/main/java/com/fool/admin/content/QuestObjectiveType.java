package com.fool.admin.content;

import com.mojang.serialization.Codec;

public enum QuestObjectiveType {
    TALK_TO_NPC,
    ITEM_TO_NPC,
    KILL_BOSS,
    CLEAR_DUNGEON;

    public static final Codec<QuestObjectiveType> CODEC = Codec.STRING.xmap(
            QuestObjectiveType::valueOf,
            QuestObjectiveType::name
    );

    public boolean isEnabledInEditor() {
        return this != CLEAR_DUNGEON;
    }
}
