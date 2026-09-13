package dev.riftgun.network;

import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.core.network.RiftNetwork;
import dev.riftgun.fuel.PortalGunVisualState;
import dev.riftgun.service.PortalGunLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Runs the actual request handler while isolating inventory, transport and loader registries. */
final class PortalAppearanceRequestsTest {
    private final List<MockedStatic<?>> boundaries = new ArrayList<>();
    private ServerPlayer player;
    private Inventory inventory;
    private AbstractContainerMenu menu;
    private ItemStack target;
    private ItemStack other;
    private CompoundTag reference;
    private CompoundTag response;
    private MockedStatic<PortalGunLocator> locator;
    private MockedStatic<PortalGunSkin> skins;
    private String storedSkin;

    private <T> MockedStatic<T> boundary(Class<T> type) {
        MockedStatic<T> mocked = mockStatic(type);
        boundaries.add(mocked);
        return mocked;
    }

    @BeforeEach
    void setup() {
//? if >=1.21.11 {
        /*boundary(net.neoforged.fml.loading.FMLEnvironment.class)
            .when(net.neoforged.fml.loading.FMLEnvironment::isProduction).thenReturn(true);
*///?}
        boundary(net.neoforged.neoforge.common.util.flag.FeatureFlagLoader.class);
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        player = mock(ServerPlayer.class);
        inventory = mock(Inventory.class);
        menu = mock(AbstractContainerMenu.class);
        when(player.getInventory()).thenReturn(inventory);
        player.containerMenu = menu;
        target = mock(ItemStack.class);
        other = mock(ItemStack.class);
        when(player.getMainHandItem()).thenReturn(other);
        reference = new CompoundTag();
        reference.putString("Instance", "target-gun");
        var located = mock(PortalGunLocator.LocatedGun.class);
        when(located.stack()).thenReturn(target);
        when(located.saveReference()).thenReturn(reference.copy());
        locator = boundary(PortalGunLocator.class);
        locator.when(() -> PortalGunLocator.resolveReference(player, reference))
            .thenReturn(Optional.of(located));

        storedSkin = "example:existing";
        skins = boundary(PortalGunSkin.class);
        skins.when(() -> PortalGunSkin.validId(anyString())).thenCallRealMethod();
        skins.when(() -> PortalGunSkin.current(target)).thenAnswer(ignored -> storedSkin);
        skins.when(() -> PortalGunSkin.set(eq(target), anyString())).thenAnswer(call -> {
            storedSkin = call.getArgument(1);
            return null;
        });
        var visual = new PortalGunVisualState(4, true, 0x123456, true);
        boundary(PortalGunVisualState.class).when(() -> PortalGunVisualState.current(target))
            .thenReturn(visual);
        boundary(RiftNetwork.class).when(() -> RiftNetwork.sendToPlayer(eq(player), any(PortalResponsePayload.class)))
            .thenAnswer(call -> {
                response = ((PortalResponsePayload) call.getArgument(1)).data();
                return null;
            });
    }

    @AfterEach
    void cleanup() {
        for (int index = boundaries.size() - 1; index >= 0; index--) boundaries.get(index).close();
    }

    private CompoundTag request(String skin) {
        CompoundTag request = new CompoundTag();
        request.putString("RequestId", "appearance-request");
        request.put("GunReference", reference.copy());
        request.putString("Skin", skin);
        return request;
    }

    private void assertRejected(String error) {
        assertNotNull(response);
        assertEquals("appearance-request", Nbt.getString(response, "RequestId"));
        assertEquals(error, Nbt.getString(response, "Error"));
        assertFalse(response.contains("Skin"));
        assertEquals("example:existing", storedSkin);
        skins.verify(() -> PortalGunSkin.set(any(), anyString()), never());
        verifyNoInteractions(target, other, inventory, menu);
        locator.verify(() -> PortalGunLocator.first(any()), never());
        verify(player, never()).getMainHandItem();
    }

    @Test
    void missingReferenceNeverFallsBackToHeldGun() {
        CompoundTag request = request("example:new");
        request.remove("GunReference");
        PortalAppearanceRequests.handle(player, request, true);
        assertRejected("screen.riftgun.appearance.invalid_gun");
        locator.verifyNoInteractions();
    }

    @Test
    void expiredReferenceRejectsWithoutChangingAnotherGun() {
        locator.when(() -> PortalGunLocator.resolveReference(player, reference)).thenReturn(Optional.empty());
        PortalAppearanceRequests.handle(player, request("example:new"), true);
        assertRejected("screen.riftgun.appearance.invalid_gun");
        locator.verify(() -> PortalGunLocator.resolveReference(player, reference));
    }

    @Test
    void spectatorCannotApplyAppearance() {
        when(player.isSpectator()).thenReturn(true);
        PortalAppearanceRequests.handle(player, request("example:new"), true);
        assertRejected("message.riftgun.spectator_denied");
    }

    @Test
    void malformedIdIsRejectedBeforeMutation() {
        PortalAppearanceRequests.handle(player, request("Example:contains space"), true);
        assertRejected("screen.riftgun.appearance.invalid_skin");
    }

    @Test
    void validUnknownSkinIsAppliedOnlyToReferencedGun() {
        PortalAppearanceRequests.handle(player, request("client_pack:unknown_to_server"), true);
        assertEquals("", Nbt.getString(response, "Error"));
        assertEquals("client_pack:unknown_to_server", Nbt.getString(response, "Skin"));
        assertEquals("Appearance", Nbt.getString(response, "Kind"));
        assertTrue(Nbt.getBoolean(response, "Applied"));
        assertEquals(reference, Nbt.getCompound(response, "GunReference"));
        skins.verify(() -> PortalGunSkin.set(target, "client_pack:unknown_to_server"));
        skins.verify(() -> PortalGunSkin.set(eq(other), anyString()), never());
        verify(inventory).setChanged();
        verify(menu).broadcastChanges();
        verifyNoInteractions(other);
        locator.verify(() -> PortalGunLocator.first(any()), never());
        verify(player, never()).getMainHandItem();
    }

    @Test
    void openingReturnsCurrentSkinAndVisualsWithoutApplyingSelection() {
        assertOpeningVisuals(false);
    }

    @Test
    void openingPreservesTargetGlintInPreviewVisuals() {
        assertOpeningVisuals(true);
    }

    private void assertOpeningVisuals(boolean foil) {
        when(target.hasFoil()).thenReturn(foil);
        PortalAppearanceRequests.handle(player, request("example:unapplied"), false);
        assertEquals("", Nbt.getString(response, "Error"));
        assertFalse(Nbt.getBoolean(response, "Applied"));
        assertEquals("example:existing", Nbt.getString(response, "Skin"));
        assertEquals(4, Nbt.getInt(response, "LiquidTint"));
        assertTrue(Nbt.getBoolean(response, "CoreVisible"));
        assertEquals(0x123456, Nbt.getInt(response, "FuelRgb"));
        assertTrue(Nbt.getBoolean(response, "PairingMode"));
        assertEquals(foil, Nbt.getBoolean(response, "Foil"));
        skins.verify(() -> PortalGunSkin.set(any(), anyString()), never());
        verify(target).hasFoil();
        verifyNoMoreInteractions(target);
        verifyNoInteractions(other, inventory, menu);
    }
}
