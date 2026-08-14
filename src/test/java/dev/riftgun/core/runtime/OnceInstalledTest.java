package dev.riftgun.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class OnceInstalledTest {
    @Test
    void installsExactlyOnceAndFailsFastBeforeInstallation() {
        OnceInstalled<String> slot = new OnceInstalled<>("test runtime");

        assertThrows(IllegalStateException.class, slot::current);
        slot.install("first");
        assertEquals("first", slot.current());
        assertThrows(IllegalStateException.class, () -> slot.install("second"));
    }
}
