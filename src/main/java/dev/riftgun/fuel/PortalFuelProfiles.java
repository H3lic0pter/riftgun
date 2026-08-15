package dev.riftgun.fuel;

import dev.riftgun.core.RiftConstants;
import dev.riftgun.core.config.RiftConfig;
import dev.riftgun.core.config.RiftConfigs;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class PortalFuelProfiles {
    public static final int UNSTABLE_RGB = 0xA855D4;
    public static final int PORTAL_RGB = 0x58BFFF;
    public static final int DIMENSIONAL_RGB = 0x4FCB72;
    public static final TagKey<Fluid> PORTAL_GUN_FUELS = TagKey.create(
        net.minecraft.core.registries.Registries.FLUID,
        ResourceLocation.fromNamespaceAndPath(RiftConstants.MOD_ID, "portal_gun_fuels")
    );
    private static final List<PortalFuelProfileResolver> RESOLVERS = new CopyOnWriteArrayList<>();
    private static volatile Map<Fluid, PortalFuelProfile> dataProfiles = Map.of();

    static {
        registerResolver(PortalFuelProfiles::resolveBuiltin);
    }

    public static Optional<PortalFuelProfile> resolve(Fluid fluid) {
        PortalFuelProfile dataProfile = dataProfiles.get(fluid);
        if (dataProfile != null) return Optional.of(dataProfile);
        for (PortalFuelProfileResolver resolver : RESOLVERS) {
            Optional<PortalFuelProfile> profile = resolver.resolve(fluid);
            if (profile.isPresent()) return profile;
        }
        return Optional.empty();
    }

    public static void registerResolver(PortalFuelProfileResolver resolver) {
        RESOLVERS.add(resolver);
    }

    static void installDataProfiles(Map<Fluid, PortalFuelProfile> profiles) {
        dataProfiles = Map.copyOf(profiles);
    }

    private static Optional<PortalFuelProfile> resolveBuiltin(Fluid fluid) {
        RiftConfig.FuelConfig config = RiftConfigs.server().fuel();
        if (sameFamily(fluid, PortalFluids.UNSTABLE.get(), PortalFluids.FLOWING_UNSTABLE.get())) {
            return Optional.of(profile("unstable_portal_fluid", UNSTABLE_RGB, false,
                config.unstableMinimum(), config.unstableMaximum()));
        }
        if (sameFamily(fluid, PortalFluids.PORTAL.get(), PortalFluids.FLOWING_PORTAL.get())) {
            return Optional.of(profile("portal_fluid", PORTAL_RGB, false,
                config.portalMinimum(), config.portalMaximum()));
        }
        if (sameFamily(fluid, PortalFluids.DIMENSIONAL.get(), PortalFluids.FLOWING_DIMENSIONAL.get())) {
            return Optional.of(profile("dimensional_portal_fluid", DIMENSIONAL_RGB, true,
                config.dimensionalMinimum(), config.dimensionalMaximum()));
        }
        return Optional.empty();
    }

    public static boolean accepts(Fluid fluid) {
        return resolve(fluid).isPresent();
    }

    public static PortalFuelProfile dimensional() {
        return resolveBuiltin(PortalFluids.DIMENSIONAL.get()).orElseThrow();
    }

    private static PortalFuelProfile profile(String path, int rgb, boolean crossDimension, int min, int max) {
        return new PortalFuelProfile(ResourceLocation.fromNamespaceAndPath(RiftConstants.MOD_ID, path), rgb,
            crossDimension, Math.max(1, min), Math.max(Math.max(1, min), max));
    }

    private static boolean sameFamily(Fluid fluid, Fluid source, Fluid flowing) {
        return fluid == source || fluid == flowing;
    }

    private PortalFuelProfiles() {}
}
