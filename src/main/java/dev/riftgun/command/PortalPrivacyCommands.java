package dev.riftgun.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.riftgun.RiftGun;
import dev.riftgun.data.PlayerPermissionOverride;
import dev.riftgun.service.PortalPrivacyService;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Chat-button responses for Player Portal privacy requests. */
@EventBusSubscriber(modid = RiftGun.MOD_ID)
public final class PortalPrivacyCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("riftgun")
            .then(Commands.literal("privacy")
                .then(Commands.literal("respond")
                    .then(Commands.argument("requester", net.minecraft.commands.arguments.UuidArgument.uuid())
                        .then(Commands.literal("allowonce").executes(context -> respond(
                            context.getSource(), net.minecraft.commands.arguments.UuidArgument
                                .getUuid(context, "requester"), null)))
                        .then(Commands.literal("allow").executes(context -> respond(
                            context.getSource(), net.minecraft.commands.arguments.UuidArgument
                                .getUuid(context, "requester"), PlayerPermissionOverride.ALLOW)))
                        .then(Commands.literal("deny").executes(context -> respond(
                            context.getSource(), net.minecraft.commands.arguments.UuidArgument
                                .getUuid(context, "requester"), PlayerPermissionOverride.DENY)))))));
    }

    private static int respond(CommandSourceStack source, UUID requesterId, PlayerPermissionOverride mode) {
        if (!(source.getEntity() instanceof ServerPlayer target)) {
            source.sendFailure(Component.translatable("message.riftgun.privacy_respond_denied"));
            return 0;
        }
        if (requesterId.equals(target.getUUID())) {
            source.sendFailure(Component.translatable("message.riftgun.privacy_respond_self"));
            return 0;
        }
        if (mode == null) {
            if (!PortalPrivacyService.allowOnce(source.getServer(), target.getUUID(), requesterId)) {
                source.sendFailure(Component.translatable("message.riftgun.privacy_respond_denied"));
                return 0;
            }
            target.displayClientMessage(Component.translatable("message.riftgun.privacy_allow_once_granted"), true);
        } else {
            PortalPrivacyService.applyOverride(target, requesterId, mode);
            target.displayClientMessage(Component.translatable(mode == PlayerPermissionOverride.ALLOW
                ? "message.riftgun.privacy_allow_granted" : "message.riftgun.privacy_deny_granted"), true);
        }
        ServerPlayer requester = source.getServer().getPlayerList().getPlayer(requesterId);
        if (requester != null) {
            if (mode == PlayerPermissionOverride.DENY) {
                requester.displayClientMessage(Component.translatable(
                    "message.riftgun.privacy_requester_denied", target.getGameProfile().getName()), true);
            } else {
                requester.displayClientMessage(Component.translatable(
                    "message.riftgun.privacy_requester_granted", target.getGameProfile().getName()), true);
            }
        }
        return 1;
    }

    private PortalPrivacyCommands() {}
}
