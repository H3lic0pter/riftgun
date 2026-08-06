package dev.riftgun.service;

import net.minecraft.world.entity.Entity;
import java.util.function.Predicate;

@FunctionalInterface
public interface PortalEntityEligibilityPolicy {
    boolean allows(Entity entity);

    default boolean allowsTree(Entity root) {
        return allowsTree(root, ignored -> false);
    }

    default boolean allowsTree(Entity root, Predicate<Entity> additionalRule) {
        if (!allows(root) && !additionalRule.test(root)) return false;
        for (Entity passenger : root.getPassengers()) {
            if (!allowsTree(passenger, additionalRule)) return false;
        }
        return true;
    }
}
