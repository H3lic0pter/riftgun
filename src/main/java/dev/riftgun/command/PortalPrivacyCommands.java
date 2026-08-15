package dev.riftgun.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.riftgun.core.RiftConstants;
import dev.riftgun.service.PortalPrivacyService;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Token-bound chat-button responses for Player Portal privacy requests. */
@EventBusSubscriber(modid = RiftConstants.MOD_ID)
public final class PortalPrivacyCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("riftgun")
            .then(Commands.literal("privacy")
                .then(Commands.literal("respond")
                    .then(Commands.argument("request", UuidArgument.uuid())
                        .then(response("allow_once", PortalPrivacyService.Response.ALLOW_ONCE))
                        .then(response("always_allow", PortalPrivacyService.Response.ALWAYS_ALLOW))
                        .then(response("deny_once", PortalPrivacyService.Response.DENY_ONCE))
                        .then(response("always_deny", PortalPrivacyService.Response.ALWAYS_DENY))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> response(
        String literal, PortalPrivacyService.Response response) {
        return Commands.literal(literal).executes(context -> respond(
            context.getSource(), UuidArgument.getUuid(context, "request"), response));
    }

    private static int respond(CommandSourceStack source, UUID requestToken,
                               PortalPrivacyService.Response response) {
        if (!(source.getEntity() instanceof ServerPlayer target)) {
            source.sendFailure(Component.translatable("message.riftgun.privacy_respond_denied"));
            return 0;
        }
        PortalPrivacyService.RespondResult result = PortalPrivacyService.respond(
            source.getServer(), target, requestToken, response);
        if (result == PortalPrivacyService.RespondResult.INVALID) {
            target.displayClientMessage(Component.translatable(
                "chat.riftgun.privacy_response_expired").withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        return result == PortalPrivacyService.RespondResult.APPLIED ? 1 : 0;
    }

    private PortalPrivacyCommands() {}
}
