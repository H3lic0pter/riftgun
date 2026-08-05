package dev.riftgun.service;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface PortalMotionPredictor {
    Vec3 predictDisplacement(ServerPlayer player, int ticks, double maximumHorizontalDisplacement);
}
