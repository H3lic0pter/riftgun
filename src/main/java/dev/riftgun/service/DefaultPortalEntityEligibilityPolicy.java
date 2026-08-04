package dev.riftgun.service;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;

public final class DefaultPortalEntityEligibilityPolicy implements PortalEntityEligibilityPolicy {
    @Override
    public boolean allows(Entity entity) {
        return entity instanceof Player || entity instanceof ItemEntity || entity instanceof VehicleEntity;
    }
}

