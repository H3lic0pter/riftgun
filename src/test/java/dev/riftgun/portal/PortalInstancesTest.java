package dev.riftgun.portal;

import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingEndpoint;
import dev.riftgun.pairing.PortalPairingLegacyMigration;
import dev.riftgun.pairing.PortalPairingPendingEndpoint;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.service.PortalGunIdentity;
import dev.riftgun.service.PortalGunLocator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class PortalInstancesTest {
    private MinecraftServer server;
    private ServerLevel level;
    private ServerPlayer player;

    @BeforeAll
    static void bootstrap() throws ClassNotFoundException {
        try (var flags = mockStatic(net.neoforged.neoforge.common.util.flag.FeatureFlagLoader.class)
            //? if >=1.21.11 {
            /*; var environment = mockStatic(net.neoforged.fml.loading.FMLEnvironment.class)
            *///?}
        ) {
            //? if >=1.21.11 {
            /*environment.when(net.neoforged.fml.loading.FMLEnvironment::isProduction).thenReturn(true);
            *///?}
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
            //? if >=1.21.11 {
            /*try (var registrations = mockConstruction(net.neoforged.neoforge.registries.DeferredHolder.class,
                    (holder, context) -> when(holder.get()).thenReturn(mock(net.minecraft.server.level.TicketType.class)))) {
                Class.forName(PortalEntity.class.getName());
            }
            *///?}
        }
    }

    @BeforeEach
    void setup() {
        server = mock(MinecraftServer.class);
        level = mock(ServerLevel.class);
        when(level.getServer()).thenReturn(server);
        player = owner(UUID.randomUUID());
    }

    @AfterEach
    void cleanup() { PortalOwnerIndex.clear(server); }

    private ServerPlayer owner(UUID id) {
        var result = mock(ServerPlayer.class);
        var data = new CompoundTag();
        data.put("riftgun:portal_instances", PortalInstanceState.EMPTY.save());
        when(result.getPersistentData()).thenReturn(data);
        when(result.getUUID()).thenReturn(id);
        when(result.level()).thenReturn(level);
        return result;
    }

    private PortalEntity portal(ServerPlayer owner, PortalFunctionMode mode) {
        UUID ownerId = owner.getUUID();
        var portal = mock(PortalEntity.class);
        when(portal.level()).thenReturn(level);
        when(portal.getUUID()).thenReturn(UUID.randomUUID());
        when(portal.ownerId()).thenReturn(ownerId);
        when(portal.functionMode()).thenReturn(mode);
        when(portal.phase()).thenReturn(PortalLifecycle.Phase.OPEN);
        PortalOwnerIndex.track(portal);
        return portal;
    }

    private PortalPairingPendingEndpoint marker(PortalPairingEndpoint endpoint) {
        return new PortalPairingPendingEndpoint(player.getUUID(), Level.OVERWORLD,
            new PortalPlacement(new Vec3(0, 64, 0), PortalOrientation.VERTICAL,
                PortalGeometry.FLOATING_VERTICAL, 0, null, null), endpoint);
    }

    @Test
    void vanillaVoidCheckDoesNotDiscardPortalBeforeItsLifecycleEnds() {
        PortalEntity portal = mock(PortalEntity.class);
        when(portal.level()).thenReturn(level);
        when(portal.getY()).thenReturn(-1_000.0);
        doCallRealMethod().when(portal).checkBelowWorld();
        doCallRealMethod().when(portal).onBelowWorld();

        portal.checkBelowWorld();

        verify(portal).onBelowWorld();
        verify(portal, never()).discard();
    }

    @Test
    void coordinateReplacementRetainsPairAndMarkerAndOtherPlayers() {
        var pending = marker(PortalPairingEndpoint.A);
        PortalInstances.placePending(player, pending);
        var pair = portal(player, PortalFunctionMode.PORTAL_PAIRING);
        var old = portal(player, PortalFunctionMode.COORDINATE_TRAVEL);
        var fresh = portal(player, PortalFunctionMode.COORDINATE_TRAVEL);
        var foreign = portal(owner(UUID.randomUUID()), PortalFunctionMode.COORDINATE_TRAVEL);
        PortalInstances.replaceOpened(player, PortalFunctionMode.COORDINATE_TRAVEL, Set.of(fresh.getUUID()));
        verify(old).startClosing();
        verify(pair, never()).startClosing();
        verify(fresh, never()).startClosing();
        verify(foreign, never()).startClosing();
        assertEquals(pending, PortalInstances.pending(player));
    }

    @Test
    void pairReplacementClearsMarkerAndOldPairOnly() {
        PortalInstances.placePending(player, marker(PortalPairingEndpoint.B));
        var coordinate = portal(player, PortalFunctionMode.COORDINATE_TRAVEL);
        var old = portal(player, PortalFunctionMode.PORTAL_PAIRING);
        var fresh = portal(player, PortalFunctionMode.PORTAL_PAIRING);
        PortalInstances.replaceOpened(player, PortalFunctionMode.PORTAL_PAIRING, Set.of(fresh.getUUID()));
        verify(old).startClosing();
        verify(fresh, never()).startClosing();
        verify(coordinate, never()).startClosing();
        assertNull(PortalInstances.pending(player));
    }

    @Test
    void entityTargetReplacesOnlyPairGroupAndClearAllRemovesBothGroups() {
        var pair = portal(player, PortalFunctionMode.PORTAL_PAIRING);
        var coordinate = portal(player, PortalFunctionMode.COORDINATE_TRAVEL);
        var target = marker(PortalPairingEndpoint.ENTITY_TARGET);
        PortalInstances.placePending(player, target);
        verify(pair).startClosing();
        verify(coordinate, never()).startClosing();
        assertEquals(target, PortalInstances.pending(player));
        PortalInstances.clearAll(player);
        verify(coordinate).startClosing();
        assertNull(PortalInstances.pending(player));
        PortalInstances.importLegacy(player, target);
        assertNull(PortalInstances.pending(player), "cleared legacy markers must not resurrect");
    }

    @Test
    void clearingCurrentCoordinateModePreservesPendingPair() {
        var pending = marker(PortalPairingEndpoint.A);
        PortalInstances.placePending(player, pending);
        var coordinate = portal(player, PortalFunctionMode.COORDINATE_TRAVEL);
        PortalInstances.clearMode(player, PortalFunctionMode.COORDINATE_TRAVEL);
        verify(coordinate).startClosing();
        assertEquals(pending, PortalInstances.pending(player));
        PortalInstances.clearMode(player, PortalFunctionMode.PORTAL_PAIRING);
        assertNull(PortalInstances.pending(player));
    }

    @Test
    void markersSurviveEntityCleanupAndPlayerCloneWithoutAGun() {
        var pending = marker(PortalPairingEndpoint.A);
        PortalInstances.placePending(player, pending);
        var opened = portal(player, PortalFunctionMode.COORDINATE_TRAVEL);
        PortalInstances.closeOpened(server, player.getUUID());
        verify(opened).startClosing();
        assertEquals(pending, PortalInstances.pending(player));
        var replacement = owner(player.getUUID());
        PortalInstances.copy(player, replacement);
        assertEquals(pending, PortalInstances.pending(replacement));
        PortalInstances.clearAll(player);
        assertEquals(pending, PortalInstances.pending(replacement), "cloned storage must not alias the old player");
    }

    @Test
    void legacyGunImportRunsOnceEvenAfterExplicitClear() {
        player.getPersistentData().remove("riftgun:portal_instances");
        var pending = marker(PortalPairingEndpoint.A);
        try (var migration = mockStatic(PortalPairingLegacyMigration.class)) {
            migration.when(() -> PortalPairingLegacyMigration.fromGuns(player)).thenReturn(pending);
            assertEquals(pending, PortalInstances.pending(player));
            PortalInstances.clearAll(player);
            assertNull(PortalInstances.pending(player));
            migration.verify(() -> PortalPairingLegacyMigration.fromGuns(player), times(1));
        }
    }

    @Test
    void migrationChoosesNewestOwnedMarkerAndDiscardsForeignOrCorruptOnes() {
        var guns = new java.util.ArrayList<PortalGunLocator.LocatedGun>();
        var a = marker(PortalPairingEndpoint.A);
        var b = marker(PortalPairingEndpoint.B);
        var foreign = new PortalPairingPendingEndpoint(UUID.randomUUID(), b.dimension(),
            b.placement(), PortalPairingEndpoint.ENTITY_TARGET);
        var inventory = mock(Inventory.class);
        when(player.getInventory()).thenReturn(inventory);
        try (var locators = mockStatic(PortalGunLocator.class);
             var identity = mockStatic(PortalGunIdentity.class)) {
            List<PortalPairingPendingEndpoint> markers = List.of(a, b, foreign, a);
            for (int i = 0; i < markers.size(); i++) {
                var stack = mock(ItemStack.class);
                var gunId = UUID.randomUUID();
                var tag = markers.get(i).save();
                Nbt.putUUID(tag, "Gun", i == 3 ? UUID.randomUUID() : gunId);
                tag.putLong("StartedAt", 100 + i);
                tag.putInt("DurationTicks", 20);
                when(stack.get(PortalGunComponents.PENDING_PAIRING_ENDPOINT)).thenReturn(tag);
                identity.when(() -> PortalGunIdentity.existing(stack)).thenReturn(gunId);
                guns.add(new PortalGunLocator.LocatedGun("test", new CompoundTag(), stack));
            }
            locators.when(() -> PortalGunLocator.all(player)).thenReturn(guns);
            assertEquals(b, PortalPairingLegacyMigration.fromGuns(player));
            for (var gun : guns) verify(gun.stack()).remove(PortalGunComponents.PENDING_PAIRING_ENDPOINT);
            verify(player.getInventory()).setChanged();
        }
    }
}
