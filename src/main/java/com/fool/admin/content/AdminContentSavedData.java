package com.fool.admin.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdminContentSavedData extends SavedData {
    public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("foolsadmin", "admin_content");
    public static final Codec<AdminContentSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BossDefinition.CODEC.listOf().optionalFieldOf("bosses", List.of()).forGetter(data -> List.copyOf(data.bosses.values())),
            NpcDefinition.CODEC.listOf().optionalFieldOf("npcs", List.of()).forGetter(data -> List.copyOf(data.npcs.values())),
            DialogueDefinition.CODEC.listOf().optionalFieldOf("dialogues", List.of()).forGetter(data -> List.copyOf(data.dialogues.values()))
    ).apply(instance, AdminContentSavedData::fromLists));

    public static final SavedDataType<AdminContentSavedData> TYPE = new SavedDataType<>(DATA_ID, AdminContentSavedData::new, CODEC);

    private final Map<String, BossDefinition> bosses = new LinkedHashMap<>();
    private final Map<String, NpcDefinition> npcs = new LinkedHashMap<>();
    private final Map<String, DialogueDefinition> dialogues = new LinkedHashMap<>();

    public AdminContentSavedData() {
        setDirty();
    }

    private AdminContentSavedData(List<BossDefinition> bossList, List<NpcDefinition> npcList, List<DialogueDefinition> dialogueList) {
        for (BossDefinition boss : bossList) {
            bosses.put(boss.id(), boss);
        }
        for (NpcDefinition npc : npcList) {
            npcs.put(npc.id(), npc);
        }
        for (DialogueDefinition dialogue : dialogueList) {
            dialogues.put(dialogue.id(), dialogue);
        }
    }

    private static AdminContentSavedData fromLists(List<BossDefinition> bossList, List<NpcDefinition> npcList, List<DialogueDefinition> dialogueList) {
        return new AdminContentSavedData(bossList, npcList, dialogueList);
    }

    public List<BossDefinition> bosses() {
        return List.copyOf(bosses.values());
    }

    public List<NpcDefinition> npcs() {
        return List.copyOf(npcs.values());
    }

    public List<DialogueDefinition> dialogues() {
        return List.copyOf(dialogues.values());
    }

    public Optional<BossDefinition> boss(String id) {
        return Optional.ofNullable(bosses.get(id));
    }

    public Optional<NpcDefinition> npc(String id) {
        return Optional.ofNullable(npcs.get(id));
    }

    public Optional<DialogueDefinition> dialogue(String id) {
        return Optional.ofNullable(dialogues.get(id));
    }

    public void putBoss(BossDefinition boss) {
        bosses.put(boss.id(), boss);
        setDirty();
    }

    public void putNpc(NpcDefinition npc) {
        npcs.put(npc.id(), npc);
        setDirty();
    }

    public void putDialogue(DialogueDefinition dialogue) {
        dialogues.put(dialogue.id(), dialogue);
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

    public boolean removeDialogue(String id) {
        boolean removed = dialogues.remove(id) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public List<NpcDefinition> npcsWithDialogue(String dialogueId) {
        List<NpcDefinition> result = new ArrayList<>();
        for (NpcDefinition npc : npcs.values()) {
            if (dialogueId.equals(npc.dialogueId())) {
                result.add(npc);
            }
        }
        return result;
    }
}
