package dev.riftgun.network;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.core.network.RiftNetwork;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.sound.PortalSoundRegistry;
import dev.riftgun.sound.PortalSoundSettings;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

final class PortalSoundRequestsTest {
    @BeforeAll
    static void bootstrap() {
        try (var flags = mockStatic(net.neoforged.neoforge.common.util.flag.FeatureFlagLoader.class)) {
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
        }
    }

    @Test
    void soundOnlyUpdateDoesNotValidateOrChangeThePersistedPlacementMode() {
        for (var mode : new PortalPlacementMode[] {PortalPlacementMode.REMOTE, PortalPlacementMode.ENTITY_RELOCATION}) {
            ServerPlayer player = mock(ServerPlayer.class);
            var data = new PortalPlayerData();
            data.settings(data.settings().withPlacementMode(mode));
            var before = data.settings();
            var sounds = new PortalSoundSettings(PortalSoundRegistry.APERTURE_ISH_ID,
                PortalSoundRegistry.RIFT_ID, PortalSoundRegistry.ENDER_ID, true);
            CompoundTag request = request(sounds);
            CompoundTag[] reply = new CompoundTag[1];
            try (var store = mockStatic(PortalDataStore.class);
                 var locator = mockStatic(PortalGunLocator.class);
                 var capabilities = mockStatic(PortalGunCapabilities.class);
                 var network = mockStatic(RiftNetwork.class)) {
                store.when(() -> PortalDataStore.load(player)).thenReturn(data);
                locator.when(() -> PortalGunLocator.resolveReference(eq(player), any()))
                    .thenReturn(Optional.of(mock(PortalGunLocator.LocatedGun.class)));
                network.when(() -> RiftNetwork.sendToPlayer(eq(player), any(PortalResponsePayload.class)))
                    .thenAnswer(call -> { reply[0] = ((PortalResponsePayload) call.getArgument(1)).data(); return null; });
                PortalRequestHandler.handle(player, request);
                assertEquals(before.withPortalSounds(sounds), data.settings());
                assertEquals("", Nbt.getString(reply[0], "Error"));
                assertEquals("PortalSounds", Nbt.getString(reply[0], "Kind"));
                assertEquals("sound-request", Nbt.getString(reply[0], "RequestId"));
                assertEquals(sounds.save(), Nbt.getCompound(reply[0], "PortalSounds"));
                capabilities.verifyNoInteractions();
                store.verify(() -> PortalDataStore.save(player, data));
            }
        }
    }

    @Test
    void rejectionAcknowledgesTheRequestAndReturnsAuthoritativeSoundsWithoutSaving() {
        ServerPlayer player = mock(ServerPlayer.class);
        var data = new PortalPlayerData();
        var before = data.settings();
        CompoundTag[] reply = new CompoundTag[1];
        try (var store = mockStatic(PortalDataStore.class);
             var locator = mockStatic(PortalGunLocator.class);
             var network = mockStatic(RiftNetwork.class)) {
            store.when(() -> PortalDataStore.load(player)).thenReturn(data);
            locator.when(() -> PortalGunLocator.resolveReference(eq(player), any())).thenReturn(Optional.empty());
            network.when(() -> RiftNetwork.sendToPlayer(eq(player), any(PortalResponsePayload.class)))
                .thenAnswer(call -> { reply[0] = ((PortalResponsePayload) call.getArgument(1)).data(); return null; });
            PortalRequestHandler.handle(player, request(PortalSoundSettings.defaults()));
            assertEquals("screen.riftgun.appearance.invalid_gun", Nbt.getString(reply[0], "Error"));
            assertEquals("sound-request", Nbt.getString(reply[0], "RequestId"));
            assertEquals(before.portalSounds().save(), Nbt.getCompound(reply[0], "PortalSounds"));
            assertEquals(before, data.settings());
            store.verify(() -> PortalDataStore.save(any(), any()), never());
        }
    }

    private static CompoundTag request(PortalSoundSettings sounds) {
        var request = new CompoundTag();
        request.putString("Action", "SET_PORTAL_SOUNDS");
        request.putString("RequestId", "sound-request");
        request.put("GunReference", new CompoundTag());
        request.put("PortalSounds", sounds.save());
        return request;
    }
}
