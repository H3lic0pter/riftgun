package dev.riftgun.client.render;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

final class PairingMarkerOverlayTest {
    @Test
    void mapsClipCoordinatesToGuiCoordinates() {
        PairingMarkerOverlay.ProjectedPoint point = PairingMarkerOverlay.projectPoint(
            new Vec3(-0.5, 0.5, 0.0), Vec3.ZERO, new Matrix4f(), 100, 80);

        assertNotNull(point);
        assertEquals(25.0F, point.x(), 0.0001F);
        assertEquals(20.0F, point.y(), 0.0001F);
    }

    @Test
    void rejectsPointsBehindPerspectiveCamera() {
        Matrix4f projection = new Matrix4f().perspective(
            (float) Math.toRadians(70.0), 16.0F / 9.0F, 0.05F, 1000.0F);

        assertNotNull(PairingMarkerOverlay.projectPoint(
            new Vec3(0.0, 0.0, -1.0), Vec3.ZERO, projection, 160, 90));
        assertNull(PairingMarkerOverlay.projectPoint(
            new Vec3(0.0, 0.0, 1.0), Vec3.ZERO, projection, 160, 90));
    }

    @Test
    void keepsLineWidthConstantInPhysicalPixelsAcrossGuiScales() {
        assertEquals(1.25F, PairingMarkerOverlay.halfLineWidth(1.0), 0.0001F);
        assertEquals(0.625F, PairingMarkerOverlay.halfLineWidth(2.0), 0.0001F);
        assertEquals(0.3125F, PairingMarkerOverlay.halfLineWidth(4.0), 0.0001F);
    }
}
