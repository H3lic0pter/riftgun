package dev.riftgun.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.riftgun.core.RiftConstants;
import dev.riftgun.service.CoordinateSharingService;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Opaque chat-token import endpoint. */
@EventBusSubscriber(modid = RiftConstants.MOD_ID)
public final class CoordinateShareCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("riftgun")
            .then(Commands.literal("share")
                .then(Commands.literal("import")
                    .then(Commands.argument("share", UuidArgument.uuid())
                        .executes(context -> importShare(context.getSource(),
                            UuidArgument.getUuid(context, "share")))))));
    }

    private static int importShare(CommandSourceStack source, UUID id) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        return CoordinateSharingService.importChat(player, id) == CoordinateSharingService.Result.SUCCESS ? 1 : 0;
    }

    private CoordinateShareCommands() {}
}
