package dev.riftgun.module;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.common.Tags;

public record PortalEntityAccessSnapshot(boolean passive, boolean hostile, boolean boss) {
    public static final PortalEntityAccessSnapshot NONE = new PortalEntityAccessSnapshot(false, false, false);

    public boolean allows(Entity entity) {
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
        return tag;
    }

    public static PortalEntityAccessSnapshot load(CompoundTag tag) {
        if (tag.isEmpty()) return NONE;
        return new PortalEntityAccessSnapshot(
            tag.getBoolean("Passive"), tag.getBoolean("Hostile"), tag.getBoolean("Boss"));
    }

    public int mask() {
        return (passive ? 1 : 0) | (hostile ? 2 : 0) | (boss ? 4 : 0);
    }
}
