package dev.riftgun.client.render;

import dev.riftgun.portal.PortalEntity;
import net.minecraft.world.phys.Vec3;

record PortalRenderBasis(Vec3 right, Vec3 up, Vec3 normal) {
    static PortalRenderBasis from(PortalEntity portal) {
        return new PortalRenderBasis(portal.right(), portal.up(), portal.normal());
    }

    Vec3 at(float x, float y, float z) {
        return right.scale(x).add(up.scale(y)).add(normal.scale(z));
    }
}
