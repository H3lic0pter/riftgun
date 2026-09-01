package dev.riftgun.client.light;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LambDynamicLightsIntegrationSourceTest {
    private static final String INITIALIZER = "RiftGunDynamicLightsInitializer.java";

    @Test
    void bothNodesRegisterPortalLuminanceThroughTheOptionalInitializer() throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            Path lightPackage = Path.of("versions", node, "src", "main", "java", "dev", "riftgun",
                "client", "light");
            String source = Files.readString(lightPackage.resolve(INITIALIZER));

            assertTrue(source.contains("implements DynamicLightsInitializer"), node);
            assertTrue(source.contains("entityLightSourceManager().onRegisterEvent()"), node);
            assertTrue(source.contains("RiftContent.PORTAL.get()"), node);
            assertTrue(source.contains("RiftContent.ENTITY_RELOCATION_PORTAL.get()"), node);
            assertTrue(source.contains("PortalDynamicLightLevel.forPortal(portal)"), node);
            assertTrue(source.contains("PortalDynamicLightLevel.forRelocationPortal(relocationPortal)"), node);
            assertFalse(Files.exists(lightPackage.resolve("PortalDynamicLights.java")), node);
        }
    }

    @Test
    void everyEffectiveManifestDeclaresLambDynamicLightsAsOptional() throws IOException {
        for (ManifestExpectation expectation : new ManifestExpectation[] {
            new ManifestExpectation(
                Path.of("src", "main", "resources", "META-INF", "neoforge.mods.toml"),
                "[4.8.10,5)"),
            new ManifestExpectation(
                Path.of("versions", "26.1.2", "src", "main", "resources", "META-INF",
                    "neoforge.mods.toml"),
                "[4.11.1,5)")
        }) {
            String manifest = normalized(Files.readString(expectation.path()));
            String dependencyBlock = normalized("""
                [[dependencies.${mod_id}]]
                modId = "lambdynlights"
                type = "optional"
                versionRange = "%s"
                ordering = "AFTER"
                side = "CLIENT"
                """.formatted(expectation.versionRange())).strip();

            assertTrue(manifest.contains(dependencyBlock), expectation.path().toString());
            assertTrue(manifest.contains("\"lambdynlights:initializer\" = "
                + "\"dev.riftgun.client.light.RiftGunDynamicLightsInitializer\""),
                expectation.path().toString());
            assertFalse(manifest.contains("ryoamiclights"), expectation.path().toString());
        }
    }

    @Test
    void optionalApiIsIsolatedFromNormalClientStartup() throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            Path sourceRoot = Path.of("versions", node, "src", "main", "java");
            try (Stream<Path> sources = Files.walk(sourceRoot)) {
                for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                    if (source.getFileName().toString().equals(INITIALIZER)) continue;

                    String text = Files.readString(source);
                    assertFalse(text.contains("dev.lambdaurora.lambdynlights"), source.toString());
                    assertFalse(text.contains("RiftGunDynamicLightsInitializer"), source.toString());
                }
            }
        }

        String buildScript = Files.readString(Path.of("build.gradle.kts"));
        assertTrue(buildScript.contains("optionalClientCompileOnly.name, coordinate"));
        assertFalse(buildScript.contains("lambdynamiclights-runtime"));
    }

    private static String normalized(String text) {
        return text.replace("\r\n", "\n");
    }

    private record ManifestExpectation(Path path, String versionRange) {}
}
