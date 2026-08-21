package dev.riftgun.mixin;

import dev.riftgun.fuel.PortalFluids;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
abstract class EntityMixin {
    @Redirect(
        method = "doWaterSplashEffect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
        )
    )
    private void riftgun$suppressPortalFluidWaterParticles(
        Level level,
        ParticleOptions options,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed
    ) {
        Entity entity = (Entity) (Object) this;
        if (!isInPortalFluid(entity)) {
            level.addParticle(options, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }

    private static boolean isInPortalFluid(Entity entity) {
        return entity.isInFluidType(PortalFluids.UNSTABLE_TYPE.get())
            || entity.isInFluidType(PortalFluids.PORTAL_TYPE.get())
            || entity.isInFluidType(PortalFluids.DIMENSIONAL_TYPE.get());
    }
}
