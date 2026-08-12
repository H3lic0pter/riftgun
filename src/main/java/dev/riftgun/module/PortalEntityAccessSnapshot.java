package dev.riftgun.module;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.common.Tags;

public record PortalEntityAccessSnapshot(boolean passive, boolean hostile, boolean boss,
                                         boolean projectile) {
    public static final PortalEntityAccessSnapshot NONE = new PortalEntityAccessSnapshot(
        false, false, false, false);

    public PortalEntityAccessSnapshot(boolean passive, boolean hostile, boolean boss) {
        this(passive, hostile, boss, false);
    }

    public boolean allows(Entity entity) {
        if (entity instanceof Projectile) {
            return projectile && !entity.getType().is(PortalEntityTags.PROJECTILE_TRANSIT_EXCLUDED);
        }
        if (!(entity instanceof LivingEntity)) return false;
        if (entity.getType().is(Tags.EntityTypes.BOSSES)) return boss;
        if (entity instanceof Enemy) return hostile;
        return passive;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Passive", passive);
        tag.putBoolean("Hostile", hostile);
        tag.putBoolean("Boss", boss);
        tag.putBoolean("Projectile", projectile);
        return tag;
    }

    public static PortalEntityAccessSnapshot load(CompoundTag tag) {
        if (tag.isEmpty()) return NONE;
        return new PortalEntityAccessSnapshot(
            tag.getBoolean("Passive"), tag.getBoolean("Hostile"), tag.getBoolean("Boss"),
            tag.getBoolean("Projectile"));
    }

    public int mask() {
        return (passive ? 1 : 0) | (hostile ? 2 : 0) | (boss ? 4 : 0) | (projectile ? 8 : 0);
    }
}
