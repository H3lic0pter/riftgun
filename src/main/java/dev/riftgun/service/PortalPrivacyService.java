package dev.riftgun.service;

import dev.riftgun.data.PlayerPermissionOverride;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.TargetPrivacy;
import java.util.UUID;
import dev.riftgun.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side authority for Player Portal privacy.
 *
 * <p>Global privacy and per-requester overrides live on the target's {@link PortalPlayerData}.
 * In-flight one-shot allowances are held here because they must not persist across restarts and
 * expire after a short window.
 */
public final class PortalPrivacyService {
    private static final int TICKS_PER_SECOND = 20;
    private static final PortalPrivacyLedger LEDGER = new PortalPrivacyLedger();

    /** True when {@code requester} may open a Player Target portal next to {@code target} right now. */
    public static Access checkPortalAccess(MinecraftServer server, ServerPlayer target, ServerPlayer requester) {
        PortalPlayerData targetData = PortalDataStore.load(target);
        PlayerPermissionOverride override = targetData.privacyOverride(requester.getUUID());
        if (override == PlayerPermissionOverride.ALLOW) return Access.ALLOWED;
        if (override == PlayerPermissionOverride.DENY) return Access.DENIED;
        return switch (targetData.targetPrivacy()) {
            case PUBLIC -> Access.ALLOWED;
            case PRIVATE -> Access.DENIED;
            case REQUEST -> LEDGER.consume(target.getUUID(), requester.getUUID(), gameTime(server))
                ? Access.ALLOWED : Access.REQUESTED;
        };
    }

    /** Grants a matching, still-pending request once. */
    public static boolean allowOnce(MinecraftServer server, UUID targetId, UUID requesterId) {
        return LEDGER.allowOnce(targetId, requesterId, gameTime(server), requestTtlTicks());
    }

    /**
     * Sends the target a chat prompt to grant or deny {@code requester}'s portal request.
     * Returns true when a fresh prompt was sent (an allowance is now pending until consumed or expired).
     */
    public static boolean promptRequest(MinecraftServer server, ServerPlayer target,
                                        ServerPlayer requester) {
        if (!LEDGER.prompt(target.getUUID(), requester.getUUID(), gameTime(server), requestTtlTicks())) {
            return false;
        }
        sendRequestPrompt(target, requester);
        return true;
    }

    /** Records a permanent override from a chat response (Always Allow / Deny). */
    public static void applyOverride(ServerPlayer target, UUID requesterId, PlayerPermissionOverride mode) {
        PortalPlayerData targetData = PortalDataStore.load(target);
        targetData.privacyOverride(requesterId, mode);
        PortalDataStore.save(target, targetData);
        clearPrompt(target, requesterId);
    }

    /** True when {@code target} has Transit Privacy enabled (forced exit exclusion by others). */
    public static boolean transitProtectsTarget(ServerPlayer target) {
        return PortalDataStore.load(target).transitPrivacyEnabled();
    }

    public enum Access {
        ALLOWED,
        REQUESTED,
        DENIED
    }

    /** True when {@code target}'s entry is visible in {@code viewer}'s player list. */
    public static boolean isVisibleTo(MinecraftServer server, ServerPlayer viewer, ServerPlayer target) {
        if (viewer.getUUID().equals(target.getUUID())) return true;
        PortalPlayerData targetData = PortalDataStore.load(target);
        PlayerPermissionOverride override = targetData.privacyOverride(viewer.getUUID());
        if (override == PlayerPermissionOverride.ALLOW) return true;
        if (override == PlayerPermissionOverride.DENY) return false;
        return targetData.targetPrivacy() != TargetPrivacy.PRIVATE;
    }

    private static void sendRequestPrompt(ServerPlayer target, ServerPlayer requester) {
        String requesterId = requester.getUUID().toString();
        Component message = Component.translatable("chat.riftgun.privacy_request",
                Component.literal(requester.getGameProfile().getName()))
            .append(Component.literal(" "))
            .append(Component.translatable("chat.riftgun.privacy_allow_once")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN).withClickEvent(
                    new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/riftgun privacy respond " + requesterId + " allowonce"))))
            .append(Component.literal(" "))
            .append(Component.translatable("chat.riftgun.privacy_always_allow")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withClickEvent(
                    new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/riftgun privacy respond " + requesterId + " allow"))))
            .append(Component.literal(" "))
            .append(Component.translatable("chat.riftgun.privacy_deny")
                .withStyle(style -> style.withColor(ChatFormatting.RED).withClickEvent(
                    new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/riftgun privacy respond " + requesterId + " deny"))));
        target.displayClientMessage(message, false);
    }

    private static void clearPrompt(ServerPlayer target, UUID requesterId) {
        LEDGER.clearPrompt(target.getUUID(), requesterId);
    }

    private static long requestTtlTicks() {
        return (long) ServerConfig.VALUES.privacyRequestTimeoutSeconds.get() * TICKS_PER_SECOND;
    }

    private static long gameTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private PortalPrivacyService() {}
}
