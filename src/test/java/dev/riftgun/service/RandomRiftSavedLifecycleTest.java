package dev.riftgun.service;

import dev.riftgun.api.PortalOpenPolicyDecision;
import dev.riftgun.api.RiftGunPortalOpenPolicies;
import dev.riftgun.compat.infinity.InfiniteDimensionsCompat;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.fuel.PortalFuelManager;
import dev.riftgun.fuel.PortalFuelUse;
import dev.riftgun.module.PortalGunCapabilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises the real request/tick/cancel paths with world and addon boundaries mocked. */
final class RandomRiftSavedLifecycleTest {
    private final List<MockedStatic<?>> mocks = new ArrayList<>();
    private MinecraftServer server;
    private ServerPlayer player;
    private ServerLevel level;
    private PortalGunLocator.LocatedGun gun;
    private PortalPlayerData data;
    private Destination saved;
    private MockedStatic<InfiniteDimensionsCompat> infinity;
    private MockedStatic<PortalOpenCoordinator> opens;
    private MockedStatic<PortalFuelManager> fuel;
    private MockedStatic<RiftGunPortalOpenPolicies> policies;
    private boolean initialized;

    private <T> MockedStatic<T> boundary(Class<T> type) {
        MockedStatic<T> mock = mockStatic(type);
        mocks.add(mock);
        return mock;
    }

    @BeforeEach
    void setup() {
//? if >=1.21.11 {
        /*boundary(net.neoforged.fml.loading.FMLEnvironment.class)
            .when(net.neoforged.fml.loading.FMLEnvironment::isProduction).thenReturn(true);
*///?}
        // Unit tests have vanilla registries and no loader-owned mod feature flags.
        boundary(net.neoforged.neoforge.common.util.flag.FeatureFlagLoader.class);
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
//? if >=1.21.11 {
        /*var ticketId = net.minecraft.resources.Identifier.fromNamespaceAndPath("riftgun", "random_rift_preparation");
        var tickets = net.minecraft.core.registries.BuiltInRegistries.TICKET_TYPE;
        if (!tickets.containsKey(ticketId)) {
            // Register the fixture just as the loader's registry event would, then refreeze.
            ((net.minecraft.core.MappedRegistry<net.minecraft.server.level.TicketType>) tickets).unfreeze(false);
            try {
                net.minecraft.core.Registry.register(tickets, ticketId, new net.minecraft.server.level.TicketType(
                    net.minecraft.server.level.TicketType.NO_TIMEOUT,
                    net.minecraft.server.level.TicketType.FLAG_LOADING | net.minecraft.server.level.TicketType.FLAG_SIMULATION));
            } finally {
                tickets.freeze();
            }
        }
*///?}
        server = mock(MinecraftServer.class, RETURNS_DEEP_STUBS);
        player = mock(ServerPlayer.class);
        level = mock(ServerLevel.class, RETURNS_DEEP_STUBS);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        when(player.level()).thenReturn(level);
        when(level.getServer()).thenReturn(server);
//? if <1.21.11 {
        when(player.getServer()).thenReturn(server);
//?}
        when(player.isAlive()).thenReturn(true);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);
        when(server.getPlayerList().getPlayer(player.getUUID())).thenReturn(player);
        gun = mock(PortalGunLocator.LocatedGun.class);
        when(gun.saveReference()).thenReturn(new CompoundTag());
        data = new PortalPlayerData();
        saved = new Destination(UUID.randomUUID(), "Saved", PortalPlayerData.DEFAULT_GROUP_ID,
            Level.OVERWORLD, 1, 64, 2, 0, 1, 0, false, false, "text");
        data.destinations().add(saved);
        boundary(PortalDataStore.class).when(() -> PortalDataStore.load(player)).thenReturn(data);
        boundary(PortalGunLocator.class).when(() -> PortalGunLocator.resolveReference(eq(player), any()))
            .thenReturn(Optional.of(gun));
        var capabilities = mock(PortalGunCapabilities.class);
        when(capabilities.dimensionalTraversal()).thenReturn(true);
        boundary(PortalGunCapabilities.class).when(() -> PortalGunCapabilities.resolve(any(), anyInt()))
            .thenReturn(capabilities);
        policies = boundary(RiftGunPortalOpenPolicies.class);
        policies.when(() -> RiftGunPortalOpenPolicies.evaluate(player)).thenReturn(PortalOpenPolicyDecision.allow());
        fuel = boundary(PortalFuelManager.class);
        fuel.when(() -> PortalFuelManager.plan(eq(player), any(), any()))
            .thenReturn(PortalFuelManager.Plan.success(mock(PortalFuelUse.class)));
        infinity = boundary(InfiniteDimensionsCompat.class);
        opens = boundary(PortalOpenCoordinator.class);
        RandomRiftManager.reset();
        initialized = true;
    }

    @AfterEach
    void cleanup() {
        try {
            if (initialized) RandomRiftManager.reset();
        } finally {
            for (int i = mocks.size() - 1; i >= 0; i--) mocks.get(i).close();
        }
    }

    private void request() {
        assertTrue(RandomRiftManager.requestSaved(player, data, saved, PortalPlacementMode.FRONT,
            gun, false, null));
    }

    @Test
    void deletedTargetCancelsBeforeGeneration() {
        request();
        data.destinations().clear();
        RandomRiftManager.tick(server);
        assertFalse(RandomRiftManager.snapshot(player).searching());
        infinity.verifyNoInteractions();
        opens.verifyNoInteractions();
    }

    @Test
    void editedTargetCancelsBeforeGeneration() {
        request();
        data.replaceDestination(saved.withDetails("Changed", saved.groupId(), saved.dimension(), 3, 64, 2, 0));
        RandomRiftManager.tick(server);
        assertFalse(RandomRiftManager.snapshot(player).searching());
        infinity.verifyNoInteractions();
    }

    @Test
    void disconnectedPlayerRemovesQueuedSearch() {
        request();
        when(server.getPlayerList().getPlayer(player.getUUID())).thenReturn(null);
        RandomRiftManager.tick(server);
        assertFalse(RandomRiftManager.snapshot(player).searching());
        infinity.verifyNoInteractions();
    }

    @Test
    void pendingGenerationNeverOpensAndExactCompletionPreservesDescriptor() {
        request();
        RandomRiftManager.tick(server);
        opens.verifyNoInteractions();
        assertTrue(RandomRiftManager.snapshot(player).searching());
        infinity.when(() -> InfiniteDimensionsCompat.prepare(eq(player), eq(saved), any(), any())).thenReturn(true);
        RandomRiftManager.tick(server);
        opens.verify(() -> PortalOpenCoordinator.openResolvedSaved(player, data, saved, saved,
            PortalPlacementMode.FRONT, gun, false, null));
        assertFalse(RandomRiftManager.snapshot(player).searching());
        assertEquals(saved, data.destination(saved.id()).orElseThrow());
    }

    @Test
    void generationFailureCancelsWithoutOpening() {
        request();
        infinity.when(() -> InfiniteDimensionsCompat.prepare(eq(player), eq(saved), any(), any()))
            .thenThrow(new IllegalArgumentException("message.riftgun.infinity_key_missing"));
        RandomRiftManager.tick(server);
        assertFalse(RandomRiftManager.snapshot(player).searching());
        opens.verifyNoInteractions();
    }

    @Test
    void timedOutInstallationCancelsAndNeverRepeatsGeneration() {
        var generated = new java.util.concurrent.atomic.AtomicInteger();
        infinity.when(() -> InfiniteDimensionsCompat.prepare(eq(player), eq(saved), any(), any()))
            .thenAnswer(call -> call.<DeferredDimensionPreparation>getArgument(2).tick(
                () -> false, () -> false, call.getArgument(3), generated::incrementAndGet));
        request();
        for (int tick = 0; tick < 201; tick++) RandomRiftManager.tick(server);
        assertEquals(1, generated.get());
        assertFalse(RandomRiftManager.snapshot(player).searching());
        opens.verifyNoInteractions();
    }

    @Test
    void resolvedOpenRechecksPolicyAndDoesNotPersistFailure() {
        policies.when(() -> RiftGunPortalOpenPolicies.evaluate(player))
            .thenReturn(PortalOpenPolicyDecision.deny(Component.literal("Blocked")));
        opens.close();
        mocks.remove(opens);
        assertFalse(PortalOpenCoordinator.openResolvedSaved(player, data, saved, saved,
            PortalPlacementMode.FRONT, gun, false, null));
        assertEquals(saved, data.destination(saved.id()).orElseThrow());
    }

