package dev.riftgun.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class LoaderBoundaryTest {
    private static final Path CORE = Path.of("src/main/java/dev/riftgun/core");
    private static final Path MAIN = Path.of("src/main/java/dev/riftgun");
    private static final Set<String> NEOFORGE_ADAPTERS = Set.of(
        "command/CoordinateShareCommands.java", "command/PortalCrisisTestCommands.java",
        "command/PortalPrivacyCommands.java",
        "fuel/PortalFluids.java", "fuel/PortalGunComponents.java",
        "fuel/PortalGunFluidInteractions.java", "fuel/PortalGunSnapshot.java",
        "fuel/PortalGunTank.java", "fuel/PortalGunWorldScoop.java",
        "module/PortalModuleMenus.java", "module/PortalModules.java",
        "portal/PortalGunItem.java",
        "recipe/FluidTransmutationEvents.java", "recipe/FluidTransmutationMatcher.java",
        "recipe/FluidTransmutationRecipe.java", "recipe/FluidTransmutationService.java",
        "recipe/RiftGunRecipes.java", "sound/PortalSounds.java");
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
    void neoForgeDependenciesStayInsideReviewedAdapters() throws IOException {
        for (Path source : javaSources(MAIN)) {
            if (!Files.readString(source).contains("net.neoforged.")) continue;
            String relative = MAIN.relativize(source).toString().replace('\\', '/');
            assertTrue(NEOFORGE_ADAPTERS.contains(relative),
                () -> source + " introduces an unreviewed NeoForge dependency");
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
