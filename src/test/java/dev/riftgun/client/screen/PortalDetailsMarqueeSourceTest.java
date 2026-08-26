package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalDetailsMarqueeSourceTest {
    @Test
    void bothNodesMarqueeAllDetailsTextInsteadOfTruncatingIt() throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", node,
                "src/main/java/dev/riftgun/client/screen/PortalConfigScreen.java"));
            int start = source.indexOf("private void renderDetails(");
            int end = source.indexOf("private void renderModal(", start);
            assertTrue(start >= 0 && end > start);
            String details = source.substring(start, end);

            assertTrue(details.contains("GuiTextMarquee.offset"));
            assertTrue(count(details, "drawDetailText(") >= 10);
            assertFalse(details.contains("trim(value"));
        }
    }

    private static int count(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }
}