//? if >=1.21.11 {
    /*@Test
    void unsupportedGameVersionRejectsTextPreparation() {
        infinity.close();
        mocks.remove(infinity);
        assertFalse(InfiniteDimensionsCompat.available());
        var error = assertThrows(IllegalArgumentException.class, () -> InfiniteDimensionsCompat.prepare(
            player, saved, new DeferredDimensionPreparation(), new DimensionGenerationBudget()));
        assertEquals("message.riftgun.infinity_unavailable", error.getMessage());
    }
*///?}

    @Test
    void fuelLostWhileQueuedCancelsBeforeKeyConsumption() {
        request();
        fuel.when(() -> PortalFuelManager.plan(eq(player), any(), any()))
            .thenReturn(PortalFuelManager.Plan.failure("message.riftgun.fuel_empty"));
        RandomRiftManager.tick(server);
        assertFalse(RandomRiftManager.snapshot(player).searching());
        infinity.verifyNoInteractions();
        opens.verifyNoInteractions();
    }

    @Test
    void changedPolicyCancelsBeforeGeneration() {
        request();
        policies.when(() -> RiftGunPortalOpenPolicies.evaluate(player))
            .thenReturn(PortalOpenPolicyDecision.deny(Component.literal("Blocked")));
        RandomRiftManager.tick(server);
        assertFalse(RandomRiftManager.snapshot(player).searching());
        infinity.verifyNoInteractions();
        opens.verifyNoInteractions();
    }

    @Test
    void automaticTargetStartsChunkSearchInsteadOfOpeningStoredZeroCoordinates() {
        saved = new Destination(saved.id(), saved.name(), saved.groupId(), saved.dimension(),
            0, 0, 0, 0, 1, 0, false, true, null);
        data.replaceDestination(saved);
        request();
        RandomRiftManager.tick(server);
        assertTrue(RandomRiftManager.snapshot(player).searching());
        infinity.verifyNoInteractions();
        opens.verifyNoInteractions();
    }
}
