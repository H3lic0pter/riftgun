package dev.riftgun.fuel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.riftgun.module.PortalGunModuleSettings;
import dev.riftgun.module.PortalGunModules;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.pairing.PortalFunctionMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Actual settings, module-save and visual derivation paths with inventory/loader boundaries. */
final class PortalGunModeIndicatorTest {
    private final List<AutoCloseable> boundaries = new ArrayList<>();
    private ItemStack gun;
    private PortalGunModuleSettings settings;
    private PortalGunVisualState visual;
    private PortalGunModules.ActiveCounts active;
    private boolean pairingInstalled;

    private <T> MockedStatic<T> boundary(Class<T> type) {
        var result = mockStatic(type);
        boundaries.add(result);
        return result;
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

        gun = mock(ItemStack.class);
        settings = PortalGunModuleSettings.defaults(8);
        visual = new PortalGunVisualState(0, false, PortalFuelProfiles.DIMENSIONAL_RGB);
        pairingInstalled = true;
        when(gun.get(PortalGunComponents.MODULE_SETTINGS)).thenAnswer(ignored -> settings);
        when(gun.getOrDefault(eq(PortalGunComponents.MODULE_SETTINGS), any(PortalGunModuleSettings.class)))
            .thenAnswer(ignored -> settings);
        doAnswer(call -> { settings = call.getArgument(1); return null; })
            .when(gun).set(eq(PortalGunComponents.MODULE_SETTINGS), any(PortalGunModuleSettings.class));
        when(gun.get(PortalGunComponents.VISUAL_STATE)).thenAnswer(ignored -> visual);
        doAnswer(call -> { visual = call.getArgument(1); return null; })
            .when(gun).set(eq(PortalGunComponents.VISUAL_STATE), any(PortalGunVisualState.class));

        var rules = PortalModuleRules.defaults();
        boundary(PortalModuleRules.class).when(PortalModuleRules::current).thenReturn(rules);
        active = mock(PortalGunModules.ActiveCounts.class);
        when(active.count(PortalModuleKind.PORTAL_PAIRING)).thenAnswer(ignored -> pairingInstalled ? 1 : 0);
        var modules = boundary(PortalGunModules.class);
        modules.when(() -> PortalGunModules.activeCounts(eq(gun), any())).thenReturn(active);
        modules.when(() -> PortalGunModules.save(eq(gun), any())).thenCallRealMethod();
        boundary(PortalFuelManager.class).when(() -> PortalFuelManager.hasInfiniteFuel(gun)).thenReturn(false);
        boundary(PortalFuelProfiles.class);
        boundaries.add(mockConstruction(PortalGunTank.class, (tank, context) -> {
            when(tank.getFluid()).thenReturn(FluidStack.EMPTY);
            when(tank.nominalCapacity()).thenReturn(8000);
        }));
    }

    @AfterEach
    void cleanup() throws Exception {
        for (int index = boundaries.size() - 1; index >= 0; index--) boundaries.get(index).close();
    }

    private void select(PortalFunctionMode mode) {
        settings.withPortalPairing(settings.portalPairing().withFunctionMode(mode)).save(gun);
    }

    @Test
    void modeSwitchAndModuleRemovalRefreshTheEffectiveIndicator() {
        select(PortalFunctionMode.PORTAL_PAIRING);
        assertTrue(visual.pairingMode());
        select(PortalFunctionMode.COORDINATE_TRAVEL);
        assertFalse(visual.pairingMode());
        select(PortalFunctionMode.PORTAL_PAIRING);
        assertTrue(visual.pairingMode());

        pairingInstalled = false;
        PortalGunModules.save(gun, NonNullList.withSize(PortalGunModules.SLOT_COUNT, ItemStack.EMPTY));
        assertFalse(visual.pairingMode());
        assertEquals(PortalFunctionMode.PORTAL_PAIRING, settings.portalPairing().functionMode());

        pairingInstalled = true;
        PortalGunModules.save(gun, NonNullList.withSize(PortalGunModules.SLOT_COUNT, ItemStack.EMPTY));
        assertTrue(visual.pairingMode());
    }

    @Test
    void repeatedRenderReadsDoNotResolveModulesOrRewriteVisualState() {
        select(PortalFunctionMode.PORTAL_PAIRING);
        var cached = visual;
        clearInvocations(active, gun);
        for (int frame = 0; frame < 100; frame++) assertSame(cached, PortalGunVisualState.current(gun));
        verifyNoInteractions(active);
        verify(gun, never()).set(eq(PortalGunComponents.VISUAL_STATE), any(PortalGunVisualState.class));
    }

    @Test
    void migratedSnapshotDerivesModeOnceAndUnrelatedSettingsKeepCache() {
        settings = settings.withPortalPairing(settings.portalPairing().withFunctionMode(PortalFunctionMode.PORTAL_PAIRING));
        visual = PortalGunVisualState.UNINITIALIZED;
        PortalGunVisualState.ensureInitialized(gun);
        assertTrue(visual.initialized());
        assertTrue(visual.pairingMode());
        var cached = visual;
        settings.withSmartDistance(12).save(gun);
        assertSame(cached, visual);
    }
}
