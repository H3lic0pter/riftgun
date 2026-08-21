package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class EndframeFaceUvTest {
    @Test
    void fallbackBackFaceCompensatesForReversedWinding() {
        assertEquals(0.0F, EndframeVisualGeometry.alignedFaceU(0.0F, false));
        assertEquals(1.0F, EndframeVisualGeometry.alignedFaceU(1.0F, false));
        assertEquals(1.0F, EndframeVisualGeometry.alignedFaceU(0.0F, true));
        assertEquals(0.0F, EndframeVisualGeometry.alignedFaceU(1.0F, true));
    }
}
