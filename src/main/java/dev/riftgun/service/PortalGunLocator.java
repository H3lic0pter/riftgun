package dev.riftgun.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface PortalGunLocator {
    List<PortalGunLocator> LOCATORS = new CopyOnWriteArrayList<>();

    boolean hasPortalGun(ServerPlayer player);

    static void register(PortalGunLocator locator) {
        LOCATORS.add(locator);
    }

    static boolean anyHasPortalGun(ServerPlayer player) {
        return LOCATORS.stream().anyMatch(locator -> locator.hasPortalGun(player));
    }
}

