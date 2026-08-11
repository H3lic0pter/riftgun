package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class PortalShortcutGunSelectionTest {
    @Test
    void heldHandsModeNeverConsultsRegisteredLocators() {
        AtomicInteger locatorCalls = new AtomicInteger();

        Optional<String> selected = PortalShortcutGunSelection.select(
            PortalShortcutGunMode.HELD_HANDS,
            () -> Optional.of("main-hand"),
            () -> {
                locatorCalls.incrementAndGet();
                return Optional.of("inventory");
            });

        assertEquals(Optional.of("main-hand"), selected);
        assertEquals(0, locatorCalls.get());
    }

    @Test
    void locatorModeUsesTheExtensibleLocatorChain() {
        AtomicInteger heldCalls = new AtomicInteger();

        Optional<String> selected = PortalShortcutGunSelection.select(
            PortalShortcutGunMode.REGISTERED_LOCATORS,
            () -> {
                heldCalls.incrementAndGet();
                return Optional.of("main-hand");
            },
            () -> Optional.of("accessory-slot"));

        assertEquals(Optional.of("accessory-slot"), selected);
        assertEquals(0, heldCalls.get());
    }

    @Test
    void mainHandWinsWithoutConsultingTheOffhand() {
        AtomicInteger offhandCalls = new AtomicInteger();

        Optional<String> selected = PortalShortcutGunSelection.preferMainHand(
            () -> Optional.of("main-hand"),
            () -> {
                offhandCalls.incrementAndGet();
                return Optional.of("offhand");
            });

        assertEquals(Optional.of("main-hand"), selected);
        assertEquals(0, offhandCalls.get());
    }

    @Test
    void offhandIsUsedOnlyWhenTheMainHandHasNoGun() {
        Optional<String> selected = PortalShortcutGunSelection.preferMainHand(
            Optional::empty, () -> Optional.of("offhand"));

        assertEquals(Optional.of("offhand"), selected);
    }
}
