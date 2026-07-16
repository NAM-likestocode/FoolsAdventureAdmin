package com.fool.admin.content;

import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Inventory-safe item counting and removal for ITEM_TO_NPC objectives. */
public final class QuestItemDelivery {
    private QuestItemDelivery() {
    }

    public static boolean tryConsume(Container inventory, Item item, int count) {
        int remaining = count;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                remaining -= stack.getCount();
            }
        }
        if (remaining > 0) {
            return false;
        }

        remaining = count;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
        }
        inventory.setChanged();
        return true;
    }
}
