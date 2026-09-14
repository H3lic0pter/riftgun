package dev.riftgun.appearance;

import dev.riftgun.core.config.GunShotAnimation;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.core.network.RiftNetwork;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.network.PortalRequestHandler;
import dev.riftgun.network.PortalResponsePayload;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.sound.PortalSoundRegistry;
import dev.riftgun.sound.PortalSoundSettings;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

final class GunPresentationRequestsTest {
    @org.junit.jupiter.api.BeforeAll
    static void bootstrap() { GunPresentationDefaultsTest.bootstrap(); }

    @Test
    void soundEditChangesOnlyTheReferencedGunAndDoesNotWritePlayerSettings() {
        var player = GunPresentationDefaultsTest.player();
        var original = GunPresentation.DEFAULT.withVisual("riftgun:endframe").withAnimation(GunShotAnimation.LOWER);
        var saved = new AtomicReference<>(original);
        var stack = GunPresentationDefaultsTest.stack(saved);
        var other = mock(net.minecraft.world.item.ItemStack.class);
        when(player.getMainHandItem()).thenReturn(other);
        var sounds = new PortalSoundSettings(PortalSoundRegistry.APERTURE_ISH_ID,
            PortalSoundRegistry.RIFT_ID, PortalSoundRegistry.ENDER_ID, true);
        CompoundTag request = new CompoundTag();
        request.putString("Action", "SET_GUN_PRESENTATION");
        request.putString("Category", "SOUNDS");
        request.putString("RequestId", "edit-one");
        request.put("GunReference", new CompoundTag());
        request.put("Presentation", GunPresentation.DEFAULT.withSounds(sounds).save());
        try (var locator = mockStatic(PortalGunLocator.class);
             var store = mockStatic(PortalDataStore.class);
             var network = mockStatic(RiftNetwork.class)) {
            locator.when(() -> PortalGunLocator.resolveReference(eq(player), any())).thenReturn(
                Optional.of(new PortalGunLocator.LocatedGun("inventory", new CompoundTag(), stack)));
            PortalRequestHandler.handle(player, request);
            assertEquals(original.withSounds(sounds), saved.get());
            verifyNoInteractions(other);
            verify(player, never()).getMainHandItem();
            store.verifyNoInteractions();
        }
    }

    @Test
    void staleReferenceIsRejectedWithoutFallingBackToAnotherGun() {
        var player = GunPresentationDefaultsTest.player();
        CompoundTag request = new CompoundTag();
        request.putString("Action", "SET_GUN_PRESENTATION");
        request.putString("RequestId", "stale");
        request.put("GunReference", new CompoundTag());
        var reply = new AtomicReference<CompoundTag>();
        try (var locator = mockStatic(PortalGunLocator.class);
             var network = mockStatic(RiftNetwork.class)) {
            locator.when(() -> PortalGunLocator.resolveReference(eq(player), any())).thenReturn(Optional.empty());
            network.when(() -> RiftNetwork.sendToPlayer(eq(player), any(PortalResponsePayload.class)))
                .thenAnswer(call -> { reply.set(call.<PortalResponsePayload>getArgument(1).data()); return null; });
            PortalRequestHandler.handle(player, request);
            assertEquals("screen.riftgun.appearance.invalid_gun", Nbt.getString(reply.get(), "Error"));
            assertEquals("stale", Nbt.getString(reply.get(), "RequestId"));
            locator.verify(() -> PortalGunLocator.first(any()), never());
        }
    }
}
