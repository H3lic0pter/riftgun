package dev.riftgun.service;

import dev.riftgun.config.ServerConfig;
import dev.riftgun.data.PlayerPermissionOverride;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.TargetPrivacy;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server-side authority and chat interaction flow for Player Portal privacy. */
public final class PortalPrivacyService {
    private static final int TICKS_PER_SECOND = 20;
    private static final PortalPrivacyLedger LEDGER = new PortalPrivacyLedger();

    /** Checks access without consuming a one-shot grant. */
    public static Access checkPortalAccess(MinecraftServer server, ServerPlayer target,
                                           ServerPlayer requester) {
        expireAndNotify(server);
        PortalPlayerData targetData = PortalDataStore.load(target);
        PlayerPermissionOverride override = targetData.privacyOverride(requester.getUUID());
        if (override == PlayerPermissionOverride.ALLOW) return Access.allowed();
        if (override == PlayerPermissionOverride.DENY) return Access.alwaysDenied();
        return switch (targetData.targetPrivacy()) {
            case PUBLIC -> Access.allowed();
            case PRIVATE -> Access.denied();
            case REQUEST -> requestAccess(target, requester, gameTime(server));
        };
    }

    /** Consumes a matching one-shot grant after a portal has actually opened. */
    public static boolean consumeGrant(MinecraftServer server, UUID targetId, UUID requesterId) {
        return LEDGER.consumeGrant(
            new PortalPrivacyLedger.RequestKey(targetId, requesterId), gameTime(server));
    }

    /** Creates or reuses a pending request and sends the appropriate chat status. */
    public static void promptRequest(MinecraftServer server, ServerPlayer target,
                                     ServerPlayer requester) {
        expireAndNotify(server);
        PortalPrivacyLedger.Parties parties = parties(target, requester);
        PortalPrivacyLedger.Prompt prompt = LEDGER.prompt(
            parties, gameTime(server), requestTtlTicks());
        if (prompt.fresh()) sendRequestPrompt(target, requester, prompt.token());
        requester.displayClientMessage(styled(prompt.fresh()
                ? "chat.riftgun.privacy_request_sent"
                : "chat.riftgun.privacy_request_pending",
            prompt.fresh() ? ChatFormatting.AQUA : ChatFormatting.GRAY,
            target.getGameProfile().getName()), false);
    }

    /** Applies a token-bound chat response and notifies both online participants. */
    public static RespondResult respond(MinecraftServer server, ServerPlayer target,
                                        UUID requestToken, Response response) {
        PortalPrivacyLedger.Resolution resolved = LEDGER.resolve(
            requestToken, target.getUUID(), gameTime(server));
        if (resolved.status() == PortalPrivacyLedger.ResolutionStatus.MISSING) {
            return RespondResult.INVALID;
        }
        if (resolved.status() == PortalPrivacyLedger.ResolutionStatus.EXPIRED) {
            notifyExpiry(server, new PortalPrivacyLedger.Expired(
                PortalPrivacyLedger.Expiration.REQUEST, resolved.parties()));
            return RespondResult.EXPIRED;
        }
        PortalPrivacyLedger.Parties parties = resolved.parties();
        switch (response) {
            case ALLOW_ONCE -> LEDGER.grantOnce(parties, gameTime(server), grantTtlTicks());
            case ALWAYS_ALLOW -> applyOverride(target, parties.requesterId(), PlayerPermissionOverride.ALLOW);
            case DENY_ONCE -> LEDGER.denyOnce(
                parties.key(), gameTime(server), denyOnceCooldownTicks());
            case ALWAYS_DENY -> applyOverride(target, parties.requesterId(), PlayerPermissionOverride.DENY);
        }
        notifyResponse(server, target, parties, response);
        return RespondResult.APPLIED;
    }

    /** Records a permanent override from the terminal or an Always response. */
    public static void applyOverride(ServerPlayer target, UUID requesterId,
                                     PlayerPermissionOverride mode) {
        PortalPlayerData targetData = PortalDataStore.load(target);
        targetData.privacyOverride(requesterId, mode);
        PortalDataStore.save(target, targetData);
        LEDGER.clearTransient(new PortalPrivacyLedger.RequestKey(target.getUUID(), requesterId));
    }

