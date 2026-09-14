package dev.riftgun.portal;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

/** Minimal render contract shared by interactive portals and visual-only relocation gates. */
public interface PortalVisualSource {
    default PortalLifecycle.Phase phase() { return PortalLifecycle.Phase.OPEN; }
    default int phaseTicks() { return 0; }
    default int openingTicks() { return PortalLifecycle.ANIMATION_TICKS; }
    default String visualType() { return dev.riftgun.appearance.GunPresentation.DEFAULT_VISUAL; }
    UUID visualId();
    PortalOrientation orientation();
    PortalGeometry geometry();
    float portalWidth();
    float portalHeight();
    Vec3 normal();
    Vec3 up();
    Vec3 right();
    PortalPlacement placement();
    int fuelRgb();
    float visualProgress(float partialTick);
    float visualAge(float partialTick);
}
