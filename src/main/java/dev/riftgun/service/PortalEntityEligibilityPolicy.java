package dev.riftgun.service;

import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface PortalEntityEligibilityPolicy {
    boolean allows(Entity entity);

    default boolean allowsTree(Entity root) {
        if (!allows(root)) return false;
        for (Entity passenger : root.getPassengers()) {
            if (!allowsTree(passenger)) return false;
        }
        return true;
    }
}

