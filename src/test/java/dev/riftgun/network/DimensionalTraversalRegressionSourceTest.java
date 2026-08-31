package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class DimensionalTraversalRegressionSourceTest {
    @Test
    void coordinateCreationUsesOneValidatedSavePath() throws Exception {
        String actions = Files.readString(Path.of(
            "src/main/java/dev/riftgun/network/PortalDestinationActions.java"));

        assertTrue(occurrences(actions, "createCoordinateDestination(") >= 3);
        assertFalse(actions.contains("relativeCoordinate("));
    }

    @Test
    void randomSearchUsesOneAdmissionPathAndAnExplicitKind() throws Exception {
        String manager = Files.readString(Path.of(
            "src/main/java/dev/riftgun/service/RandomRiftManager.java"));

        assertTrue(occurrences(manager, "startSearch(") >= 3);
        assertTrue(manager.contains("private enum SearchKind"));
        assertFalse(manager.contains("boolean dimensional"));
    }

    @Test
    void fullDimensionCatalogIsOnlyBuiltForRelevantGuiOpen() throws Exception {
        String networking = Files.readString(Path.of(
            "src/main/java/dev/riftgun/network/PortalNetworking.java"));
        String labels = Files.readString(Path.of(
            "src/main/java/dev/riftgun/client/DimensionLabelState.java"));

        assertTrue(networking.contains(
            "if (openScreen && Nbt.getBoolean(gun, \"DimensionalTraversalInstalled\")"));
        assertTrue(networking.contains("putDimensionCatalog(envelope, player);"));
        assertTrue(labels.contains("if (envelope.contains(\"Dimensions\"))"));
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
