package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalTransitServiceTest {
    @Test
    void mountedNodesReuseTheRootTransitPlan() {
        PortalTransitService.TransitPlan rootPlan = new PortalTransitService.TransitPlan(
            new Vec3(4.0, 5.0, 6.0), new Vec3(0.5, 0.0, 1.5), 90.0F, 10.0F);

        PortalTransitService.TransitPlan mounted = PortalTransitService.mountedPlan(
            rootPlan, 25.0F, -30.0F);

        assertEquals(rootPlan.destination(), mounted.destination());
        assertEquals(rootPlan.momentum(), mounted.momentum());
        assertEquals(25.0F, mounted.yaw());
        assertEquals(-30.0F, mounted.pitch());
    }
}
