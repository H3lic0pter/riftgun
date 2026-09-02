package dev.riftgun.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ReleaseCandidateIdentitySourceTest {
    @Test
    void bothNodesPublishTheSameRcAndRejectProtocolOnePeers() throws IOException {
        assertContains("gradle.properties", "mod_version=0.2.0-rc.1");
        assertContains("versions/1.21.1/gradle.properties", "mod_version=0.2.0-rc.1");
        assertContains("versions/26.1.2/gradle.properties", "mod_version=0.2.0-rc.1");
        assertContains("versions/1.21.1/src/main/java/dev/riftgun/network/NeoForgeNetworkAdapter.java",
            "event.registrar(\"2\")");
        assertContains("versions/26.1.2/src/main/java/dev/riftgun/network/NeoForgeNetworkAdapter.java",
            "event.registrar(\"2\")");
    }

    @Test
    void releaseNotesAndModernJeiFloorMatchTheCandidate() throws IOException {
        assertTrue(Files.isRegularFile(Path.of(
            "docs/release-notes/1.21.1-v0.2.0-rc.1.md")));
        assertTrue(Files.isRegularFile(Path.of(
            "docs/release-notes/26.1.2-v0.2.0-rc.1.md")));
        assertFalse(Files.exists(Path.of(
            "docs/release-notes/1.21.1-v0.2.0-r1.md")));
        assertFalse(Files.exists(Path.of(
            "docs/release-notes/26.1.2-v0.2.0-r1.md")));
        assertContains("versions/26.1.2/src/main/resources/META-INF/neoforge.mods.toml",
            "versionRange = \"[29.29.0.76,)\"");
    }

    @Test
    void optionalClientRuntimeDependenciesCannotEnterServerRuns() throws IOException {
        String build = Files.readString(Path.of("build.gradle.kts"));
        assertFalse(build.contains(
            "configurations.runtimeOnly.get().extendsFrom(optionalClientRuntimeOnly)"));
        assertTrue(build.contains(
            "additionalRuntimeClasspathConfiguration.extendsFrom(optionalClientRuntimeOnly)"));
    }

    private static void assertContains(String path, String expected) throws IOException {
        assertTrue(Files.readString(Path.of(path)).contains(expected), path);
    }
}
