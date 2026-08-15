package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.world.level.Level;
//?}
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalExitTargetTest {
    //? if >=1.21.11 {
    /*private static final ResourceKey<net.minecraft.world.level.Level> NETHER = ResourceKey.create(
        Registries.DIMENSION, Identifier.withDefaultNamespace("the_nether"));
    *///?} else {
    private static final ResourceKey<Level> NETHER = Level.NETHER;
    //?}

    @Test
    void deferredTargetSurvivesEntityNbtRoundTrip() {
        PortalExitTarget original = new PortalExitTarget(
            UUID.randomUUID(), NETHER, new Vec3(12.25, 70.0, -31.75), 135.0F);

        assertEquals(original, PortalExitTarget.load(original.save()));
    }
}
