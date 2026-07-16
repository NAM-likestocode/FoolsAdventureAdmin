package com.fool.admin.content;

import net.minecraft.world.entity.Entity;

import org.jspecify.annotations.Nullable;

public record ManagedEntityIdentity(AdminEntityRole role, String definitionId) {
    public String tagValue() {
        return role.name().toLowerCase() + ":" + definitionId;
    }

    public String tag() {
        return AdminContentConstants.MANAGED_TAG_PREFIX + tagValue();
    }

    public static @Nullable ManagedEntityIdentity fromTag(String tag) {
        if (!tag.startsWith(AdminContentConstants.MANAGED_TAG_PREFIX)) {
            return null;
        }
        String payload = tag.substring(AdminContentConstants.MANAGED_TAG_PREFIX.length());
        int separator = payload.indexOf(':');
        if (separator <= 0) {
            return null;
        }
        String roleName = payload.substring(0, separator);
        String definitionId = payload.substring(separator + 1);
        return new ManagedEntityIdentity(AdminEntityRole.valueOf(roleName.toUpperCase()), definitionId);
    }

    public static @Nullable ManagedEntityIdentity read(Entity entity) {
        for (String tag : entity.entityTags()) {
            ManagedEntityIdentity identity = fromTag(tag);
            if (identity != null) {
                return identity;
            }
        }
        return null;
    }

    public static void apply(Entity entity, ManagedEntityIdentity identity) {
        entity.entityTags().removeIf(tag -> tag.startsWith(AdminContentConstants.MANAGED_TAG_PREFIX));
        entity.addTag(identity.tag());
    }
}
