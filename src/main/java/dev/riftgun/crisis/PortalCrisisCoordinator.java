package dev.riftgun.crisis;

import dev.riftgun.core.runtime.RiftRuntime;
import dev.riftgun.core.registry.RiftContent;
import com.mojang.logging.LogUtils;
import dev.riftgun.core.RiftConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/** Deep module joining eligibility, fixed-budget selection, preparation, and common effects. */
public final class PortalCrisisCoordinator {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static Optional<PortalCrisisPlan> prepare(PortalCrisisConfigurationSnapshot configuration,
                                                     ServerPlayer player, ServerLevel targetLevel,
                                                     Vec3 normalDestination, Vec3 normalMomentum,
                                                     float destinationYaw, boolean mountedTransit,
                                                     boolean relocationAllowed) {
        if (!configuration.unstable() || player.isSpectator()) return Optional.empty();
        try {
            PortalCrisisCapabilitySnapshot capabilities = PortalCrisisCapabilitySnapshot.capture(
                player, targetLevel, mountedTransit);
            List<PortalCrisisEngine.Candidate<PortalCrisis>> candidates = new ArrayList<>();
            for (PortalCrisis crisis : PortalCrisisRegistry.definitions()) {
                candidates.add(new PortalCrisisEngine.Candidate<>(crisis,
                    configuration.weight(crisis.id()), crisis.eligible(capabilities)));
            }
            Optional<PortalCrisis> selected = PortalCrisisEngine.select(candidates,
                player.getRandom().nextInt(PortalCrisisEngine.TOTAL_WEIGHT));
            if (selected.isEmpty()) return Optional.empty();
            PortalCrisisContext context = new PortalCrisisContext(player, targetLevel, normalDestination,
                normalMomentum, destinationYaw, capabilities, RiftRuntime.current().safetyInspector(),
                relocationAllowed);
            return selected.get().prepare(context);
        } catch (RuntimeException exception) {
            LOGGER.error("Unstable portal crisis evaluation failed", exception);
            return Optional.empty();
        }
    }

    public static ForcedCrisisPreparation prepareForced(ResourceLocation crisisId,
                                                        ServerPlayer player, ServerLevel targetLevel,
                                                        Vec3 normalDestination, Vec3 normalMomentum,
                                                        float destinationYaw, boolean mountedTransit,
                                                        boolean relocationAllowed) {
        PortalCrisis crisis = PortalCrisisRegistry.find(crisisId);
        if (crisis == null) {
            return ForcedCrisisPreparation.failed(
                ForcedCrisisPreparation.Failure.UNKNOWN_CRISIS);
        }
        if (player.isSpectator()) {
            return ForcedCrisisPreparation.failed(ForcedCrisisPreparation.Failure.SPECTATOR);
        }
        if (mountedTransit && !crisis.supportsForcedMountedTransit()) {
            return ForcedCrisisPreparation.failed(
                ForcedCrisisPreparation.Failure.MOUNTED_TRANSIT);
        }
        if (!relocationAllowed && crisis.requiresRelocation()) {
            return ForcedCrisisPreparation.failed(
                ForcedCrisisPreparation.Failure.CRISIS_EXIT_LIMIT);
        }
        try {
            PortalCrisisCapabilitySnapshot capabilities = PortalCrisisCapabilitySnapshot.capture(
                player, targetLevel, mountedTransit);
            PortalCrisisContext context = new PortalCrisisContext(player, targetLevel,
                normalDestination, normalMomentum, destinationYaw, capabilities,
                RiftRuntime.current().safetyInspector(), relocationAllowed);
            return crisis.prepare(context)
                .map(ForcedCrisisPreparation::success)
                .orElseGet(() -> ForcedCrisisPreparation.failed(
                    ForcedCrisisPreparation.Failure.DESTINATION_UNAVAILABLE));
        } catch (RuntimeException exception) {
            LOGGER.error("Forced portal crisis {} preparation failed", crisisId, exception);
            return ForcedCrisisPreparation.failed(
                ForcedCrisisPreparation.Failure.INTERNAL_ERROR);
        }
    }

    public static boolean apply(PortalCrisisPlan plan, ServerPlayer player) {
        try {
            plan.effect().apply(player);
        } catch (RuntimeException exception) {
            LOGGER.error("Portal crisis {} failed while applying its effect", plan.crisisId(), exception);
            return false;
        }
        if (plan.cooldownTicks() > 0) {
            player.getCooldowns().addCooldown(RiftContent.PORTAL_GUN.get(), plan.cooldownTicks());
        }
        Component crisisName = Component.translatable(
            "crisis." + plan.crisisId().getNamespace() + "." + plan.crisisId().getPath());
        player.sendSystemMessage(Component.translatable("message.riftgun.crisis", crisisName)
            .withStyle(ChatFormatting.DARK_RED));
        return true;
    }

    private PortalCrisisCoordinator() {}
}
