package dev.riftgun.module;

import dev.riftgun.core.RiftConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class PortalEntityTags {
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath("c", "bosses"));
    public static final TagKey<EntityType<?>> PROJECTILE_TRANSIT_EXCLUDED = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath(RiftConstants.MOD_ID, "projectile_transit_excluded"));
    public static final TagKey<EntityType<?>> PORTAL_TRANSIT_ALLOWED = riftgun("portal_transit_allowed");
    public static final TagKey<EntityType<?>> PORTAL_TRANSIT_DENIED = riftgun("portal_transit_denied");
    public static final TagKey<EntityType<?>> PORTAL_TRANSIT_SWEPT = riftgun("portal_transit_swept");
    public static final TagKey<EntityType<?>> ENTITY_RELOCATION_ALLOWED = riftgun("entity_relocation_allowed");
    public static final TagKey<EntityType<?>> ENTITY_RELOCATION_DENIED = riftgun("entity_relocation_denied");

    private static TagKey<EntityType<?>> riftgun(String path) {
        return TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(RiftConstants.MOD_ID, path));
    }

    private PortalEntityTags() {}
}
