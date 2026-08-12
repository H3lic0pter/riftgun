package dev.riftgun.relocation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class EntityRelocationFuelPolicyTest {
    private static final EntityRelocationFuelPolicy.Multipliers DEFAULTS =
        new EntityRelocationFuelPolicy.Multipliers(1.5, 3.0, 3.0, 10.0, 1.0);

    @Test
    void classificationUsesProjectilePlayerBossHostilePassivePriority() {
        assertEquals(EntityRelocationFuelPolicy.TargetKind.PROJECTILE,
            EntityRelocationFuelPolicy.classify(true, true, true, true));
        assertEquals(EntityRelocationFuelPolicy.TargetKind.PLAYER,
            EntityRelocationFuelPolicy.classify(false, true, true, true));
        assertEquals(EntityRelocationFuelPolicy.TargetKind.BOSS,
            EntityRelocationFuelPolicy.classify(false, false, true, true));
        assertEquals(EntityRelocationFuelPolicy.TargetKind.HOSTILE,
            EntityRelocationFuelPolicy.classify(false, false, false, true));
        assertEquals(EntityRelocationFuelPolicy.TargetKind.PASSIVE,
            EntityRelocationFuelPolicy.classify(false, false, false, false));
    }

    @Test
    void defaultMultipliersMatchTargetCategories() {
        assertEquals(1.5, DEFAULTS.forKind(EntityRelocationFuelPolicy.TargetKind.PASSIVE));
        assertEquals(3.0, DEFAULTS.forKind(EntityRelocationFuelPolicy.TargetKind.HOSTILE));
        assertEquals(3.0, DEFAULTS.forKind(EntityRelocationFuelPolicy.TargetKind.PLAYER));
        assertEquals(10.0, DEFAULTS.forKind(EntityRelocationFuelPolicy.TargetKind.BOSS));
        assertEquals(1.0, DEFAULTS.forKind(EntityRelocationFuelPolicy.TargetKind.PROJECTILE));
    }

    @Test
    void scalesRolledAndMaximumCostsDownward() {
        EntityRelocationFuelPolicy.Quote quote = new EntityRelocationFuelPolicy.Quote(
            1.5, EntityRelocationFuelPolicy.scale(8, 1.5));

        assertEquals(10, quote.cost(7));
        assertEquals(12, quote.maximumReservation());
    }

    @Test
    void zeroMultiplierMakesReservationAndUseFree() {
        assertEquals(0, EntityRelocationFuelPolicy.scale(100, 0.0));
        assertEquals(0, new EntityRelocationFuelPolicy.Quote(0.0, 0).cost(100));
    }
}
