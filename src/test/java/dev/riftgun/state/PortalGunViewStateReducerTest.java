package dev.riftgun.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.state.PortalGunViewStateReducer.BooleanSetting;
import dev.riftgun.state.PortalGunViewStateReducer.DistanceSetting;
import dev.riftgun.state.PortalGunViewStateReducer.FallbackSetting;
import org.junit.jupiter.api.Test;

final class PortalGunViewStateReducerTest {
    @Test
    void optimisticBooleanUpdateChangesOnlyItsOwnedSetting() {
        PortalGunViewState initial = PortalGunViewStateFixtures.representative();

        PortalGunViewState updated = PortalGunViewStateReducer.withBoolean(initial,
            BooleanSetting.REMOTE_RADIAL_SLIDER, false);

        assertFalse(updated.remoteRadialSliderEnabled());
        assertTrue(updated.remoteScrollAdjustmentEnabled());
        assertEquals(initial.transit(), updated.transit());
    }

    @Test
    void typedDistanceAndFallbackUpdatesPreserveUnrelatedState() {
        PortalGunViewState initial = PortalGunViewStateFixtures.representative();
        PortalGunViewState distance = PortalGunViewStateReducer.withDistance(initial,
            DistanceSetting.REMOTE_DISTANCE, 77);
        PortalGunViewState fallback = PortalGunViewStateReducer.withFallback(distance,
            FallbackSetting.COORDINATE_SMART, PortalFloatingFallback.REMOTE);

        assertEquals(77, fallback.remoteDistance());
        assertEquals(PortalFloatingFallback.REMOTE,
            fallback.placement().coordinateSmartFallback());
        assertEquals(initial.transit(), fallback.transit());
        assertEquals(initial.navigation(), fallback.navigation());
    }
}
