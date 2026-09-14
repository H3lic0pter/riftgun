package dev.riftgun.network;

import dev.riftgun.appearance.GunPresentationDefaults;
import dev.riftgun.core.config.RiftConfig;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.core.msg.Msg;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlayerSettings;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingManager;
import dev.riftgun.pairing.PortalPairingPendingEndpoints;
import dev.riftgun.relocation.EntityRelocationManager;
import dev.riftgun.service.PortalGunIdentity;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.service.PortalShortcutGunSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises request dispatch and real relocation destination routing together. */
final class PairingRelocationShortcutTest {
    private final List<MockedStatic<?>> boundaries = new ArrayList<>();
    private ServerPlayer player;
    private ItemStack stack;
    private PortalGunLocator.LocatedGun gun;
    private PortalPlayerData data;
    private PortalGunCapabilities capabilities;
    private MockedStatic<PortalPairingPendingEndpoints> targets;
    private MockedStatic<Msg> messages;

    private <T> MockedStatic<T> boundary(Class<T> type) {
        var mocked = mockStatic(type);
        boundaries.add(mocked);
        return mocked;
    }

    @BeforeEach
    void setup() throws ClassNotFoundException {
        //? if >=1.21.11 {
        /*boundary(net.neoforged.fml.loading.FMLEnvironment.class)
            .when(net.neoforged.fml.loading.FMLEnvironment::isProduction).thenReturn(true);
        *///?}
        boundary(net.neoforged.neoforge.common.util.flag.FeatureFlagLoader.class);
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        //? if >=1.21.11 {
        /*// Unit tests do not run the mod's ticket registration events.
        try (var registrations = mockConstruction(net.neoforged.neoforge.registries.DeferredHolder.class,
                (holder, context) -> when(holder.get()).thenReturn(mock(net.minecraft.server.level.TicketType.class)))) {
            Class.forName(EntityRelocationManager.class.getName());
        }
        *///?}
        player = mock(ServerPlayer.class);
        stack = mock(ItemStack.class);
        gun = mock(PortalGunLocator.LocatedGun.class);
        when(gun.stack()).thenReturn(stack);
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        var server = mock(MinecraftServer.class);
        var level = mock(ServerLevel.class);
        when(player.level()).thenReturn(level);
        when(level.getServer()).thenReturn(server);
        when(server.overworld()).thenReturn(level);
        //? if <1.21.11 {
        when(player.getServer()).thenReturn(server);
        //?}
        data = mock(PortalPlayerData.class);
        when(data.settings()).thenReturn(PortalPlayerSettings.defaults()
            .withPlacementMode(PortalPlacementMode.ENTITY_RELOCATION));
        capabilities = mock(PortalGunCapabilities.class);
        when(capabilities.entityRelocation()).thenReturn(true);
        when(capabilities.portalPairing()).thenReturn(true);
        when(capabilities.maximumSurfaceRange()).thenReturn(16);
        when(capabilities.functionMode()).thenReturn(PortalFunctionMode.COORDINATE_TRAVEL);
        boundary(PortalGunCapabilities.class).when(() -> PortalGunCapabilities.resolve(eq(stack), anyInt()))
            .thenReturn(capabilities);
        boundary(PortalShortcutGunSelection.class).when(() -> PortalShortcutGunSelection.locate(player))
            .thenReturn(Optional.of(gun));
        boundary(GunPresentationDefaults.class).when(() -> GunPresentationDefaults.initialize(player, stack))
            .thenReturn(true);
        boundary(PortalDataStore.class).when(() -> PortalDataStore.load(player)).thenReturn(data);
        boundary(PortalGunIdentity.class).when(() -> PortalGunIdentity.ensure(stack)).thenReturn(UUID.randomUUID());
        targets = boundary(PortalPairingPendingEndpoints.class);
        messages = boundary(Msg.class);
        var config = mock(RiftConfig.class, RETURNS_DEEP_STUBS);
        when(config.relocation().maximumPassengerTreeSize()).thenReturn(16);
        boundary(RiftConfigs.class).when(RiftConfigs::server).thenReturn(config);

        // A live item in the crosshair lets the real target selection reach destination resolution.
        var target = mock(ItemEntity.class);
        when(target.getUUID()).thenReturn(UUID.randomUUID());
        when(target.isAlive()).thenReturn(true);
        when(target.getBoundingBox()).thenReturn(new AABB(1, 0, 0, 2, 1, 1));
        when(player.getEyePosition()).thenReturn(Vec3.ZERO);
        when(player.getLookAngle()).thenReturn(new Vec3(1, 0, 0));
        when(player.getBoundingBox()).thenReturn(new AABB(0, 0, 0, 1, 2, 1));
        var miss = mock(HitResult.class);
        when(miss.getType()).thenReturn(HitResult.Type.MISS);
        when(player.pick(anyDouble(), anyFloat(), anyBoolean())).thenReturn(miss);
        when(level.getEntities(eq(player), any(AABB.class), any())).thenReturn(List.of(target));
    }

    @AfterEach
    void cleanup() {
        for (int i = boundaries.size() - 1; i >= 0; i--) boundaries.get(i).close();
    }

    @Test
    void pairingShortcutRequiresPairingTargetEvenWhenGunUsesCoordinates() {
        send(PortalAction.PLACE_PAIRING_ENDPOINT, false);
        messages.verify(() -> Msg.displayClientMessage(player,
            Component.translatable("message.riftgun.pairing_target_required"), true));
        targets.verify(() -> PortalPairingPendingEndpoints.getValid(eq(stack), any(), any(), anyLong()));
        verify(data, never()).selectedDestinationId();
        verify(data, never()).selectedPlayerId();
        verifyNoInteractions(stack);
    }

    @Test
    void ordinaryRelocationStillUsesStoredCoordinateMode() {
        send(PortalAction.RELOCATE_ENTITY, false);
        messages.verify(() -> Msg.displayClientMessage(player,
            Component.translatable("message.riftgun.no_destination_selected"), true));
        targets.verifyNoInteractions();
        verify(data).selectedDestinationId();
        verifyNoInteractions(stack);
    }

    @Test
    void shiftPairingShortcutStillSetsTarget() {
        try (var pairing = mockStatic(PortalPairingManager.class)) {
            send(PortalAction.PLACE_PAIRING_ENDPOINT, true);
            pairing.verify(() -> PortalPairingManager.setRelocationTargetFromShortcut(player, data, gun));
            verify(player, never()).getEyePosition();
        }
    }

    @Test
    void pairingShortcutStillRequiresPairingModule() {
        when(capabilities.portalPairing()).thenReturn(false);
        send(PortalAction.PLACE_PAIRING_ENDPOINT, false);
        messages.verify(() -> Msg.displayClientMessage(player,
            Component.translatable("message.riftgun.portal_pairing_module_required"), true));
        verify(player, never()).getEyePosition();
    }

    private void send(PortalAction action, boolean endpointA) {
        var request = new CompoundTag();
        request.putString("Action", action.name());
        request.putBoolean("EndpointA", endpointA);
        PortalRequestHandler.handle(player, request);
    }
}
