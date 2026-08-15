package dev.riftgun.entity;

import dev.riftgun.module.PortalEntityTags;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/** Atomically publishes datapack tag decisions for one running server. */
public final class SpecialEntityTransitPolicies {
    private static final SpecialEntityTransitPolicy<EntityType<?>> EMPTY =
        SpecialEntityTransitPolicy.compile(emptyRules(), emptyRules(), Set.of());
    private static volatile SpecialEntityTransitPolicy<EntityType<?>> current = EMPTY;

    public static SpecialEntityTransitPolicy<EntityType<?>> current() {
        return current;
    }

    public static void rebuild(RegistryAccess access) {
//? if >=1.21.11 {
        /*Registry<EntityType<?>> entities = access.lookupOrThrow(Registries.ENTITY_TYPE);
*///?} else {
        Registry<EntityType<?>> entities = access.registryOrThrow(Registries.ENTITY_TYPE);
//?}
        current = SpecialEntityTransitPolicy.compile(
            new SpecialEntityTransitPolicy.AccessRules<>(
                values(entities, PortalEntityTags.PORTAL_TRANSIT_ALLOWED),
                values(entities, PortalEntityTags.PORTAL_TRANSIT_DENIED)),
            new SpecialEntityTransitPolicy.AccessRules<>(
                values(entities, PortalEntityTags.ENTITY_RELOCATION_ALLOWED),
                values(entities, PortalEntityTags.ENTITY_RELOCATION_DENIED)),
            values(entities, PortalEntityTags.PORTAL_TRANSIT_SWEPT));
    }

    public static void reset() {
        current = EMPTY;
    }

    private static Set<EntityType<?>> values(Registry<EntityType<?>> registry,
                                              TagKey<EntityType<?>> tag) {
//? if >=1.21.11 {
        /*return java.util.stream.StreamSupport.stream(registry.getTagOrEmpty(tag).spliterator(), false)
            .map(Holder::value)
            .collect(Collectors.toUnmodifiableSet());
        *///?} else {
        return registry.getTag(tag).stream()
            .flatMap(named -> named.stream().map(Holder::value))
            .collect(Collectors.toUnmodifiableSet());
        //?}
    }

    private static SpecialEntityTransitPolicy.AccessRules<EntityType<?>> emptyRules() {
        return new SpecialEntityTransitPolicy.AccessRules<>(Set.of(), Set.of());
    }

    private SpecialEntityTransitPolicies() {}
}
