package dev.riftgun.service;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Application output port for client acknowledgements; networking installs the adapter. */
public final class PortalClientSync {
    private static volatile Adapter adapter = Adapter.NOOP;

    public static void install(Adapter next) {
        adapter = Objects.requireNonNull(next, "client sync adapter");
    }

    public static void snapshot(ServerPlayer player, boolean openScreen) {
        adapter.snapshot(player, openScreen, null);
    }

    public static void snapshot(ServerPlayer player, boolean openScreen,
                                PortalGunLocator.LocatedGun gun) {
        adapter.snapshot(player, openScreen, gun);
    }

    public static void portalOpened(ServerPlayer player) {
        adapter.portalOpened(player);
    }

    public interface Adapter {
        Adapter NOOP = new Adapter() {
            @Override
            public void snapshot(ServerPlayer player, boolean openScreen,
                                 @Nullable PortalGunLocator.LocatedGun gun) {}

            @Override
            public void portalOpened(ServerPlayer player) {}
        };

        void snapshot(ServerPlayer player, boolean openScreen,
                      @Nullable PortalGunLocator.LocatedGun gun);

        void portalOpened(ServerPlayer player);
    }

    private PortalClientSync() {}
}
