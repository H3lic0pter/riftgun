package dev.riftgun.fuel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;

final class PortalGunVisualStateTest {
    @Test
    void oldSavedVisualsAreRecomputedToPickUpActualMode() {
        var legacy = JsonParser.parseString("""
            {"liquid_tint":4,"core_visible":true,"fuel_rgb":5233522}
            """);
        var decoded = PortalGunVisualState.CODEC.parse(JsonOps.INSTANCE, legacy).getOrThrow();
        assertFalse(decoded.initialized());
        assertTrue(decoded.coreVisible());
        assertEquals(5233522, decoded.fuelRgb());
    }

    @Test
    void modeRoundTripsThroughPersistenceAndItemSynchronizationWithoutChangingGeometry() {
        for (boolean pairing : new boolean[] {false, true}) {
            var state = new PortalGunVisualState(4, true, 0x4FCB72, pairing);
            var encoded = PortalGunVisualState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
            assertEquals(state, PortalGunVisualState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
            var buffer = Unpooled.buffer();
            try {
                PortalGunVisualState.STREAM_CODEC.encode(buffer, state);
                assertEquals(state, PortalGunVisualState.STREAM_CODEC.decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
            assertEquals(11, state.geometryKey());
            assertEquals(11, state.snapshot().geometryKey());
        }
    }

    @Test
    void encodesEightLiquidStatesAndTwoCoreStates() {
        assertEquals(0, new PortalGunVisualState(0, false, 0).geometryKey());
        assertEquals(1, new PortalGunVisualState(2, false, 0).geometryKey());
        assertEquals(7, new PortalGunVisualState(8, false, 0).geometryKey());
        assertEquals(8, new PortalGunVisualState(0, true, 0).geometryKey());
        assertEquals(15, new PortalGunVisualState(8, true, 0).geometryKey());
    }

    @Test
    void distinguishesMigrationSentinelFromAnEmptyTank() {
        assertFalse(PortalGunVisualState.UNINITIALIZED.initialized());
        assertTrue(new PortalGunVisualState(0, false, 0).initialized());
    }

    @Test
    void quantizesLiquidUsingTheExistingSevenVisualLevels() {
        assertEquals(0, PortalGunVisualState.liquidTintIndex(0, 8000));
        assertEquals(8, PortalGunVisualState.liquidTintIndex(1, 8000));
        assertEquals(7, PortalGunVisualState.liquidTintIndex(400, 8000));
        assertEquals(2, PortalGunVisualState.liquidTintIndex(7600, 8000));
    }
}
