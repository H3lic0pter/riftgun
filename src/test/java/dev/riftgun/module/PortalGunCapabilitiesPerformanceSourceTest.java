package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalGunCapabilitiesPerformanceSourceTest {
    @Test
    void capabilityResolutionMaterializesTheModuleContainerOnce() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/dev/riftgun/module/PortalGunCapabilities.java"));
        int start = source.indexOf("public static PortalGunCapabilities resolve(ItemStack gun");
        int end = source.indexOf("public PortalFloatingFallback activeSmartFallback()", start);
        String resolve = source.substring(start, end);

        assertEquals(1, occurrences(resolve, "PortalGunModules.activeCounts(gun, rules)"));
        assertFalse(resolve.contains("PortalGunModules.activeCount("));

        String modules = Files.readString(Path.of(
            "src/main/java/dev/riftgun/module/PortalGunModules.java"));
        int countsStart = modules.indexOf("public static ActiveCounts activeCounts(ItemStack gun");
        int countsEnd = modules.indexOf("public static final class ActiveCounts", countsStart);
        assertEquals(1, occurrences(modules.substring(countsStart, countsEnd), "load(gun)"));
    }

    @Test
    void previewReusesTheSettingsDecodedForCapabilities() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/dev/riftgun/client/PortalPreviewGunState.java"));
        int start = source.indexOf("public static @Nullable PortalPreviewGunState fromStack(");
        int end = source.indexOf("public static @Nullable PortalPreviewGunState fromSnapshot(", start);
        String fromStack = source.substring(start, end);

        assertEquals(1, occurrences(fromStack, "PortalGunModuleSettings.get(gun, smartDistance)"));
    }

    private static int occurrences(String source, String token) {
        return source.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }
}
