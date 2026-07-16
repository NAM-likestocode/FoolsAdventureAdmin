package com.fool.admin.content;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminEntityCatalogTest {
    @Test
    void parsesEntityTypeIds() {
        assertEquals(Identifier.withDefaultNamespace("chicken"), AdminEntityCatalog.parseEntityTypeId("minecraft:chicken"));
        assertEquals(Identifier.withDefaultNamespace("chicken"), AdminEntityCatalog.parseEntityTypeId("chicken"));
        assertEquals(Identifier.withDefaultNamespace("zombie"), AdminEntityCatalog.parseEntityTypeId("  minecraft:zombie  "));
        assertNull(AdminEntityCatalog.parseEntityTypeId(""));
        assertNull(AdminEntityCatalog.parseEntityTypeId("   "));
    }

    @Test
    void defaultEntriesExist() {
        assertTrue(AdminEntityCatalog.defaultBoss().isPresent());
        assertTrue(AdminEntityCatalog.defaultNpc().isPresent());
    }
}
