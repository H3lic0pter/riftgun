package dev.riftgun.crisis;

import dev.riftgun.portal.PortalPlacement;
import java.util.Objects;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Immutable result of expensive crisis preparation; applying it must not perform searches. */
public record PortalCrisisPlan(
//? if >=1.21.11 {
    /*Identifier crisisId,
*///?} else {
    ResourceLocation crisisId,
//?}
    @Nullable Relocation relocation,
    Effect effect,
    int cooldownTicks
) {
    public PortalCrisisPlan {
        Objects.requireNonNull(crisisId, "crisisId");
        if (effect == null) effect = Effect.NONE;
        cooldownTicks = Math.max(0, cooldownTicks);
    }

//? if >=1.21.11 {
    /*public static PortalCrisisPlan effect(Identifier id, Effect effect, int cooldownTicks) {
*///?} else {
    public static PortalCrisisPlan effect(ResourceLocation id, Effect effect, int cooldownTicks) {
//?}
        return new PortalCrisisPlan(id, null, effect, cooldownTicks);
    }

    public record Relocation(Vec3 destination, Vec3 momentum, PortalPlacement exitPlacement) {
        public Relocation {
            Objects.requireNonNull(destination, "destination");
            Objects.requireNonNull(momentum, "momentum");
            Objects.requireNonNull(exitPlacement, "exitPlacement");
        }
    }

    @FunctionalInterface
    public interface Effect {
        Effect NONE = ignored -> {};

        void apply(ServerPlayer player);
    }
}
