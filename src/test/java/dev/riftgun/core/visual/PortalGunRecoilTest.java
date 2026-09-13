package dev.riftgun.core.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.core.config.GunRecoilConfig;
import org.junit.jupiter.api.Test;

final class PortalGunRecoilTest {
    private static long millis(long value) {
        return value * 1_000_000L;
    }

    @Test
    void startsAtRestKicksThenReturnsWithin435Milliseconds() {
        PortalGunRecoil recoil = new PortalGunRecoil();
        assertEquals(0.0, recoil.sample(0L));
        recoil.fire(0L, GunRecoilConfig.defaults());
        assertEquals(0.0, recoil.sample(0L));
        assertTrue(recoil.sample(millis(15)) > 0.0);
        assertTrue(recoil.sample(millis(35)) > recoil.sample(millis(15)));
        assertTrue(recoil.sample(millis(100)) < recoil.sample(millis(35)));
        assertTrue(recoil.sample(millis(180)) > 0.0);
        assertEquals(0.0, recoil.sample(millis(435)));
        assertEquals(0.0, recoil.sample(millis(1000)));
    }

    @Test
    void firingDuringKickOrRecoveryPreservesTheCurrentPosition() {
        for (long delay : new long[] {10, 35, 80, 160}) {
            PortalGunRecoil recoil = new PortalGunRecoil();
            recoil.fire(0L, GunRecoilConfig.defaults());
            double before = recoil.sample(millis(delay));
            recoil.fire(millis(delay), GunRecoilConfig.defaults());
            assertEquals(before, recoil.sample(millis(delay)), 1.0e-12);
            assertTrue(recoil.sample(millis(delay + 20)) >= before);
            assertEquals(0.0, recoil.sample(millis(delay + 435)));
        }
    }

    @Test
    void sustainedRapidFireStaysBoundedAndStillSettles() {
        PortalGunRecoil recoil = new PortalGunRecoil();
        for (int time = 0; time < 2000; time++) {
            if (time % 20 == 0) recoil.fire(millis(time), GunRecoilConfig.defaults());
            double value = recoil.sample(millis(time));
            assertTrue(value >= 0.0 && value <= 1.0, "Recoil must stay within its pose limit");
        }
        assertEquals(0.0, recoil.sample(millis(2500)));
    }

    @Test
    void clearingForUnequipDisconnectOrDisableStopsTheShot() {
        PortalGunRecoil recoil = new PortalGunRecoil();
        recoil.fire(0L, GunRecoilConfig.defaults());
        assertTrue(recoil.sample(millis(35)) > 0.0);
        recoil.reset();
        assertEquals(0.0, recoil.sample(millis(36)));
        recoil.fire(millis(40), GunRecoilConfig.defaults());
        assertEquals(0.0, recoil.sample(millis(40)));
        assertTrue(recoil.sample(millis(75)) > 0.0);
    }

    @Test
    void customTimingAndStrengthControlTheEnvelope() {
        PortalGunRecoil recoil = new PortalGunRecoil();
        recoil.fire(0L, new GunRecoilConfig(80, 300, 0.4, 0.12, 6.0, 200));
        assertEquals(0.4, recoil.sample(millis(80)), 1.0e-12);
        assertEquals(0.2, recoil.sample(millis(230)), 1.0e-12);
        assertTrue(recoil.sample(millis(180)) > 0.0);
        assertEquals(0.0, recoil.sample(millis(380)));
    }

    @Test
    void nextShotUsesNewSettingsWithoutResettingTheCurrentEnvelope() {
        PortalGunRecoil recoil = new PortalGunRecoil();
        recoil.fire(0L, GunRecoilConfig.defaults());
        double before = recoil.sample(millis(60));
        recoil.fire(millis(60), new GunRecoilConfig(50, 250, 0.9, 0.12, 6.0, 200));
        assertEquals(before, recoil.sample(millis(60)), 1.0e-12);
        assertEquals(1.0, recoil.sample(millis(110)), 1.0e-12);
        assertEquals(0.0, recoil.sample(millis(360)));
    }

    @Test
    void samplingFrequencyDoesNotChangeTheAnimation() {
        PortalGunRecoil frequent = new PortalGunRecoil();
        PortalGunRecoil sparse = new PortalGunRecoil();
        frequent.fire(0L, GunRecoilConfig.defaults());
        sparse.fire(0L, GunRecoilConfig.defaults());
        for (long time = 0; time < 100; time++) frequent.sample(millis(time));
        assertEquals(sparse.sample(millis(100)), frequent.sample(millis(100)));
    }
}
