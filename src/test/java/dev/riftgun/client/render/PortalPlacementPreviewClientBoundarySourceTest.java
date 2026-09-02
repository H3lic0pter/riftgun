package dev.riftgun.client.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalPlacementPreviewClientBoundarySourceTest {
    @Test
    void bothPreviewAdaptersStayClientOnlyAndPacketFree() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", version, "src", "main",
                "java", "dev", "riftgun", "client", "render", "PortalPlacementPreview.java"));

            assertTrue(source.contains("value = Dist.CLIENT"));
            assertFalse(source.contains("PortalNetworking"));
            assertFalse(source.contains("PORTAL_SPLASH"));
            assertFalse(source.contains("addParticle"));
            assertFalse(source.contains("PortalEntity"));
            assertFalse(source.contains("PortalPreviewVisualSource"));
            assertFalse(source.contains("PortalVisualDispatcher"));
            assertFalse(source.contains("SurfaceFaceRequest"),
                version + " preview hot path must use the domain selection directly");
            assertFalse(source.contains(".toSelection()"),
                version + " preview hot path must not allocate a packet-to-domain wrapper");
            assertTrue(source.contains("ENGINE.tick(input(minecraft),"));
            assertFalse(source.contains("tickPrecision("));
            assertFalse(source.contains("tickShiftRoutedPreview("));
            assertFalse(source.contains("updateRemotePreview("));
        }
    }

    @Test
    void modernPreviewSeparatesPlacementSubmissionFromPostCompositePairing() throws Exception {
        String source = Files.readString(Path.of("versions", "26.1.2", "src", "main",
            "java", "dev", "riftgun", "client", "render", "PortalPlacementPreview.java"));
        int methodStart = source.indexOf("public static void submitCustomGeometry(");
        int methodEnd = source.indexOf("@SubscribeEvent", methodStart + 1);
        String method = source.substring(methodStart, methodEnd);

        assertEquals(1, occurrences(method, ".submitCustomGeometry("));
        assertTrue(method.contains("RenderTypes.lines()"));
        assertFalse(method.contains("state.frame().pendingSegments()"));
        assertFalse(method.contains("state.frame().entityTargetSegments()"));
        assertFalse(method.contains("drawColored("));
        assertTrue(source.contains("RenderLevelStageEvent.AfterLevel"));
        assertTrue(source.contains("PortalRenderTypes.pairingMarker()"));
        assertTrue(source.contains("MultiBufferSource.BufferSource"));
        assertTrue(source.contains(
            "PortalPreviewCoordinates.relativeTo(camera.x, point.x)"));
        assertFalse(source.contains("poses.translate(-state.camera()"));
        assertFalse(source.contains("PairingMarkerOverlay"));
        assertFalse(source.contains("gameRenderer.getMainCamera()"));
    }

    @Test
    void legacyPreviewKeepsTheSharedBatchButUsesCameraRelativeVertices() throws Exception {
        String source = Files.readString(Path.of("versions", "1.21.1", "src", "main",
            "java", "dev", "riftgun", "client", "render", "PortalPlacementPreview.java"));

        assertTrue(source.contains("minecraft.renderBuffers().bufferSource()"));
        assertTrue(source.contains(
            "PortalPreviewCoordinates.relativeTo(camera.x, point.x)"));
        assertTrue(source.contains("PortalRenderTypes.pairingMarker()"));
        assertTrue(source.contains("drawColored("));
        assertFalse(source.contains("ByteBufferBuilder"));
        assertFalse(source.contains("poses.translate(-camera.x"));
    }

    @Test
    void markerVisibilityUsesGpuDepthInsteadOfCpuRaycasts() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", version, "src", "main",
                "java", "dev", "riftgun", "client", "render", "PortalPlacementPreview.java"));
            int methodStart = source.indexOf("public boolean markerVisible(");
            int methodEnd = source.indexOf("public @Nullable PortalPlacementPreviewEngine.SurfaceHit",
                methodStart);
            String method = source.substring(methodStart, methodEnd);

            assertTrue(method.contains("hasChunkAt"));
            assertFalse(method.contains(".clip("));
            assertFalse(method.contains("getMainCamera"));
        }
    }

    @Test
    void modernAfterLevelUsesStateRetainedAcrossLevelRenderStateReset() throws Exception {
        String source = Files.readString(Path.of("versions", "26.1.2", "src", "main",
            "java", "dev", "riftgun", "client", "render", "PortalPlacementPreview.java"));
        int methodStart = source.indexOf(
            "public static void renderPairingMarker(RenderLevelStageEvent.AfterLevel event)");
        int methodEnd = source.indexOf("private static void drawLines(", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertTrue(source.contains("afterLevelState = state"));
        assertTrue(method.contains("RenderState state = afterLevelState"));
        assertFalse(method.contains("getRenderData(RENDER_STATE_KEY)"));
    }

    private static int occurrences(String source, String token) {
        return source.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }
}
