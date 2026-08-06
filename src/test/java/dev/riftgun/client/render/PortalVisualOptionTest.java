package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class PortalVisualOptionTest {
    @Test
    void rangeClampsAndSnapsToConfiguredStep() {
        AtomicReference<Double> value = new AtomicReference<>(5.0);
        PortalVisualOption.Range option = range(value);

        assertEquals(1.5, option.valueAt(-1.0));
        assertEquals(20.0, option.valueAt(2.0));
        assertEquals(5.2, option.valueAt((5.16 - 1.5) / 18.5));
    }

    @Test
    void rangeResetRestoresItsOwnDefaultWithoutChangingAvailability() {
        AtomicReference<Double> value = new AtomicReference<>(14.0);
        PortalVisualOption.Range option = range(value);

        option.reset();

        assertEquals(5.0, value.get());
        assertFalse(option.active());
    }

    private static PortalVisualOption.Range range(AtomicReference<Double> value) {
        return new PortalVisualOption.Range("period", value::get, value::set,
            () -> false, 1.5, 20.0, 0.1, 5.0);
    }
}
