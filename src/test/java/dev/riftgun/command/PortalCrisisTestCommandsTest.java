package dev.riftgun.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.brigadier.StringReader;
import java.util.Optional;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class PortalCrisisTestCommandsTest {
    private static final ResourceLocation HIGH_FALL = ResourceLocation.fromNamespaceAndPath(
        "riftgun", "high_altitude_fall");

    @Test
    void resourceLocationArgumentAcceptsNamespacedCrisisId() throws Exception {
        ResourceLocation parsed = ResourceLocationArgument.id().parse(
            new StringReader("riftgun:high_altitude_fall"));

        assertEquals(HIGH_FALL, parsed);
    }

    @Test
    void resolverAcceptsFullNamespacedIdOnly() {
        assertEquals(Optional.of(HIGH_FALL),
            PortalCrisisTestCommands.resolveCrisisId(HIGH_FALL));
        // Shorthand without the riftgun namespace is no longer accepted.
        assertEquals(Optional.empty(), PortalCrisisTestCommands.resolveCrisisId(
            ResourceLocation.withDefaultNamespace("high_altitude_fall")));
    }
}
