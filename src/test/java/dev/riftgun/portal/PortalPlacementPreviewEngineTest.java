package dev.riftgun.portal;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.pairing.PortalPairingEndpoint;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingPendingEndpoint;
import dev.riftgun.service.SurfaceFaceSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalPlacementPreviewEngineTest {
    @Test
    void shiftRouteActivatesForPairingEntityMode() {
        assertTrue(PortalPlacementPreviewEngine.usesShiftRoutedPreview(gun(
            PortalFunctionMode.PORTAL_PAIRING, PortalPlacementMode.ENTITY_RELOCATION,
            PortalFloatingFallback.FRONT, false)));
    }

    @Test
    void shiftRouteActivatesForSmartRemoteFallback() {
        assertTrue(PortalPlacementPreviewEngine.usesShiftRoutedPreview(gun(
            PortalFunctionMode.COORDINATE_TRAVEL, PortalPlacementMode.SMART,
            PortalFloatingFallback.REMOTE, true)));
    }

    @Test
    void ordinaryRemoteModeDoesNotUseShiftRoute() {
        assertFalse(PortalPlacementPreviewEngine.usesShiftRoutedPreview(gun(
            PortalFunctionMode.COORDINATE_TRAVEL, PortalPlacementMode.REMOTE,
            PortalFloatingFallback.FRONT, true)));
    }

    @Test
    void emptyFrameReportsNoGeometry() {
        var frame = new PortalPlacementPreviewEngine.Frame(List.of(), List.of(), List.of());
        assertTrue(frame.isEmpty());
    }

    @Test
    void changingWorldIdentityForcesRemoteRefresh() {
        PortalPlacementPreviewEngine engine = new PortalPlacementPreviewEngine();
        FakeResolver resolver = new FakeResolver();
        PortalPlacementPreviewEngine.Gun gun = gun(
            PortalFunctionMode.COORDINATE_TRAVEL, PortalPlacementMode.REMOTE,
            PortalFloatingFallback.FRONT, true);
        Object firstLevel = new Object();

        engine.tick(input(firstLevel, 0L, true, false, gun, null), resolver);
        engine.tick(input(firstLevel, 1L, true, false, gun, null), resolver);
        assertEquals(1, resolver.remoteCalls);

        engine.tick(input(new Object(), 1L, true, false, gun, null), resolver);
        assertEquals(2, resolver.remoteCalls);
    }

    @Test
    void leavingPrecisionContextClearsPlacementAndGeometry() {
        PortalPlacementPreviewEngine engine = new PortalPlacementPreviewEngine();
        FakeResolver resolver = new FakeResolver();
        Object level = new Object();
        PortalPlacementPreviewEngine.Gun gun = gun(
            PortalFunctionMode.COORDINATE_TRAVEL, PortalPlacementMode.FRONT,
            PortalFloatingFallback.FRONT, false);

        engine.tick(input(level, 0L, false, false, gun,
            new PortalPlacementPreviewEngine.PrecisionTarget.Floating(
                PortalOrientation.VERTICAL)), resolver);

        assertSame(resolver.frontPlacement, engine.currentPlacement());
        assertFalse(engine.frame().isEmpty());

        engine.tick(input(level, 1L, false, false, gun, null), resolver);

        assertNull(engine.currentPlacement());
        assertTrue(engine.frame().isEmpty());
    }

    @Test
    void pendingAndEntityMarkersFollowModeAndVisibility() {
        PortalPlacementPreviewEngine engine = new PortalPlacementPreviewEngine();
        FakeResolver resolver = new FakeResolver();
        Object level = new Object();
        PortalPairingPendingEndpoint pair = pending(PortalPairingEndpoint.A);
        PortalPairingPendingEndpoint entity = pending(PortalPairingEndpoint.ENTITY_TARGET);

        engine.tick(input(level, 0L, false, false,
            gun(PortalFunctionMode.PORTAL_PAIRING, PortalPlacementMode.FRONT,
                PortalFloatingFallback.FRONT, false, pair), null), resolver);
        assertEquals(11, engine.frame().pendingSegments().size());
        assertTrue(engine.frame().entityTargetSegments().isEmpty());

        engine.tick(input(level, 1L, false, false,
            gun(PortalFunctionMode.PORTAL_PAIRING, PortalPlacementMode.ENTITY_RELOCATION,
                PortalFloatingFallback.FRONT, false, entity), null), resolver);
        assertTrue(engine.frame().pendingSegments().isEmpty());
        assertEquals(3, engine.frame().entityTargetSegments().size());

        resolver.markerVisible = false;
        engine.tick(input(level, 2L, false, false,
            gun(PortalFunctionMode.PORTAL_PAIRING, PortalPlacementMode.ENTITY_RELOCATION,
                PortalFloatingFallback.FRONT, false, entity), null), resolver);
        assertTrue(engine.frame().isEmpty());
    }

    @Test
    void shiftRoutedSurfaceFailureFallsBackToRemotePreview() {
        PortalPlacementPreviewEngine engine = new PortalPlacementPreviewEngine();
        FakeResolver resolver = new FakeResolver();
        resolver.surfaceHit = new PortalPlacementPreviewEngine.SurfaceHit(
            new SurfaceFaceSelection(BlockPos.ZERO, Direction.UP), 1.0);
        resolver.surfacePlacement = null;
        PortalPlacementPreviewEngine.Gun gun = gun(
            PortalFunctionMode.PORTAL_PAIRING, PortalPlacementMode.ENTITY_RELOCATION,
            PortalFloatingFallback.FRONT, false);

        engine.tick(input(new Object(), 0L, true, true, gun, null), resolver);

        assertEquals(1, resolver.surfaceCalls);
        assertEquals(1, resolver.remoteCalls);
        assertFalse(engine.frame().isEmpty());
    }

    private static PortalPlacementPreviewEngine.Gun gun(PortalFunctionMode function,
                                                         PortalPlacementMode placement,
                                                         PortalFloatingFallback fallback,
                                                         boolean remote) {
        return gun(function, placement, fallback, remote, null);
    }

    private static PortalPlacementPreviewEngine.Gun gun(
        PortalFunctionMode function, PortalPlacementMode placement,
        PortalFloatingFallback fallback, boolean remote,
        PortalPairingPendingEndpoint pending
    ) {
        return new PortalPlacementPreviewEngine.Gun(function, placement, fallback,
            16, 16, 64, PortalAperture.STANDARD, remote, true, pending);
    }

    private static PortalPlacementPreviewEngine.TickInput input(
        Object level, long tick, boolean clientIdle, boolean shiftDown,
        PortalPlacementPreviewEngine.Gun gun,
        PortalPlacementPreviewEngine.PrecisionTarget precisionTarget
    ) {
        return new PortalPlacementPreviewEngine.TickInput(level, tick, true, clientIdle,
            shiftDown, new PortalPlacementPreviewEngine.PlayerView(
                Vec3.ZERO, new Vec3(0.0, 0.0, 1.0), 0.0F, 0.0F), gun, precisionTarget);
    }

    private static PortalPairingPendingEndpoint pending(PortalPairingEndpoint endpoint) {
        //? if >=1.21.11 {
        /*ResourceKey<Level> dimension = ResourceKey.create(
            Registries.DIMENSION, Identifier.fromNamespaceAndPath("riftgun", "preview_test"));
        *///?} else {
        ResourceKey<Level> dimension = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("riftgun", "preview_test"));
        //?}
        return new PortalPairingPendingEndpoint(UUID.randomUUID(), UUID.randomUUID(),
            dimension, placement(), endpoint, 0L, 20);
    }

    private static PortalPlacement placement() {
        return new PortalPlacement(new Vec3(0.0, 1.0, 8.0), PortalOrientation.VERTICAL,
            PortalGeometry.FLOATING_VERTICAL, 0.0F, null, null);
    }

    private static final class FakeResolver implements PortalPlacementPreviewEngine.Resolver {
        private boolean markerVisible = true;
        private PortalPlacementPreviewEngine.SurfaceHit surfaceHit;
        private PortalPlacement surfacePlacement = placement();
        private final PortalPlacement frontPlacement = placement();
        private final PortalPlacement remotePlacement = placement();
        private int surfaceCalls;
        private int remoteCalls;

        @Override
        public boolean markerVisible(PortalPairingPendingEndpoint endpoint) {
            return markerVisible;
        }

        @Override
        public PortalPlacementPreviewEngine.SurfaceHit surfaceHit(int maximumRange) {
            return surfaceHit;
        }

        @Override
        public PortalPlacement surface(PortalPlacementPreviewEngine.Gun gun,
                                       SurfaceFaceSelection selection, double distance,
                                       int maximumRange) {
            surfaceCalls++;
            return surfacePlacement;
        }

        @Override
        public PortalPlacement front(PortalPlacementPreviewEngine.Gun gun,
                                     PortalOrientation orientation) {
            return frontPlacement;
        }

        @Override
        public PortalPlacement remote(int range, PortalAperture aperture,
                                      PortalOrientation orientation) {
            remoteCalls++;
            return remotePlacement;
        }
    }
}
