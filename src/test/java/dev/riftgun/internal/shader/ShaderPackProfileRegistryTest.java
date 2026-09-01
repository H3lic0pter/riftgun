package dev.riftgun.internal.shader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

final class ShaderPackProfileRegistryTest {
    @Test
    void registersBothComplementaryR5FamiliesForNativeEndPortalMaterial() {
        assertNativeEndPortal("ComplementaryReimagined_r5.8.1.zip");
        assertNativeEndPortal("ComplementaryUnbound_r5.8.1.zip");
        assertNativeEndPortal("D:\\shaderpacks\\COMPLEMENTARYUNBOUND_R5.9.zip");
    }

    @Test
    void leavesUnknownAndFutureMajorPacksEmpty() {
        assertSame(ShaderPackProfile.EMPTY, ShaderPackProfileRegistry.resolve(null));
        assertSame(ShaderPackProfile.EMPTY, ShaderPackProfileRegistry.resolve("BSL_v10.zip"));
        assertSame(ShaderPackProfile.EMPTY,
            ShaderPackProfileRegistry.resolve("ComplementaryReimagined_r6.0.zip"));
    }

    private static void assertNativeEndPortal(String packName) {
        ShaderPackProfile.EndframeCenter center =
            ShaderPackProfileRegistry.resolve(packName).endframeCenter();
        assertEquals(ShaderPackProfile.EndframeCenter.Mode.IRIS_BLOCK_ENTITY, center.mode());
        assertEquals(5025, center.materialId());
    }
}
