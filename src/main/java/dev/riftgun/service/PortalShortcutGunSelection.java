package dev.riftgun.service;

import dev.riftgun.core.config.RiftConfigs;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

/** Resolves the one gun a keyboard shortcut is allowed to operate on. */
public final class PortalShortcutGunSelection {
    public static Optional<PortalGunLocator.LocatedGun> locate(ServerPlayer player) {
        return select(RiftConfigs.server().shortcuts().gunLookupMode(),
            () -> VanillaInventoryPortalGunLocator.locateHeld(player),
            () -> PortalGunLocator.first(player));
    }

    public static PortalShortcutGunMode mode() {
        return RiftConfigs.server().shortcuts().gunLookupMode();
    }

    static <T> Optional<T> select(PortalShortcutGunMode mode,
                                  Supplier<Optional<T>> heldHands,
                                  Supplier<Optional<T>> registeredLocators) {
        return switch (mode) {
            case HELD_HANDS -> heldHands.get();
            case REGISTERED_LOCATORS -> registeredLocators.get();
        };
    }

    static <T> Optional<T> preferMainHand(Supplier<Optional<T>> mainHand,
                                           Supplier<Optional<T>> offhand) {
        Optional<T> main = mainHand.get();
        return main.isPresent() ? main : offhand.get();
    }

    private PortalShortcutGunSelection() {}
}
