package dev.riftgun.core.msg;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class MsgSourceContractTest {
    private static final Path SOURCE = Path.of("src/main/java/dev/riftgun/core/msg/Msg.java");

    @Test
    void modernBranchUsesPolymorphicPlayerMessaging() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("player.sendOverlayMessage(message);"));
        assertTrue(source.contains("player.sendSystemMessage(message);"));
        assertFalse(source.contains("(ServerPlayer) player"));
        assertFalse(source.contains("ClientboundSystemChatPacket"));
    }

    @Test
    void legacyBranchKeepsNativeDisplayClientMessage() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains("player.displayClientMessage(message, actionBar);"));
    }
}
