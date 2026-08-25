package dev.riftgun.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

final class RiftGunDestinationProvidersTest {
    @Test
    void registersProvidersByStableIdAndRejectsDuplicates() {
        RiftResourceId providerId = RiftResourceId.parse("riftworld:test_provider");
        RiftGunDestinationProvider provider = new RiftGunDestinationProvider() {
            @Override
            public RiftResourceId id() {
                return providerId;
            }

            @Override
            public List<ProvidedPortalDestination> destinations(net.minecraft.server.level.ServerPlayer viewer) {
                return List.of();
            }
        };

        RiftGunDestinationProviders.register(provider);

        assertSame(provider, RiftGunDestinationProviders.provider(providerId).orElseThrow());
        assertThrows(IllegalStateException.class, () -> RiftGunDestinationProviders.register(provider));
    }

    @Test
    void destinationEntrySeparatesProviderLocalIdFromResolvedTarget() {
        PortalDestination target = new PortalDestination(
            RiftResourceId.parse("riftworld:reality/test"), 4.0, 80.0, 8.0, 180.0F);
        ProvidedPortalDestination entry = new ProvidedPortalDestination(
            RiftResourceId.parse("riftworld:test"), Component.literal("Test Reality"), target);

        assertEquals("riftworld:test", entry.id().toString());
        assertEquals(target, entry.destination());
    }
}
