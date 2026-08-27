package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ModeRadialRenderOrderTest {
    @Test
    void legacyScreenBlursBackgroundBeforeDrawingRadial() throws IOException {
        String source = Files.readString(Path.of(
            "versions/1.21.1/src/main/java/dev/riftgun/client/screen/ModeRadialScreen.java"));
        int renderStart = source.indexOf("public void render(");
        int nextMethod = source.indexOf("public void commitAndClose()", renderStart);
        String renderMethod = source.substring(renderStart, nextMethod);
        assertTrue(renderMethod.indexOf("super.render(") < renderMethod.indexOf("drawRing("),
            "1.21.1 Screen.render applies blur, so it must run before the radial is drawn");
    }
}
