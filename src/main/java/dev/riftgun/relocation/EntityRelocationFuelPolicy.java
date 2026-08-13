package dev.riftgun.relocation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import java.util.List;
import java.util.function.IntSupplier;
import net.neoforged.neoforge.common.Tags;

/** Classifies a relocation target once and snapshots its configurable fuel multiplier. */
final class EntityRelocationFuelPolicy {
    static Quote quote(Entity target, int maximumBaseCost, Multipliers multipliers) {
        return quote(List.of(classify(target)), maximumBaseCost, multipliers);
    }

    static Quote quote(List<TargetKind> kinds, int maximumBaseCost, Multipliers multipliers) {
        List<Double> applied = kinds.stream().map(multipliers::forKind).toList();
        long maximum = 0L;
        for (double multiplier : applied) {
            maximum += scale(maximumBaseCost, multiplier);
            if (maximum >= Integer.MAX_VALUE) {
                maximum = Integer.MAX_VALUE;
                break;
            }
        }
        return new Quote(applied, (int) maximum);
    }

    static TargetKind classify(Entity target) {
        if (target instanceof ItemEntity || target instanceof VehicleEntity) return TargetKind.UTILITY;
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
        PROJECTILE,
        UTILITY
    }

    record Multipliers(double passive, double hostile, double player,
                       double boss, double projectile, double utility) {
        double forKind(TargetKind kind) {
            return switch (kind) {
                case PASSIVE -> passive;
                case HOSTILE -> hostile;
                case PLAYER -> player;
                case BOSS -> boss;
                case PROJECTILE -> projectile;
                case UTILITY -> utility;
            };
        }
    }

    record Quote(List<Double> multipliers, int maximumReservation) {
        Quote(double multiplier, int maximumReservation) {
            this(List.of(multiplier), maximumReservation);
        }

        int cost(int rolledBaseCost) {
            long total = 0L;
            for (double multiplier : multipliers) {
                total += scale(rolledBaseCost, multiplier);
                if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
            }
            return (int) total;
        }

        int cost(IntSupplier rolledBaseCosts) {
            long total = 0L;
            for (double multiplier : multipliers) {
                total += scale(rolledBaseCosts.getAsInt(), multiplier);
                if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
            }
            return (int) total;
        }
    }

    private EntityRelocationFuelPolicy() {}
}
