package dev.riftgun.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.riftgun.core.RiftConstants;
import dev.riftgun.crisis.PortalCrisis;
import dev.riftgun.crisis.PortalCrisisRegistry;
import dev.riftgun.crisis.PortalCrisisTestOverrides;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator-only, in-memory controls for reproducing portal crises on a player's next transit. */
@EventBusSubscriber(modid = RiftConstants.MOD_ID)
public final class PortalCrisisTestCommands {
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("riftgun")
            .then(Commands.literal("crisis")
                .requires(source -> source.hasPermission(REQUIRED_PERMISSION_LEVEL))
                .then(Commands.literal("force")
                    .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("crisis", ResourceLocationArgument.id())
                            .suggests(PortalCrisisTestCommands::suggestCrises)
                            .executes(PortalCrisisTestCommands::force))))
                .then(Commands.literal("clear")
                    .then(Commands.argument("targets", EntityArgument.players())
                        .executes(PortalCrisisTestCommands::clear)))
                .then(Commands.literal("status")
                    .then(Commands.argument("targets", EntityArgument.players())
                        .executes(PortalCrisisTestCommands::status)))
                .then(Commands.literal("list").executes(PortalCrisisTestCommands::list))));
    }

    private static int force(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation parsedId = ResourceLocationArgument.getId(context, "crisis");
        Optional<ResourceLocation> resolvedId = resolveCrisisId(parsedId);
        if (resolvedId.isEmpty()) {
            context.getSource().sendFailure(Component.translatable(
                "commands.riftgun.crisis.unknown", parsedId.toString()));
            return 0;
        }
        ResourceLocation crisisId = resolvedId.get();

        for (ServerPlayer player : targets) {
            Optional<ResourceLocation> previous = PortalCrisisTestOverrides.force(
                player.getUUID(), crisisId);
            if (previous.isPresent()) {
                context.getSource().sendSuccess(() -> Component.translatable(
                    "commands.riftgun.crisis.force.replaced", player.getDisplayName(),
                    previous.get().toString(), crisisId.toString()).withStyle(ChatFormatting.YELLOW), false);
            }
        }
        context.getSource().sendSuccess(() -> Component.translatable(
            "commands.riftgun.crisis.force.success", crisisId.toString(), targets.size())
            .withStyle(ChatFormatting.GREEN), false);
        return targets.size();
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int cleared = 0;
        for (ServerPlayer player : targets) {
            if (PortalCrisisTestOverrides.clear(player.getUUID()).isPresent()) cleared++;
        }
        int result = cleared;
        context.getSource().sendSuccess(() -> Component.translatable(
            "commands.riftgun.crisis.clear.success", result).withStyle(ChatFormatting.GREEN), false);
        return cleared;
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int armed = 0;
        for (ServerPlayer player : targets) {
            Optional<ResourceLocation> forced = PortalCrisisTestOverrides.forced(player.getUUID());
            if (forced.isPresent()) {
                armed++;
                ResourceLocation id = forced.get();
                context.getSource().sendSuccess(() -> Component.translatable(
                    "commands.riftgun.crisis.status.armed", player.getDisplayName(), id.toString()), false);
            } else {
                context.getSource().sendSuccess(() -> Component.translatable(
                    "commands.riftgun.crisis.status.clear", player.getDisplayName()), false);
            }
        }
        return armed;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        MutableComponent ids = Component.empty();
        boolean first = true;
        for (PortalCrisis crisis : PortalCrisisRegistry.definitions()) {
            if (!first) ids.append(Component.literal(", "));
            ids.append(Component.literal(crisis.id().toString()));
            first = false;
        }
        context.getSource().sendSuccess(() -> Component.translatable(
            "commands.riftgun.crisis.list", ids), false);
        return PortalCrisisRegistry.definitions().size();
    }

    private static CompletableFuture<Suggestions> suggestCrises(
        CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (PortalCrisis crisis : PortalCrisisRegistry.definitions()) {
            String id = crisis.id().toString();
            if (id.toLowerCase(java.util.Locale.ROOT).startsWith(remaining)) builder.suggest(id);
        }
        return builder.buildFuture();
    }

    static Optional<ResourceLocation> resolveCrisisId(ResourceLocation parsedId) {
        return PortalCrisisRegistry.find(parsedId) == null
            ? Optional.empty() : Optional.of(parsedId);
    }

    private PortalCrisisTestCommands() {}
}
