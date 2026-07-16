package com.fool.adventure.admin.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneMaskTest {
    @Test
    void paintAndContainsBlock() {
        ZoneMask mask = new ZoneMask();
        mask.paintDisc(10, 20, 1, false);
        assertTrue(mask.contains(10, 20));
        assertTrue(mask.contains(11, 20));
        assertFalse(mask.contains(30, 30));
    }

    @Test
    void eraseRemovesPaintedBlocks() {
        ZoneMask mask = new ZoneMask();
        mask.paintDisc(0, 0, 2, false);
        assertTrue(mask.contains(0, 0));
        mask.paintDisc(0, 0, 1, true);
        assertFalse(mask.contains(0, 0));
    }

    @Test
    void paintedBlockCountTracksBits() {
        ZoneMask mask = new ZoneMask();
        mask.setBlock(1, 1, true);
        mask.setBlock(2, 2, true);
        assertEquals(2, mask.paintedBlockCount());
    }

    @Test
    void chunkBoundariesWork() {
        ZoneMask mask = new ZoneMask();
        mask.setBlock(15, 15, true);
        mask.setBlock(16, 16, true);
        assertTrue(mask.contains(15, 15));
        assertTrue(mask.contains(16, 16));
    }
}
