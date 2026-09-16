package dev.riftgun.appearance;

import dev.riftgun.core.config.GunShotAnimation;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.sound.PortalSoundRegistry;
import dev.riftgun.sound.PortalSoundSettings;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

final class GunPresentationDefaultsTest {
    @org.junit.jupiter.api.BeforeAll
    static void bootstrap() {
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
        }
    }
    @Test
    void newGunsCopyOwnerDefaultsOnceAndKeepValuesAfterTransferOrPresetChanges() {
        ServerPlayer alice = player();
        ServerPlayer bob = player();
        var original = GunPresentation.DEFAULT.withVisual("riftgun:endframe").withAnimation(GunShotAnimation.LOWER);
        GunPresentationDefaults.receive(alice, defaults(original, original));
        GunPresentationDefaults.receive(bob, defaults(GunPresentation.DEFAULT, GunPresentation.DEFAULT));
        var saved = new AtomicReference<>(GunPresentation.NEW);
        ItemStack gun = stack(saved);
        assertTrue(GunPresentationDefaults.initialize(alice, gun));
        assertEquals(original, saved.get());
        GunPresentationDefaults.receive(alice, defaults(GunPresentation.DEFAULT, GunPresentation.DEFAULT));
        GunPresentationDefaults.initialize(alice, gun);
        GunPresentationDefaults.initialize(bob, gun);
        assertEquals(original, saved.get());
        var another = new AtomicReference<>(GunPresentation.NEW);
        GunPresentationDefaults.initialize(alice, stack(another));
        assertEquals(GunPresentation.DEFAULT, another.get());
    }

    @Test
    void animationReadsEachSavedGunWithoutLoadingClientPreferences() {
        var first = stack(new AtomicReference<>(GunPresentation.DEFAULT.withAnimation(GunShotAnimation.SWING)));
        var second = stack(new AtomicReference<>(GunPresentation.DEFAULT.withAnimation(GunShotAnimation.OFF)));
        assertEquals(GunShotAnimation.SWING,
            dev.riftgun.client.appearance.SkinRecommendations.animation(first));
        assertEquals(GunShotAnimation.OFF,
            dev.riftgun.client.appearance.SkinRecommendations.animation(second));
    }

    @Test
    void oldGunUsesEffectiveLegacyChoicesAndServerSoundsWithoutReapplyingSkin() {
        ServerPlayer owner = player();
        var legacy = GunPresentation.DEFAULT.withVisual("riftgun:classic").withAnimation(GunShotAnimation.OFF);
        GunPresentationDefaults.receive(owner, defaults(GunPresentation.DEFAULT, legacy));
        var data = new PortalPlayerData();
        var sounds = new PortalSoundSettings(PortalSoundRegistry.APERTURE_ISH_ID,
            PortalSoundRegistry.ENDER_ID, PortalSoundRegistry.RIFT_ID, true);
        data.settings(data.settings().withPortalSounds(sounds));
        var saved = new AtomicReference<GunPresentation>();
        try (var store = mockStatic(PortalDataStore.class)) {
            store.when(() -> PortalDataStore.load(owner)).thenReturn(data);
            assertTrue(GunPresentationDefaults.initialize(owner, stack(saved)));
            assertEquals(legacy.withSounds(sounds), saved.get());
            store.verify(() -> PortalDataStore.save(any(), any()), never());
        }
    }

    @Test
    void missingClientHelloDoesNotPermanentlyWriteFallback() {
        var saved = new AtomicReference<>(GunPresentation.NEW);
        assertFalse(GunPresentationDefaults.initialize(player(), stack(saved)));
        assertEquals(GunPresentation.NEW, saved.get());
    }

    @Test
    void customOnlySkinsUseTheirSynchronizedPresetsForNewGuns() {
        ServerPlayer owner = player();
        var compact = GunPresentation.DEFAULT.withVisual("riftgun:classic")
            .withAnimation(GunShotAnimation.OFF);
        CompoundTag values = defaults(GunPresentation.DEFAULT, GunPresentation.DEFAULT);
        values.put("riftgun:pink_water_gun", compact.save());
        GunPresentationDefaults.receive(owner, values);

        var saved = new AtomicReference<>(GunPresentation.NEW);
        assertTrue(GunPresentationDefaults.initialize(owner, stack(saved, "riftgun:pink_water_gun")));
        assertEquals(compact, saved.get());
    }

    private static CompoundTag defaults(GunPresentation fresh, GunPresentation legacy) {
        var tag = new CompoundTag();
        tag.put("Custom", fresh.save());
        tag.put("Legacy", legacy.save());
        tag.put("riftgun:default", fresh.save());
        return tag;
    }

    static ServerPlayer player() {
        var player = mock(ServerPlayer.class);
        when(player.getInventory()).thenReturn(mock(Inventory.class));
        player.containerMenu = mock(AbstractContainerMenu.class);
        return player;
    }

    static ItemStack stack(AtomicReference<GunPresentation> value) {
        return stack(value, PortalGunSkin.DEFAULT);
    }

    private static ItemStack stack(AtomicReference<GunPresentation> value, String skin) {
        var stack = mock(ItemStack.class);
        when(stack.get(PortalGunComponents.PRESENTATION)).thenAnswer(call -> value.get());
        when(stack.has(PortalGunComponents.PRESENTATION)).thenAnswer(call -> value.get() != null);
        when(stack.getOrDefault(eq(PortalGunComponents.PRESENTATION), any()))
            .thenAnswer(call -> value.get() == null ? call.getArgument(1) : value.get());
        when(stack.getOrDefault(PortalGunComponents.SKIN, PortalGunSkin.DEFAULT)).thenReturn(skin);
        when(stack.set(eq(PortalGunComponents.PRESENTATION), any()))
            .thenAnswer(call -> value.getAndSet(call.getArgument(1)));
        return stack;
    }
}
