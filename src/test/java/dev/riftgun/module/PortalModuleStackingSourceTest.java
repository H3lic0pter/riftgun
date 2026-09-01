package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalModuleStackingSourceTest {
    @Test
    void moduleStrengthUsesStackCountsAndStackLimitsUseServerRules() throws Exception {
        String modules = Files.readString(Path.of(
            "src/main/java/dev/riftgun/module/PortalGunModules.java"));
        String menu = Files.readString(Path.of(
            "src/main/java/dev/riftgun/module/PortalModuleMenu.java"));

        assertTrue(modules.contains("count += stack.getCount()"));
        assertTrue(modules.contains("expansions += stack.getCount()"));
        assertTrue(modules.contains("installed[definition.kind().ordinal()] += items.get(slot).getCount()"));
        assertTrue(menu.contains("definition.maximumCount") || menu.contains("remainingCapacity("));
        assertTrue(menu.contains(
            "canGrowStack(installed.getCount(), getMaxStackSize(stack))"));
        assertTrue(menu.contains("maximumRemovableCount("));
        assertTrue(menu.contains("buffer.writeNbt(PortalModuleRules.current().save())"));
        assertTrue(menu.contains("readRules(buffer)"));
    }

    @Test
    void onlyMultiCopyModulesReceiveStackableItems() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/dev/riftgun/module/PortalModules.java"));

        assertTrue(source.contains("RESERVOIR_EXPANSION = registerStackable("));
        assertTrue(source.contains("SURFACE_RANGE = registerStackable("));
        assertTrue(source.contains("MODULE_BAY_EXPANSION = registerStackable("));
        assertTrue(source.contains("DURATION_EXTENSION = registerStackable("));
    }
}
