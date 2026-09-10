package dev.riftgun.navigation;

import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Shared dimension lookup and vanilla coordinate-scale mapping. */
public final class DimensionalTraversalTargets {
    /** Uses the same provider and path fallback as the dimension picker, without client input. */
    public static String name(ServerPlayer player, ResourceKey<Level> dimension) {
//? if >=1.21.11 {
        /*String id = dimension.identifier().toString();
*///?} else {
        String id = dimension.location().toString();
//?}
        return dev.riftgun.api.RiftGunDimensionLabels.label(player, dev.riftgun.api.RiftResourceId.parse(id))
            .map(net.minecraft.network.chat.Component::getString).orElseGet(() -> {
                StringBuilder name = new StringBuilder();
                for (String word : id.substring(id.indexOf(':') + 1).replace('_', ' ').split(" ")) {
                    if (!name.isEmpty()) name.append(' ');
                    if (!word.isEmpty()) name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
                }
                return name.toString();
            });
    }

    public static Optional<ServerLevel> resolve(ServerPlayer player, String dimensionId) {
//? if >=1.21.11 {
        /*Identifier parsed = Identifier.tryParse(dimensionId);
*///?} else {
        ResourceLocation parsed = ResourceLocation.tryParse(dimensionId);
//?}
        if (parsed == null) return Optional.empty();
//? if >=1.21.11 {
        /*MinecraftServer server = player.level().getServer();
*///?} else {
        MinecraftServer server = player.getServer();
//?}
        if (server == null) return Optional.empty();
        return Optional.ofNullable(server.getLevel(ResourceKey.create(Registries.DIMENSION, parsed)));
    }

    public static double mapCoordinate(double coordinate, Level source, Level target) {
        return coordinate * source.dimensionType().coordinateScale()
            / target.dimensionType().coordinateScale();
    }

    public static String id(Level level) {
//? if >=1.21.11 {
        /*return level.dimension().identifier().toString();
*///?} else {
        return level.dimension().location().toString();
//?}
    }

    private DimensionalTraversalTargets() {}
}
