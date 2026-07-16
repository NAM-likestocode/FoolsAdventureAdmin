package com.fool.admin.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManagedEntityIdentityTest {
    @Test
    void roundTripsTagValue() {
        ManagedEntityIdentity identity = new ManagedEntityIdentity(AdminEntityRole.BOSS, "abc-123");
        assertEquals("boss:abc-123", identity.tagValue());
        assertEquals(AdminContentConstants.MANAGED_TAG_PREFIX + "boss:abc-123", identity.tag());
        ManagedEntityIdentity parsed = ManagedEntityIdentity.fromTag(identity.tag());
        assertEquals(identity, parsed);
    }
}
