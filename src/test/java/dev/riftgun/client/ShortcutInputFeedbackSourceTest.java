package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ShortcutInputFeedbackSourceTest {
    private static final List<String> VERSIONS = List.of("1.21.1", "26.1.2");

    @Test
    void radialWaitsForServerApprovalBeforeOpening() throws Exception {
        for (String version : VERSIONS) {
            String source = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "ModeRadialInput.java"));
            String request = method(source, "private static void request(",
                "private static boolean sourceDown(");
            String approved = method(source, "public static void openFromServer(",
                "public static void rejectFromServer(");
            String pending = method(source, "if (pendingSource != null) {",
                "if (radialDown && !radialWasDown)");

            assertFalse(request.contains("setScreen("),
                version + " opens the radial before the server validates the gun");
            assertTrue(approved.contains("setScreen(new ModeRadialScreen())"),
                version + " must open the radial after server approval");
            assertTrue(pending.contains("REQUEST.release()"),
                version + " must preserve a request released before server approval");
        }
    }

    @Test
    void missingGunFeedbackIncludesKeyboardShortcuts() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/dev/riftgun/network/PortalRequestHandler.java"));
        String missingGun = method(source, "if (gun == null) {",
            "if (action == PortalAction.OPEN_GUI) {");

        assertTrue(missingGun.contains("if (keyboardShortcut || action == PortalAction.OPEN_GUI)"),
            "all Portal Gun keyboard shortcuts must show missing-gun feedback");
    }

    @Test
    void pairingSettingsUsesTheMainFunctionIcon() throws Exception {
        for (String version : VERSIONS) {
            String source = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "screen", "PortalConfigScreen.java"));
            String icons = method(source, "private void renderGunSettingEntries(",
                "private void renderGunSettingTooltips(");

            assertTrue(icons.contains("drawFunctionModeIcon(graphics, portalPairingSettingsButton.getX()"),
                version + " pairing settings must reuse the main pairing icon");
            assertFalse(icons.contains("A↔B"),
                version + " still renders the retired pairing text icon");
        }
    }

    private static String method(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + startMarker.length());
        assertTrue(start >= 0 && end > start, "Could not isolate source contract");
        return source.substring(start, end);
    }
}
