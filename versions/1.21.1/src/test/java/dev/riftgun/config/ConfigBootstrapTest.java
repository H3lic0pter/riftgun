package dev.riftgun.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.riftgun.core.config.RiftConfigs;
import org.junit.jupiter.api.Test;

final class ConfigBootstrapTest {
    @Test
    void configSpecsCanInitializeBeforeNeoForgeLoadsTheirValues() {
        ClassLoader loader = ConfigBootstrapTest.class.getClassLoader();

        assertDoesNotThrow(() -> Class.forName(ServerConfig.class.getName(), true, loader));
        assertDoesNotThrow(() -> Class.forName(ClientConfig.class.getName(), true, loader));
        assertEquals(256, RiftConfigs.server().destinations().maximumDestinations());
        assertEquals("riftgun:swirl", RiftConfigs.client().portalVisualType());
    }
}
