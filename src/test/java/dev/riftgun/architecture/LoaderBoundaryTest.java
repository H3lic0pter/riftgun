package dev.riftgun.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class LoaderBoundaryTest {
    private static final Path CORE = Path.of("src/main/java/dev/riftgun/core");
    private static final List<String> FORBIDDEN_VISUAL_TYPES = List.of(
        "BakedModel", "BakedQuad", "ItemOverrides", "RenderType",
        "PoseStack", "VertexConsumer");

    @Test
    void commonCoreDoesNotImportNeoForge() throws IOException {
        for (Path source : javaSources(CORE)) {
            assertFalse(Files.readString(source).contains("net.neoforged."),
                () -> source + " imports NeoForge");
        }
    }

    @Test
    void sharedVisualSemanticsDoNotExposeVersionRendererTypes() throws IOException {
        for (Path source : javaSources(CORE.resolve("visual"))) {
            String text = Files.readString(source);
            for (String forbidden : FORBIDDEN_VISUAL_TYPES) {
                assertFalse(text.contains(forbidden),
                    () -> source + " exposes " + forbidden);
            }
        }
    }

    private static List<Path> javaSources(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }
}
