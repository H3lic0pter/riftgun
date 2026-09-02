package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalPreviewDepthPipelineSourceTest {
    @Test
    void legacyPreviewLinesUseFixedWidthReadOnlyWorldDepth() throws Exception {
        String renderTypes = read("1.21.1", "PortalRenderTypes.java");
        String preview = read("1.21.1", "PortalPlacementPreview.java");
        String clientEvents = Files.readString(Path.of("versions", "1.21.1", "src", "main",
            "java", "dev", "riftgun", "client", "ClientModEvents.java"));
        int markerStart = renderTypes.indexOf("private static final RenderType PREVIEW_LINES");
        int markerEnd = renderTypes.indexOf("private static final RenderType SWIRL", markerStart);
        String marker = renderTypes.substring(markerStart, markerEnd);

        assertTrue(marker.contains("OptionalDouble.of(2.5)"));
        assertTrue(marker.contains(".setDepthTestState(LEQUAL_DEPTH_TEST)"));
        assertTrue(marker.contains(".setWriteMaskState(COLOR_WRITE)"));
        assertTrue(marker.contains(".setTransparencyState(NO_TRANSPARENCY)"));
        assertTrue(marker.contains(".setCullState(NO_CULL)"));
        assertTrue(marker.contains(".setOutputState(MAIN_TARGET)"));
        assertFalse(marker.contains(".setOutputState(ITEM_ENTITY_TARGET)"));
        assertTrue(marker.contains("new ShaderStateShard(() -> previewLinesShader)"));
        assertFalse(marker.contains("RENDERTYPE_LINES_SHADER"));
        assertTrue(clientEvents.contains("rendertype_rift_pairing_marker"));
        assertTrue(preview.contains("RenderLevelStageEvent.Stage.AFTER_LEVEL"));
        assertTrue(preview.contains("RenderSystem.getModelViewStack()"));
        assertTrue(preview.contains(".mul(event.getModelViewMatrix())"));
        assertTrue(preview.contains("PortalRenderTypes.previewLines()"));
        assertFalse(preview.contains("RenderType.lines()"));
        assertTrue(preview.indexOf("frame.segments()")
            < preview.indexOf("frame.pendingSegments()"));
        assertTrue(preview.contains("color | 0xFF000000"));
    }

    @Test
    void modernPreviewLinesUseOpaquePostCompositeWorldDepth() throws Exception {
        String renderTypes = read("26.1.2", "PortalRenderTypes.java");
        String preview = read("26.1.2", "PortalPlacementPreview.java");
        String clientEvents = Files.readString(Path.of("versions", "26.1.2", "src", "main",
            "java", "dev", "riftgun", "client", "ClientModEvents.java"));

        assertFalse(preview.contains("RenderTypes.lines()"));
        assertTrue(preview.contains("state.frame().segments()"));
        assertTrue(preview.contains("state.frame().pendingSegments()"));
        assertTrue(preview.contains("state.frame().entityTargetSegments()"));
        assertTrue(preview.contains("RenderLevelStageEvent.AfterLevel"));
        assertTrue(preview.contains("RenderSystem.getModelViewStack()"));
        assertTrue(preview.contains(".mul(event.getModelViewMatrix())"));
        assertTrue(preview.contains("PortalRenderTypes.previewLines()"));
        assertTrue(preview.contains(".setLineWidth(2.5F)"));
        assertTrue(preview.contains("color | 0xFF000000"));
        assertTrue(preview.indexOf("state.frame().segments()")
            < preview.indexOf("state.frame().pendingSegments()"));
        assertFalse(preview.contains("RenderGuiEvent"));
        assertFalse(preview.contains("PairingMarkerOverlay"));
        assertFalse(preview.contains("gameRenderer.getMainCamera()"));
        assertFalse(preview.contains("shaderPackActive"));
        assertTrue(renderTypes.contains("public static final RenderPipeline PREVIEW_LINES"));
        assertTrue(renderTypes.contains("RenderPipelines.MATRICES_PROJECTION_SNIPPET"));
        assertTrue(renderTypes.contains("RenderPipelines.GLOBALS_SNIPPET"));
        assertTrue(renderTypes.contains("core/rendertype_rift_pairing_marker"));
        assertTrue(renderTypes.contains("withColorTargetState(ColorTargetState.DEFAULT)"));
        assertTrue(renderTypes.contains(
            "new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false)"));
        assertTrue(clientEvents.contains(
            "event.registerPipeline(PortalRenderTypes.Pipelines.PREVIEW_LINES)"));
        assertTrue(Files.exists(Path.of("versions", "26.1.2", "src", "main", "resources",
            "assets", "riftgun", "shaders", "core", "rendertype_rift_pairing_marker.vsh")));
        assertTrue(Files.exists(Path.of("versions", "26.1.2", "src", "main", "resources",
            "assets", "riftgun", "shaders", "core", "rendertype_rift_pairing_marker.fsh")));
        assertFalse(Files.exists(Path.of("versions", "26.1.2", "src", "main", "java",
            "dev", "riftgun", "client", "render", "PairingMarkerOverlay.java")));
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
    void lineExpansionPreservesWorldDepthExactlyInBothVersions() throws Exception {
        for (Path shader : new Path[] {
            Path.of("src", "main", "resources", "assets", "minecraft", "shaders", "core",
                "rendertype_rift_pairing_marker.vsh"),
            Path.of("versions", "26.1.2", "src", "main", "resources", "assets", "riftgun",
                "shaders", "core", "rendertype_rift_pairing_marker.vsh")
        }) {
            String source = Files.readString(shader);
            assertFalse(source.contains("VIEW_SHRINK"));
            assertFalse(source.contains("VIEW_SCALE"));
            assertFalse(source.contains("fog"));
            assertTrue(source.contains("ProjMat * ModelViewMat * vec4(Position, 1.0)"));
        }
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
