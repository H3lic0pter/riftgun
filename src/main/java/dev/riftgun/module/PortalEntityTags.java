package dev.riftgun.module;

import dev.riftgun.RiftGun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class PortalEntityTags {
    public static final TagKey<EntityType<?>> PROJECTILE_TRANSIT_EXCLUDED = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, "projectile_transit_excluded"));

    private PortalEntityTags() {}
}
