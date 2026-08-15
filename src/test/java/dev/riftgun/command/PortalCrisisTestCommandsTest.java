package dev.riftgun.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.brigadier.StringReader;
import java.util.Optional;
//? if >=1.21.11 {
/*import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
//?}
import org.junit.jupiter.api.Test;

class PortalCrisisTestCommandsTest {
    //? if >=1.21.11 {
    /*private static final Identifier HIGH_FALL = Identifier.fromNamespaceAndPath(
        "riftgun", "high_altitude_fall");
    *///?} else {
    private static final ResourceLocation HIGH_FALL = ResourceLocation.fromNamespaceAndPath(
        "riftgun", "high_altitude_fall");
    //?}

    @Test
    void resourceLocationArgumentAcceptsNamespacedCrisisId() throws Exception {
        //? if >=1.21.11 {
        /*Identifier parsed = IdentifierArgument.id().parse(
            new StringReader("riftgun:high_altitude_fall"));
        *///?} else {
        ResourceLocation parsed = ResourceLocationArgument.id().parse(
            new StringReader("riftgun:high_altitude_fall"));
        //?}

        assertEquals(HIGH_FALL, parsed);
    }

    @Test
    void resolverAcceptsFullNamespacedIdOnly() {
        assertEquals(Optional.of(HIGH_FALL),
            PortalCrisisTestCommands.resolveCrisisId(HIGH_FALL));
        // Shorthand without the riftgun namespace is no longer accepted.
        //? if >=1.21.11 {
        /*assertEquals(Optional.empty(), PortalCrisisTestCommands.resolveCrisisId(
            Identifier.withDefaultNamespace("high_altitude_fall")));
        *///?} else {
        assertEquals(Optional.empty(), PortalCrisisTestCommands.resolveCrisisId(
            ResourceLocation.withDefaultNamespace("high_altitude_fall")));
        //?}
    }
}
