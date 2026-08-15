package dev.riftgun.crisis;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.core.RiftConstants;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Resolves server-authoritative instability with explicit config overrides. */
public final class PortalFluidInstability {
    public static final TagKey<Fluid> UNSTABLE_FLUIDS = TagKey.create(
        Registries.FLUID, ResourceLocation.fromNamespaceAndPath(RiftConstants.MOD_ID, "unstable_portal_fluids"));

    public static boolean isUnstable(Fluid fluid) {
        if (fluid == Fluids.EMPTY) return false;
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        if (contains(RiftConfigs.server().crises().forceStableFluids(), id)) return false;
        if (contains(RiftConfigs.server().crises().forceUnstableFluids(), id)) return true;
        return fluid.defaultFluidState().is(UNSTABLE_FLUIDS);
    }

    private static boolean contains(List<? extends String> configured, ResourceLocation id) {
        String expected = id.toString();
        return configured.stream().anyMatch(expected::equals);
    }

    private PortalFluidInstability() {}
}
