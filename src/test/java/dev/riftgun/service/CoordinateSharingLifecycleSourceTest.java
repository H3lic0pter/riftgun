package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class CoordinateSharingLifecycleSourceTest {
    @Test
    void serverStopClearsCoordinateSharingSessionState() throws Exception {
        String lifecycle = Files.readString(Path.of(
            "src/main/java/dev/riftgun/lifecycle/RiftLifecycle.java"));
        String sharing = Files.readString(Path.of(
            "src/main/java/dev/riftgun/service/CoordinateSharingService.java"));

        assertTrue(lifecycle.contains("CoordinateSharingService.reset();"));
        assertTrue(sharing.contains("CHAT_SHARES.clear();"));
        assertTrue(sharing.contains("CHAT_COOLDOWNS.clear();"));
        assertTrue(sharing.contains("CHAT_COOLDOWNS.entrySet().removeIf"));
    }
}
