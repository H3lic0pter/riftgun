package dev.riftgun.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

final class RiftGunCallbackIsolationTest {
    @Test
    void skipsFailingDimensionLabelProviderAndContinuesInOrder() {
        AtomicInteger failures = new AtomicInteger();
        RiftGunDimensionLabels.register(provider("test:failing_label", () -> {
            failures.incrementAndGet();
            throw new IllegalStateException("broken label provider");
        }));
        RiftGunDimensionLabels.register(provider("test:fallback_label",
            () -> Optional.of(Component.literal("Fallback"))));

        assertEquals("Fallback", RiftGunDimensionLabels.label(null,
            RiftResourceId.parse("test:dimension")).orElseThrow().getString());
        assertEquals("Fallback", RiftGunDimensionLabels.label(null,
            RiftResourceId.parse("test:dimension")).orElseThrow().getString());
        assertEquals(2, failures.get());
    }

    @Test
    void failsClosedWhenPortalOpenPolicyThrows() {
        AtomicInteger failures = new AtomicInteger();
        RiftGunPortalOpenPolicies.register(new RiftGunPortalOpenPolicy() {
            @Override
            public RiftResourceId id() {
                return RiftResourceId.parse("test:failing_open_policy");
            }

            @Override
            public PortalOpenPolicyDecision evaluate(
                net.minecraft.server.level.ServerPlayer opener
            ) {
                failures.incrementAndGet();
                throw new IllegalStateException("broken policy");
            }
        });

        PortalOpenPolicyDecision first = RiftGunPortalOpenPolicies.evaluate(null);
        PortalOpenPolicyDecision second = RiftGunPortalOpenPolicies.evaluate(null);

        assertFalse(first.allowed());
        assertFalse(second.allowed());
        assertEquals("message.riftgun.portal_open_policy_failed", first.message().getString());
        assertEquals(2, failures.get());
    }

    private static RiftGunDimensionLabelProvider provider(
        String id, java.util.function.Supplier<Optional<Component>> result
    ) {
        return new RiftGunDimensionLabelProvider() {
            @Override
            public RiftResourceId id() {
                return RiftResourceId.parse(id);
            }

            @Override
            public Optional<Component> label(net.minecraft.server.level.ServerPlayer viewer,
                                             RiftResourceId dimensionId) {
                return result.get();
            }
        };
    }
}
