package dev.riftgun.portal;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.core.RiftConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.projectile.Projectile;

/** Persistent, per-projectile transit budget shared by normal and relocation portals. */
public final class PortalProjectileState {
    private static final String ROOT = RiftConstants.MOD_ID + ":projectile_transit";
    private static final String COUNT = "Count";

    public static boolean canTransit(Projectile projectile) {
        return count(projectile) < RiftConfigs.server().projectile().maximumTransits();
    }

    public static int count(Projectile projectile) {
        return projectile.getPersistentData().getCompound(ROOT).getInt(COUNT);
    }

    public static void recordSuccessfulTransit(Projectile projectile) {
        CompoundTag root = projectile.getPersistentData().getCompound(ROOT);
        root.putInt(COUNT, Math.min(Integer.MAX_VALUE, Nbt.getInt(root, COUNT) + 1));
        projectile.getPersistentData().put(ROOT, root);
    }

    private PortalProjectileState() {}
}
