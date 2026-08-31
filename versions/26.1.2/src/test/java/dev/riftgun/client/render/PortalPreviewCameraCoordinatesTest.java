package dev.riftgun.client.render;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalPreviewCameraCoordinatesTest {
    @Test
    void fixedWorldPointMovesSmoothlyOppositeToCameraAtLargeCoordinates() {
        Vec3 point = new Vec3(29_999_999.75, 80.5, -29_999_995.25);
        Vec3 firstCamera = new Vec3(29_999_995.62655, 78.0, -29_999_999.37345);
        Vec3 secondCamera = firstCamera.add(0.01, 0.0, -0.01);

        float firstX = PortalPreviewCameraCoordinates.relativeTo(firstCamera.x, point.x);
        float secondX = PortalPreviewCameraCoordinates.relativeTo(secondCamera.x, point.x);
        float firstZ = PortalPreviewCameraCoordinates.relativeTo(firstCamera.z, point.z);
        float secondZ = PortalPreviewCameraCoordinates.relativeTo(secondCamera.z, point.z);

        assertEquals(-0.01, secondX - firstX, 1.0E-6);
        assertEquals(0.01, secondZ - firstZ, 1.0E-6);
    }

    @Test
    void modernRendererSubtractsCameraBeforeConvertingVerticesToFloat() throws Exception {
        String source = Files.readString(Path.of("versions", "26.1.2", "src", "main",
            "java", "dev", "riftgun", "client", "render", "PortalPlacementPreview.java"));

        assertTrue(source.contains("PortalPreviewCameraCoordinates.relativeTo(camera.x, point.x)"));
        assertFalse(source.contains("poses.translate(-state.camera()"));
    }
}
