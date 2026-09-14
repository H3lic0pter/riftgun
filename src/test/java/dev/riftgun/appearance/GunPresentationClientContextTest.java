package dev.riftgun.appearance;

import dev.riftgun.client.PortalClientState;
import dev.riftgun.client.appearance.SkinRecommendations;
import dev.riftgun.client.screen.PortalConfigScreen;
import dev.riftgun.core.nbt.Nbt;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class GunPresentationClientContextTest {
    @BeforeAll
    static void bootstrap() { GunPresentationDefaultsTest.bootstrap(); }

    @Test
    void backgroundFullAndFocusedSnapshotsCannotRetargetSettingsRequests() {
        var minecraft = mock(Minecraft.class);
        minecraft.screen = mock(PortalConfigScreen.class);
        try (var client = mockStatic(Minecraft.class)) {
            client.when(Minecraft::getInstance).thenReturn(minecraft);
            CompoundTag editing = snapshot(UUID.randomUUID().toString());
            editing.putBoolean("OpenScreen", true);
            SkinRecommendations.receive(editing);
            editing.remove("OpenScreen");
            PortalClientState.handle(editing);
            assertRequestTarget(editing);

            CompoundTag background = snapshot(UUID.randomUUID().toString());
            PortalClientState.handle(background);
            assertRequestTarget(editing);
            background.putString("Kind", "GunSnapshot");
            background.putBoolean("Rollback", true);
            PortalClientState.handle(background);
            assertRequestTarget(editing);
            // Closing the editor restores the existing background synchronization behavior.
            var editor = minecraft.screen;
            minecraft.screen = null;
            background.putString("Kind", "Snapshot");
            PortalClientState.handle(background);
            minecraft.screen = editor;
            assertRequestTarget(background);
        }
    }

    private static void assertRequestTarget(CompoundTag editing) {
        CompoundTag request = new CompoundTag();
        PortalClientState.writeGunReference(request);
        assertEquals(Nbt.getCompound(editing, "GunReference"), Nbt.getCompound(request, "GunReference"));
    }

    private static CompoundTag snapshot(String identity) {
        CompoundTag reference = new CompoundTag();
        reference.putString("Instance", identity);
        CompoundTag response = new CompoundTag();
        response.putString("Kind", "Snapshot");
        response.put("GunReference", reference);
        response.put("Presentation", GunPresentation.DEFAULT.save());
        return response;
    }
}
