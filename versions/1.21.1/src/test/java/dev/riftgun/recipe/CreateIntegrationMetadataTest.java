package dev.riftgun.recipe;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class CreateIntegrationMetadataTest {
    @Test
    void createIsAnOptionalBothSidesDependency() throws Exception {
        String metadata;
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("META-INF/neoforge.mods.toml")) {
            if (stream == null) throw new IllegalStateException("missing NeoForge metadata");
            metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        int createDependency = metadata.indexOf("modId = \"create\"");
        assertTrue(createDependency >= 0);
        String block = metadata.substring(createDependency, Math.min(metadata.length(), createDependency + 260));
        assertTrue(block.contains("type = \"optional\""));
        assertTrue(block.contains("versionRange = \"[6.0.7,)\""));
        assertTrue(block.contains("ordering = \"AFTER\""));
        assertTrue(block.contains("side = \"BOTH\""));
    }
}
