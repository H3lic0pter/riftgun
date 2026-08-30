package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class SurfaceFacePairingModifierSourceTest {
    @Test
    void surfaceFaceShortcutCarriesTheSneakChoiceToTheServer() throws Exception {
        String input = Files.readString(Path.of("src", "main", "java",
            "dev", "riftgun", "client", "ModeRadialInput.java"));
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String radial = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "screen", "ModeRadialScreen.java"));
            assertTrue(radial.contains("boolean endpointA = ModeRadialInput.sneakDown();"),
                version + " must sample Sneak while the radial Screen is still open");
            assertTrue(radial.contains("tag.putBoolean(\"EndpointA\""),
                version + " must capture the modifier in the shortcut packet");
        }
        assertTrue(input.contains("return keyDown(Minecraft.getInstance().options.keyShift);"),
            "precision radial must honor the configured Sneak binding inside a Screen");

        String handler = Files.readString(Path.of(
            "src/main/java/dev/riftgun/network/PortalRequestHandler.java"));
        int methodStart = handler.indexOf("private static void openSelectedSurfaceFace");
        int methodEnd = handler.indexOf("private static void sendChangedState", methodStart);
        String method = handler.substring(methodStart, methodEnd);
        assertTrue(handler.contains("SurfaceFaceRequest.decode(request),\n"
            + "                    Nbt.getBoolean(request, \"EndpointA\")"));
        assertTrue(method.contains("endpointA ? PortalPairingEndpoint.A : PortalPairingEndpoint.B"));
        assertFalse(method.contains("player.isShiftKeyDown()"));
    }
}
