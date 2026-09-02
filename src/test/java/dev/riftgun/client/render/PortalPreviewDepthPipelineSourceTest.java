package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalPreviewDepthPipelineSourceTest {
    @Test
    void legacyPairingLinesUseFixedWidthReadOnlyWorldDepth() throws Exception {
        String renderTypes = read("1.21.1", "PortalRenderTypes.java");
        String preview = read("1.21.1", "PortalPlacementPreview.java");
        String clientEvents = Files.readString(Path.of("versions", "1.21.1", "src", "main",
            "java", "dev", "riftgun", "client", "ClientModEvents.java"));
        int markerStart = renderTypes.indexOf("private static final RenderType PAIRING_MARKER");
        int markerEnd = renderTypes.indexOf("private static final RenderType SWIRL", markerStart);
        String marker = renderTypes.substring(markerStart, markerEnd);

        assertTrue(marker.contains("OptionalDouble.of(2.5)"));
        assertTrue(marker.contains(".setDepthTestState(LEQUAL_DEPTH_TEST)"));
        assertTrue(marker.contains(".setWriteMaskState(COLOR_WRITE)"));
        assertTrue(marker.contains(".setCullState(NO_CULL)"));
        assertTrue(marker.contains(".setOutputState(MAIN_TARGET)"));
        assertFalse(marker.contains(".setOutputState(ITEM_ENTITY_TARGET)"));
        assertTrue(marker.contains("new ShaderStateShard(() -> pairingMarkerShader)"));
        assertFalse(marker.contains("RENDERTYPE_LINES_SHADER"));
        assertTrue(clientEvents.contains("rendertype_rift_pairing_marker"));
        assertTrue(preview.contains("RenderLevelStageEvent.Stage.AFTER_LEVEL"));
        assertTrue(preview.contains("RenderSystem.getModelViewStack()"));
        assertTrue(preview.contains(".mul(event.getModelViewMatrix())"));
        assertTrue(preview.contains("PortalRenderTypes.pairingMarker()"));
    }

    @Test
    void modernPairingLinesUsePostProcessedOpaqueGuiGeometryWithShaders() throws Exception {
        String renderTypes = read("26.1.2", "PortalRenderTypes.java");
        String preview = read("26.1.2", "PortalPlacementPreview.java");
        String overlay = read("26.1.2", "PairingMarkerOverlay.java");
        String clientEvents = Files.readString(Path.of("versions", "26.1.2", "src", "main",
            "java", "dev", "riftgun", "client", "ClientModEvents.java"));

        assertTrue(preview.contains("RenderTypes.lines()"));
        assertTrue(preview.contains("state.frame().pendingSegments()"));
        assertTrue(preview.contains("state.frame().entityTargetSegments()"));
        assertTrue(preview.contains("PortalRenderFrameState.current().shaderPackActive()"));
        assertTrue(preview.contains("if (!state.shaderPackActive())"));
        assertTrue(preview.contains("RenderGuiEvent.Pre"));
        assertTrue(preview.contains("submitGuiElementRenderState"));
        assertTrue(preview.contains("PairingMarkerOverlay.project"));
        assertTrue(preview.contains("minecraft.gameRenderer.getMainCamera().position()"));
        assertTrue(preview.contains("minecraft.level.clip"));
        assertTrue(preview.contains(".setLineWidth(2.5F)"));
        assertTrue(preview.contains("colored.color() | 0xFF000000"));
        assertTrue(overlay.contains("implements GuiElementRenderState"));
        assertTrue(overlay.contains("RenderPipelines.GUI"));
        assertTrue(overlay.contains("TextureSetup.noTexture()"));
        assertTrue(overlay.contains("colored.color() | 0xFF000000"));
        assertFalse(preview.contains("RenderLevelStageEvent.AfterLevel"));
        assertFalse(preview.contains("PortalRenderTypes.pairingMarker()"));
        assertFalse(overlay.contains("RenderPipeline.builder"));
        assertFalse(renderTypes.contains("public static final RenderPipeline PAIRING_MARKER"));
        assertFalse(renderTypes.contains("core/rendertype_rift_pairing_marker"));
        assertFalse(clientEvents.contains(
            "event.registerPipeline(PortalRenderTypes.Pipelines.PAIRING_MARKER)"));
        assertFalse(Files.exists(Path.of("versions", "26.1.2", "src", "main", "resources",
            "assets", "riftgun", "shaders", "core", "rendertype_rift_pairing_marker.vsh")));
        assertFalse(Files.exists(Path.of("versions", "26.1.2", "src", "main", "resources",
            "assets", "riftgun", "shaders", "core", "rendertype_rift_pairing_marker.fsh")));
    }

    @Test
    void legacyPostCompositeShaderOutputsUnlitUnfoggedVertexColor() throws Exception {
        String legacy = Files.readString(Path.of("src", "main", "resources", "assets",
            "minecraft", "shaders", "core", "rendertype_rift_pairing_marker.fsh"));

        assertTrue(legacy.contains("vertexColor * ColorModulator"));
        assertFalse(legacy.contains("fog"));
        assertFalse(legacy.contains("light"));
    }

    @Test
    void pairingLinesNeedNoTextureOrEntityVisualAdapter() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String preview = read(version, "PortalPlacementPreview.java");
            assertFalse(preview.contains("PortalPreviewVisualSource"));
            assertFalse(preview.contains("PortalVisualDispatcher"));
            assertFalse(preview.contains("Billboard"));
        }
    }

    private static String read(String version, String file) throws Exception {
        return Files.readString(Path.of("versions", version, "src", "main", "java", "dev",
            "riftgun", "client", "render", file));
    }
}
