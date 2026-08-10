package dev.riftgun.crisis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PortalCrisisTestOverridesTest {
    private static final ResourceLocation FALL = ResourceLocation.fromNamespaceAndPath(
        "riftgun", "high_altitude_fall");
    private static final ResourceLocation LAVA = ResourceLocation.fromNamespaceAndPath(
        "riftgun", "lava_hazard");

    @AfterEach
    void resetOverrides() {
        PortalCrisisTestOverrides.reset();
    }

    @Test
    void forceReplacesTheExistingOneShotOverride() {
        UUID player = UUID.randomUUID();

        assertEquals(Optional.empty(), PortalCrisisTestOverrides.force(player, FALL));
        assertEquals(Optional.of(FALL), PortalCrisisTestOverrides.force(player, LAVA));
        assertEquals(Optional.of(LAVA), PortalCrisisTestOverrides.forced(player));
    }

    @Test
    void consumeOnlyRemovesTheExpectedOverride() {
        UUID player = UUID.randomUUID();
        PortalCrisisTestOverrides.force(player, FALL);

        assertFalse(PortalCrisisTestOverrides.consume(player, LAVA));
        assertEquals(Optional.of(FALL), PortalCrisisTestOverrides.forced(player));
        assertTrue(PortalCrisisTestOverrides.consume(player, FALL));
        assertEquals(Optional.empty(), PortalCrisisTestOverrides.forced(player));
    }

    @Test
    void clearAndResetRemoveArmedOverrides() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PortalCrisisTestOverrides.force(first, FALL);
        PortalCrisisTestOverrides.force(second, LAVA);

        assertEquals(Optional.of(FALL), PortalCrisisTestOverrides.clear(first));
        assertEquals(Optional.empty(), PortalCrisisTestOverrides.forced(first));
        PortalCrisisTestOverrides.reset();
        assertEquals(Optional.empty(), PortalCrisisTestOverrides.forced(second));
    }
}
