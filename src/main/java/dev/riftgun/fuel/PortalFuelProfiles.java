package dev.riftgun.fuel;

import dev.riftgun.RiftGun;
import dev.riftgun.config.ServerConfig;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public final class PortalFuelProfiles {
    public static final int UNSTABLE_RGB = 0xA855D4;
    public static final int PORTAL_RGB = 0x58BFFF;
    public static final int DIMENSIONAL_RGB = 0x4FCB72;
    public static final TagKey<Fluid> PORTAL_GUN_FUELS = TagKey.create(
        net.minecraft.core.registries.Registries.FLUID,
        ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, "portal_gun_fuels")
    );
    private static final List<PortalFuelProfileResolver> RESOLVERS = new CopyOnWriteArrayList<>();

    static {
        registerResolver(PortalFuelProfiles::resolveBuiltin);
    }

    public static Optional<PortalFuelProfile> resolve(Fluid fluid) {
        for (PortalFuelProfileResolver resolver : RESOLVERS) {
            Optional<PortalFuelProfile> profile = resolver.resolve(fluid);
            if (profile.isPresent()) return profile;
        }
        return Optional.empty();
    }

    public static void registerResolver(PortalFuelProfileResolver resolver) {
        RESOLVERS.add(resolver);
    }

    private static Optional<PortalFuelProfile> resolveBuiltin(Fluid fluid) {
        ServerConfig.Values config = ServerConfig.VALUES;
        if (sameFamily(fluid, PortalFluids.UNSTABLE.get(), PortalFluids.FLOWING_UNSTABLE.get())) {
            return Optional.of(profile("unstable_portal_fluid", UNSTABLE_RGB, false,
                config.unstableFuelMin.get(), config.unstableFuelMax.get()));
        }
        if (sameFamily(fluid, PortalFluids.PORTAL.get(), PortalFluids.FLOWING_PORTAL.get())) {
            return Optional.of(profile("portal_fluid", PORTAL_RGB, false,
                config.portalFuelMin.get(), config.portalFuelMax.get()));
        }
        if (sameFamily(fluid, PortalFluids.DIMENSIONAL.get(), PortalFluids.FLOWING_DIMENSIONAL.get())) {
            return Optional.of(profile("dimensional_portal_fluid", DIMENSIONAL_RGB, true,
                config.dimensionalFuelMin.get(), config.dimensionalFuelMax.get()));
        }
        return Optional.empty();
    }

    public static Optional<PortalFuelProfile> resolve(FluidStack stack) {
        if (stack.isEmpty() || !stack.is(PORTAL_GUN_FUELS)) return Optional.empty();
        return resolve(stack.getFluid());
    }

    public static boolean accepts(FluidStack stack) {
        return resolve(stack).isPresent();
    }

    private static PortalFuelProfile profile(String path, int rgb, boolean crossDimension, int min, int max) {
        return new PortalFuelProfile(ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, path), rgb,
            crossDimension, Math.max(1, min), Math.max(Math.max(1, min), max));
    }

    private static boolean sameFamily(Fluid fluid, Fluid source, Fluid flowing) {
        return fluid == source || fluid == flowing;
    }

    private PortalFuelProfiles() {}
}
