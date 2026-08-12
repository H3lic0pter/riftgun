package dev.riftgun.relocation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.common.Tags;

/** Classifies a relocation target once and snapshots its configurable fuel multiplier. */
final class EntityRelocationFuelPolicy {
    static Quote quote(Entity target, int maximumBaseCost, Multipliers multipliers) {
        double multiplier = multipliers.forKind(classify(target));
        return new Quote(multiplier, scale(maximumBaseCost, multiplier));
    }

    static TargetKind classify(Entity target) {
        return classify(target instanceof Projectile, target instanceof Player,
            target.getType().is(Tags.EntityTypes.BOSSES), target instanceof Enemy);
    }

    static TargetKind classify(boolean projectile, boolean player, boolean boss, boolean hostile) {
        if (projectile) return TargetKind.PROJECTILE;
        if (player) return TargetKind.PLAYER;
        if (boss) return TargetKind.BOSS;
        if (hostile) return TargetKind.HOSTILE;
        return TargetKind.PASSIVE;
    }

    static int scale(int baseCost, double multiplier) {
        if (baseCost <= 0 || multiplier <= 0.0) return 0;
        double scaled = Math.floor(baseCost * multiplier);
        return scaled >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
    }

    enum TargetKind {
        PASSIVE,
        HOSTILE,
        PLAYER,
        BOSS,
        PROJECTILE
    }

    record Multipliers(double passive, double hostile, double player,
                       double boss, double projectile) {
        double forKind(TargetKind kind) {
            return switch (kind) {
                case PASSIVE -> passive;
                case HOSTILE -> hostile;
                case PLAYER -> player;
                case BOSS -> boss;
                case PROJECTILE -> projectile;
            };
        }
    }

    record Quote(double multiplier, int maximumReservation) {
        int cost(int rolledBaseCost) {
            return scale(rolledBaseCost, multiplier);
        }
    }

    private EntityRelocationFuelPolicy() {}
}
