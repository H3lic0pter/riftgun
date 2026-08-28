package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RemoteModulePreferenceSourceTest {
    @Test
    void removingRemoteModuleKeepsSavedPreferenceAndRuntimeFallsBack() throws Exception {
        String container = Files.readString(Path.of(
            "src/main/java/dev/riftgun/module/PortalGunModuleContainer.java"));
        String coordinator = Files.readString(Path.of(
            "src/main/java/dev/riftgun/service/PortalOpenCoordinator.java"));

        int remoteRemoval = container.indexOf("if (oldActiveRemote > 0 && newActiveRemote == 0)");
        String remoteRemovalBlock = container.substring(remoteRemoval,
            container.indexOf("previous = copyItems();", remoteRemoval));
        assertFalse(remoteRemovalBlock.contains("withPlacementMode"));
        assertTrue(coordinator.contains("mode = gunCapabilities.effectivePlacementMode(mode);"));
    }
}