    /** Invalidates outstanding request state after the target changes global privacy policy. */
    public static void privacyChanged(ServerPlayer target) {
        LEDGER.clearTarget(target.getUUID());
    }

    /** Sends a requester-facing denial through chat, preserving the reason. */
    public static void notifyDenied(ServerPlayer requester, ServerPlayer target, Access access) {
        String targetName = target.getGameProfile().getName();
        Component message = switch (access.outcome()) {
            case ALWAYS_DENIED -> styled("chat.riftgun.privacy_always_denied_requester",
                ChatFormatting.RED, targetName);
            case DENIED_ONCE -> styled("chat.riftgun.privacy_deny_once_cooldown",
                ChatFormatting.RED, targetName, access.retryAfterSeconds());
            default -> styled("chat.riftgun.privacy_denied_requester",
                ChatFormatting.RED, targetName);
        };
        requester.displayClientMessage(message, false);
    }

    /** Expires transient state once per second and notifies participants who remain online. */
    public static void tick(MinecraftServer server) {
        long now = gameTime(server);
        if (now % TICKS_PER_SECOND == 0L) expireAndNotify(server);
    }

    /** Drops all server-lifetime request state when the integrated or dedicated server stops. */
    public static void reset() {
        LEDGER.clear();
    }

    /** True when {@code target} has Transit Privacy enabled. */
    public static boolean transitProtectsTarget(ServerPlayer target) {
        return PortalDataStore.load(target).transitPrivacyEnabled();
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

    private static Access requestAccess(ServerPlayer target, ServerPlayer requester, long now) {
        PortalPrivacyLedger.RequestKey key = new PortalPrivacyLedger.RequestKey(
            target.getUUID(), requester.getUUID());
        if (LEDGER.hasGrant(key, now)) return Access.grantedOnce();
        long remaining = LEDGER.denyRemainingTicks(key, now);
        if (remaining > 0L) {
            return Access.deniedOnce(secondsRoundedUp(remaining));
        }
        return Access.requested();
    }

    private static void sendRequestPrompt(ServerPlayer target, ServerPlayer requester, UUID token) {
        target.displayClientMessage(styled("chat.riftgun.privacy_request_received",
            ChatFormatting.AQUA, requester.getGameProfile().getName()), false);
        String command = "/riftgun privacy respond " + token + " ";
        Component actions = Component.empty()
            .append(action("chat.riftgun.privacy_allow_once", ChatFormatting.GREEN,
                command + "allow_once"))
            .append(Component.literal(" "))
            .append(action("chat.riftgun.privacy_always_allow", ChatFormatting.DARK_GREEN,
                command + "always_allow"))
            .append(Component.literal(" "))
            .append(action("chat.riftgun.privacy_deny_once", ChatFormatting.RED,
                command + "deny_once"))
            .append(Component.literal(" "))
            .append(action("chat.riftgun.privacy_always_deny", ChatFormatting.DARK_PURPLE,
                command + "always_deny"));
        target.displayClientMessage(actions, false);
    }

    private static Component action(String key, ChatFormatting color, String command) {
        return Component.translatable(key).withStyle(style -> style
            .withColor(color)
            .withUnderlined(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    private static void notifyResponse(MinecraftServer server, ServerPlayer target,
                                       PortalPrivacyLedger.Parties parties, Response response) {
        boolean allowed = response == Response.ALLOW_ONCE || response == Response.ALWAYS_ALLOW;
        ChatFormatting color = allowed ? ChatFormatting.GREEN : ChatFormatting.RED;
        String targetKey = switch (response) {
            case ALLOW_ONCE -> "chat.riftgun.privacy_allow_once_target";
            case ALWAYS_ALLOW -> "chat.riftgun.privacy_always_allow_target";
            case DENY_ONCE -> "chat.riftgun.privacy_deny_once_target";
            case ALWAYS_DENY -> "chat.riftgun.privacy_always_deny_target";
        };
        String requesterKey = switch (response) {
            case ALLOW_ONCE -> "chat.riftgun.privacy_allow_once_requester";
            case ALWAYS_ALLOW -> "chat.riftgun.privacy_always_allow_requester";
            case DENY_ONCE -> "chat.riftgun.privacy_deny_once_requester";
            case ALWAYS_DENY -> "chat.riftgun.privacy_always_deny_requester";
        };
        target.displayClientMessage(styled(targetKey, color, parties.requesterName()), false);
        ServerPlayer requester = server.getPlayerList().getPlayer(parties.requesterId());
        if (requester != null) {
            requester.displayClientMessage(styled(
                requesterKey, color, parties.targetName()), false);
        }
    }

    private static void expireAndNotify(MinecraftServer server) {
        for (PortalPrivacyLedger.Expired expired : LEDGER.expire(gameTime(server))) {
            notifyExpiry(server, expired);
        }
    }

    private static void notifyExpiry(MinecraftServer server, PortalPrivacyLedger.Expired expired) {
        PortalPrivacyLedger.Parties parties = expired.parties();
        ServerPlayer target = server.getPlayerList().getPlayer(parties.targetId());
        ServerPlayer requester = server.getPlayerList().getPlayer(parties.requesterId());
        if (expired.expiration() == PortalPrivacyLedger.Expiration.REQUEST) {
            if (target != null) target.displayClientMessage(styled(
                "chat.riftgun.privacy_request_expired_target", ChatFormatting.YELLOW,
                parties.requesterName()), false);
            if (requester != null) requester.displayClientMessage(styled(
                "chat.riftgun.privacy_request_expired_requester", ChatFormatting.YELLOW,
                parties.targetName()), false);
        } else {
            if (target != null) target.displayClientMessage(styled(
                "chat.riftgun.privacy_grant_expired_target", ChatFormatting.YELLOW,
                parties.requesterName()), false);
            if (requester != null) requester.displayClientMessage(styled(
                "chat.riftgun.privacy_grant_expired_requester", ChatFormatting.YELLOW,
                parties.targetName()), false);
        }
    }

    private static PortalPrivacyLedger.Parties parties(ServerPlayer target, ServerPlayer requester) {
        return new PortalPrivacyLedger.Parties(
            target.getUUID(), target.getGameProfile().getName(),
            requester.getUUID(), requester.getGameProfile().getName());
    }

    private static Component styled(String key, ChatFormatting color, Object... arguments) {
        return Component.translatable(key, arguments).withStyle(color);
    }

    private static int secondsRoundedUp(long ticks) {
        return (int) Math.max(1L, (ticks + TICKS_PER_SECOND - 1L) / TICKS_PER_SECOND);
    }

    private static long requestTtlTicks() {
        return (long) ServerConfig.VALUES.privacyRequestTimeoutSeconds.get() * TICKS_PER_SECOND;
    }

    private static long grantTtlTicks() {
        return (long) ServerConfig.VALUES.privacyGrantTimeoutSeconds.get() * TICKS_PER_SECOND;
    }

    private static long denyOnceCooldownTicks() {
        return (long) ServerConfig.VALUES.privacyDenyOnceCooldownSeconds.get() * TICKS_PER_SECOND;
    }

    private static long gameTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    public enum Outcome {
        ALLOWED,
        GRANTED_ONCE,
        REQUESTED,
        DENIED,
        DENIED_ONCE,
        ALWAYS_DENIED
    }

    public record Access(Outcome outcome, int retryAfterSeconds) {
        private static Access allowed() {
            return new Access(Outcome.ALLOWED, 0);
        }

        private static Access grantedOnce() {
            return new Access(Outcome.GRANTED_ONCE, 0);
        }

        private static Access requested() {
            return new Access(Outcome.REQUESTED, 0);
        }

        private static Access denied() {
            return new Access(Outcome.DENIED, 0);
        }

        private static Access deniedOnce(int retryAfterSeconds) {
            return new Access(Outcome.DENIED_ONCE, retryAfterSeconds);
        }

        private static Access alwaysDenied() {
            return new Access(Outcome.ALWAYS_DENIED, 0);
        }
    }

    public enum Response {
        ALLOW_ONCE,
        ALWAYS_ALLOW,
        DENY_ONCE,
        ALWAYS_DENY
    }

    public enum RespondResult {
        APPLIED,
        EXPIRED,
        INVALID
    }

    private PortalPrivacyService() {}
}
