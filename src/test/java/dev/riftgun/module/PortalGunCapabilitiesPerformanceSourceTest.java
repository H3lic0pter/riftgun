package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void moduleContainerResolvesOldAndNewCountsOncePerChange() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/dev/riftgun/module/PortalGunModuleContainer.java"));
        int start = source.indexOf("public void setChanged()");
        int end = source.indexOf("private NonNullList<ItemStack> copyItems()", start);
        String setChanged = source.substring(start, end);

        assertEquals(2, occurrences(setChanged, "PortalGunModules.activeCounts("));
        assertFalse(setChanged.contains("activeCount("));
    }

    @Test
    void moduleMenuDataReusesMetricsUntilTheComponentChanges() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/dev/riftgun/module/PortalModuleMenu.java"));
        int start = source.indexOf("private static ContainerData serverData(ItemStack gun)");
        int end = source.indexOf("private static CompoundTag readReference", start);
        String serverData = source.substring(start, end);

        assertEquals(1, occurrences(serverData, "PortalGunModules.load(gun)"));
        assertTrue(serverData.contains(
            "if (component == cachedComponent && rules.equals(cachedRules)) return;"));
    }

    private static int occurrences(String source, String token) {
        return source.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }
}
