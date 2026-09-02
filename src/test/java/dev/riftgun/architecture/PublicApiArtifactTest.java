package dev.riftgun.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies the published API artifacts rather than only the main test classpath. */
final class PublicApiArtifactTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void binaryJarContainsItsRuntimeClosureAndLicense() throws Exception {
        Path artifact = requiredArtifact("riftgun.apiJar");
        try (JarFile jar = new JarFile(artifact.toFile())) {
            assertNotNull(jar.getJarEntry("dev/riftgun/api/RiftGunApiBootstrap.class"));
            assertNotNull(jar.getJarEntry("META-INF/LICENSE"));
            assertNull(jar.getJarEntry("dev/riftgun/api/RiftGunDestinationProvider.class"));
            assertNull(jar.getJarEntry("dev/riftgun/api/RiftGunDestinationProviders.class"));
            assertNull(jar.getJarEntry("dev/riftgun/api/ProvidedPortalDestination.class"));
        }

        try (URLClassLoader loader = new URLClassLoader(
            new java.net.URL[] {artifact.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> context = Class.forName("dev.riftgun.api.RiftGunTransitContext", true, loader);
            Method currentAuthorization = context.getMethod("currentAuthorization");
            assertEquals(Optional.empty(), currentAuthorization.invoke(null));
        }
    }

    @Test
    void sourcesJarContainsItsRuntimeBridgeAndLicense() throws Exception {
        Path artifact = requiredArtifact("riftgun.apiSourcesJar");
        try (JarFile jar = new JarFile(artifact.toFile())) {
            assertNotNull(jar.getJarEntry("dev/riftgun/api/RiftGunApiBootstrap.java"));
            assertNotNull(jar.getJarEntry("META-INF/LICENSE"));
            assertNull(jar.getJarEntry("dev/riftgun/api/RiftGunDestinationProvider.java"));
            assertNull(jar.getJarEntry("dev/riftgun/api/RiftGunDestinationProviders.java"));
            assertNull(jar.getJarEntry("dev/riftgun/api/ProvidedPortalDestination.java"));
        }
    }

    @Test
    void addonFixtureCompilesAgainstOnlyTheBinaryApiArtifact() throws Exception {
        Path artifact = requiredArtifact("riftgun.apiJar");
        Path source = temporaryDirectory.resolve("AddonFixture.java");
        Path classes = temporaryDirectory.resolve("classes");
        Files.createDirectories(classes);
        Files.writeString(source, """
            import dev.riftgun.api.RiftGunTransitContext;

            final class AddonFixture {
                Object currentAuthorization() {
                    return RiftGunTransitContext.currentAuthorization();
                }
            }
            """);

        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "artifact test requires the project JDK, not a JRE");
        int result = compiler.run(null, null, null,
            "-classpath", artifact.toString(), "-d", classes.toString(), source.toString());
        assertEquals(0, result, "minimal Addon fixture must compile using only the API JAR");
    }

    private static Path requiredArtifact(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing test artifact property: " + property);
        }
        return Path.of(value);
    }
}
