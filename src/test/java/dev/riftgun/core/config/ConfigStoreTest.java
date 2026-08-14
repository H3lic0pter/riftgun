package dev.riftgun.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ConfigStoreTest {
    @Test
    void publishesCompleteSnapshots() {
        ConfigStore<String> store = new ConfigStore<>("old");

        assertEquals("old", store.current());
        store.publish("new");
        assertEquals("new", store.current());
    }

    @Test
    void rejectsNullSnapshots() {
        ConfigStore<String> store = new ConfigStore<>("initial");

        assertThrows(NullPointerException.class, () -> store.publish(null));
        assertEquals("initial", store.current());
    }
}
