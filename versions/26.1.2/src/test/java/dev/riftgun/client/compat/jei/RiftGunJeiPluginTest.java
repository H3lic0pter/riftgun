package dev.riftgun.client.compat.jei;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import org.junit.jupiter.api.Test;

/**
 * Structural guard for the 26.1.2 JEI bridge: it must be present, implement
 * {@link IModPlugin}, and expose the fluid-transmutation registration seam.
 * The special crafting recipes (advanced basic module chain) are non-special
 * with a placeable placementInfo, so JEI collects them automatically and no
 * crafting seam is required.
 *
 * <p>Plain JUnit cannot initialize MC recipe types (their class initializers
 * require a bootstrapped registry set), so the plugin is loaded without
 * initialization via reflection and verified structurally; the actual rendering
 * is validated in-game. This test is red when the bridge is missing and green
 * once the seam exists, which is exactly the regression the 26.1.2 port
 * introduced.
 */
final class RiftGunJeiPluginTest {
    private static final String PLUGIN = "dev.riftgun.client.compat.jei.RiftGunJeiPlugin";

    @Test
    void pluginIsPresentAndImplementsIModPlugin() throws Exception {
        Class<?> type = Class.forName(PLUGIN, false, RiftGunJeiPluginTest.class.getClassLoader());
        assertTrue(IModPlugin.class.isAssignableFrom(type), "plugin must implement IModPlugin");
    }

    @Test
    void fluidTransmutationRegistrationSeamExists() throws Exception {
        Class<?> type = Class.forName(PLUGIN, false, RiftGunJeiPluginTest.class.getClassLoader());
        assertNotNull(type.getDeclaredMethod("registerTransmutationRecipes",
            IRecipeRegistration.class, List.class), "fluid transmutation seam missing");
    }
}
