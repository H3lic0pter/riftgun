package dev.riftgun.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One engine particle type can serve many client-side effect definitions. */
public final class RiftParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, "riftgun");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DYNAMIC =
        TYPES.register("dynamic", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) { TYPES.register(bus); }

    private RiftParticles() {}
}
