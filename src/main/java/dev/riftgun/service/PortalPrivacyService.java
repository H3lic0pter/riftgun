package dev.riftgun.service;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.data.PlayerPermissionProfile;
import dev.riftgun.data.PlayerPermissionProfileMode;
import dev.riftgun.data.PortalPermissionDefinition;
import dev.riftgun.data.PortalPermissionPolicy;
import dev.riftgun.data.PortalPermissions;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
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
        return checkAccess(server, target, requester, PortalRequestPurpose.PORTAL);
    }

    public static Access checkAccess(MinecraftServer server, ServerPlayer target,
                                     ServerPlayer requester, PortalRequestPurpose purpose) {
        expireAndNotify(server);
        if (target.getUUID().equals(requester.getUUID())) return Access.allowed();
        PortalPlayerData targetData = PortalDataStore.load(target);
        PortalPermissionPolicy policy = effectivePolicy(
            targetData, requester.getUUID(), purpose.permissionId());
        return switch (policy) {
            case ALLOW -> Access.allowed();
            case DENY -> Access.alwaysDenied();
            case ASK -> requestAccess(target, requester, purpose, gameTime(server));
            case FOLLOW_GLOBAL -> Access.denied();
        };
    }

    /** Consumes a matching one-shot grant after a portal has actually opened. */
    public static boolean consumeGrant(MinecraftServer server, UUID targetId, UUID requesterId) {
        return consumeGrant(server, targetId, requesterId, PortalRequestPurpose.PORTAL);
    }

    public static boolean consumeGrant(MinecraftServer server, UUID targetId, UUID requesterId,
                                       PortalRequestPurpose purpose) {
        return LEDGER.consumeGrant(
            new PortalPrivacyLedger.RequestKey(targetId, requesterId, purpose), gameTime(server));
    }

    /** Creates or reuses a pending request and sends the appropriate chat status. */
    public static void promptRequest(MinecraftServer server, ServerPlayer target,
                                     ServerPlayer requester) {
        promptRequest(server, target, requester, PortalRequestPurpose.PORTAL);
    }

    public static void promptRequest(MinecraftServer server, ServerPlayer target,
                                     ServerPlayer requester, PortalRequestPurpose purpose) {
        expireAndNotify(server);
        PortalPrivacyLedger.Parties parties = parties(target, requester, purpose);
        PortalPrivacyLedger.Prompt prompt = LEDGER.prompt(
            parties, gameTime(server), requestTtlTicks());
        if (prompt.fresh()) sendRequestPrompt(target, requester, prompt.token(), purpose);
        requester.displayClientMessage(styled(purposeKey(prompt.fresh()
                ? "chat.riftgun.privacy_request_sent"
                : "chat.riftgun.privacy_request_pending", purpose),
            prompt.fresh() ? ChatFormatting.AQUA : ChatFormatting.GRAY,
            target.getGameProfile().getName()), false);
    }

    public static GrantReservation reserveGrant(MinecraftServer server, UUID targetId,
                                                UUID requesterId, PortalRequestPurpose purpose) {
        PortalPrivacyLedger.RequestKey key = new PortalPrivacyLedger.RequestKey(targetId, requesterId, purpose);
        PortalPrivacyLedger.TimedParties grant = LEDGER.reserveGrant(key, gameTime(server));
        return grant == null ? null : new GrantReservation(key, grant);
    }

    public static void releaseGrant(MinecraftServer server, GrantReservation reservation) {
        if (reservation != null) {
            LEDGER.restoreGrant(reservation.key(), reservation.grant(), gameTime(server));
        }
    }

    public static boolean reservationValid(MinecraftServer server, GrantReservation reservation) {
        return reservation != null && gameTime(server) < reservation.grant().expiresAt();
    }

    public static boolean allowsWithoutRequest(ServerPlayer target, UUID requesterId,
                                               PortalRequestPurpose purpose) {
        return target.getUUID().equals(requesterId)
            || effectivePolicy(PortalDataStore.load(target), requesterId,
                purpose.permissionId()) == PortalPermissionPolicy.ALLOW;
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
            case ALWAYS_ALLOW -> applyPermissionOverride(target, parties.requesterId(),
                parties.purpose().permissionId(), PortalPermissionPolicy.ALLOW);
            case DENY_ONCE -> LEDGER.denyOnce(
                parties.key(), gameTime(server), denyOnceCooldownTicks());
            case ALWAYS_DENY -> applyPermissionOverride(target, parties.requesterId(),
                parties.purpose().permissionId(), PortalPermissionPolicy.DENY);
        }
        notifyResponse(server, target, parties, response);
        return RespondResult.APPLIED;
    }

    /** Records a permanent override from the terminal or an Always response. */
    public static void applyProfileMode(ServerPlayer target, UUID requesterId,
                                        PlayerPermissionProfileMode mode) {
        PortalPlayerData targetData = PortalDataStore.load(target);
        targetData.permissionProfileMode(requesterId, mode);
        PortalDataStore.save(target, targetData);
        for (PortalRequestPurpose purpose : PortalRequestPurpose.values()) {
            LEDGER.clearTransient(new PortalPrivacyLedger.RequestKey(
                target.getUUID(), requesterId, purpose));
        }
    }

    public static void applyPermissionOverride(ServerPlayer target, UUID requesterId,
                                               ResourceLocation permissionId,
                                               PortalPermissionPolicy policy) {
        PortalPermissionDefinition definition = PortalPermissions.definition(permissionId);
        if (definition == null || policy == PortalPermissionPolicy.ASK && !definition.supportsAsk()) return;
        PortalPlayerData targetData = PortalDataStore.load(target);
        targetData.permissionProfile(requesterId).customize(permissionId, policy);
        PortalDataStore.save(target, targetData);
        clearTransient(target.getUUID(), requesterId, permissionId);
    }

    public static void applyGlobalPermission(ServerPlayer target, ResourceLocation permissionId,
                                             PortalPermissionPolicy policy) {
        PortalPermissionDefinition definition = PortalPermissions.definition(permissionId);
        if (definition == null || policy == PortalPermissionPolicy.FOLLOW_GLOBAL
            || policy == PortalPermissionPolicy.ASK && !definition.supportsAsk()) return;
        PortalPlayerData data = PortalDataStore.load(target);
        data.globalPermission(permissionId, policy);
        PortalDataStore.save(target, data);
        LEDGER.clearTarget(target.getUUID());
    }

    /** Sends a requester-facing denial through chat, preserving the reason. */
    public static void notifyDenied(ServerPlayer requester, ServerPlayer target, Access access) {
        notifyDenied(requester, target, access, PortalRequestPurpose.PORTAL);
    }

    public static void notifyDenied(ServerPlayer requester, ServerPlayer target, Access access,
                                    PortalRequestPurpose purpose) {
        String targetName = target.getGameProfile().getName();
        Component message = switch (access.outcome()) {
            case ALWAYS_DENIED -> styled(purposeKey("chat.riftgun.privacy_always_denied_requester", purpose),
                ChatFormatting.RED, targetName);
            case DENIED_ONCE -> styled(purposeKey("chat.riftgun.privacy_deny_once_cooldown", purpose),
                ChatFormatting.RED, targetName, access.retryAfterSeconds());
            default -> styled(purposeKey("chat.riftgun.privacy_denied_requester", purpose),
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

    /** True when another owner's exit portal may carry {@code target}. */
    public static boolean allowsForeignExitTransit(ServerPlayer target, UUID ownerId) {
        if (ownerId != null && ownerId.equals(target.getUUID())) return true;
        return effectivePolicy(PortalDataStore.load(target), ownerId,
            PortalPermissions.FOREIGN_EXIT_TRANSIT) == PortalPermissionPolicy.ALLOW;
    }

    private static PortalPermissionPolicy effectivePolicy(PortalPlayerData data,
                                                          UUID requesterId,
                                                          ResourceLocation permissionId) {
        if (requesterId == null) return data.globalPermission(permissionId);
        PlayerPermissionProfile profile = data.permissionProfile(requesterId);
        PortalPermissionPolicy configured = profile.configured(permissionId);
        return configured == PortalPermissionPolicy.FOLLOW_GLOBAL
            ? data.globalPermission(permissionId) : configured;
    }

    private static void clearTransient(UUID targetId, UUID requesterId,
                                       ResourceLocation permissionId) {
        for (PortalRequestPurpose purpose : PortalRequestPurpose.values()) {
            if (purpose.permissionId().equals(permissionId)) {
                LEDGER.clearTransient(new PortalPrivacyLedger.RequestKey(targetId, requesterId, purpose));
            }
        }
    }

    private static Access requestAccess(ServerPlayer target, ServerPlayer requester,
                                        PortalRequestPurpose purpose, long now) {
        PortalPrivacyLedger.RequestKey key = new PortalPrivacyLedger.RequestKey(
            target.getUUID(), requester.getUUID(), purpose);
        if (LEDGER.hasGrant(key, now)) return Access.grantedOnce();
        long remaining = LEDGER.denyRemainingTicks(key, now);
        if (remaining > 0L) {
            return Access.deniedOnce(secondsRoundedUp(remaining));
        }
        return Access.requested();
    }

    private static void sendRequestPrompt(ServerPlayer target, ServerPlayer requester, UUID token,
                                          PortalRequestPurpose purpose) {
        target.displayClientMessage(styled(purposeKey("chat.riftgun.privacy_request_received", purpose),
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
        targetKey = purposeKey(targetKey, parties.purpose());
        requesterKey = purposeKey(requesterKey, parties.purpose());
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
                purposeKey("chat.riftgun.privacy_request_expired_target", parties.purpose()), ChatFormatting.YELLOW,
                parties.requesterName()), false);
            if (requester != null) requester.displayClientMessage(styled(
                purposeKey("chat.riftgun.privacy_request_expired_requester", parties.purpose()), ChatFormatting.YELLOW,
                parties.targetName()), false);
        } else {
            if (target != null) target.displayClientMessage(styled(
                purposeKey("chat.riftgun.privacy_grant_expired_target", parties.purpose()), ChatFormatting.YELLOW,
                parties.requesterName()), false);
            if (requester != null) requester.displayClientMessage(styled(
                purposeKey("chat.riftgun.privacy_grant_expired_requester", parties.purpose()), ChatFormatting.YELLOW,
                parties.targetName()), false);
        }
    }

    private static PortalPrivacyLedger.Parties parties(ServerPlayer target, ServerPlayer requester,
                                                        PortalRequestPurpose purpose) {
        return new PortalPrivacyLedger.Parties(
            target.getUUID(), target.getGameProfile().getName(),
            requester.getUUID(), requester.getGameProfile().getName(), purpose);
    }

    private static Component styled(String key, ChatFormatting color, Object... arguments) {
        return Component.translatable(key, arguments).withStyle(color);
    }

    private static int secondsRoundedUp(long ticks) {
        return (int) Math.max(1L, (ticks + TICKS_PER_SECOND - 1L) / TICKS_PER_SECOND);
    }

    private static long requestTtlTicks() {
        return (long) RiftConfigs.server().privacy().requestTimeoutSeconds() * TICKS_PER_SECOND;
    }

    private static long grantTtlTicks() {
        return (long) RiftConfigs.server().privacy().grantTimeoutSeconds() * TICKS_PER_SECOND;
    }

    private static long denyOnceCooldownTicks() {
        return (long) RiftConfigs.server().privacy().denyOnceCooldownSeconds() * TICKS_PER_SECOND;
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

    private static String purposeKey(String base, PortalRequestPurpose purpose) {
        return base + purpose.languageSuffix();
    }

    public static final class GrantReservation {
        private final PortalPrivacyLedger.RequestKey key;
        private final PortalPrivacyLedger.TimedParties grant;

        private GrantReservation(PortalPrivacyLedger.RequestKey key,
                                 PortalPrivacyLedger.TimedParties grant) {
            this.key = key;
            this.grant = grant;
        }

        private PortalPrivacyLedger.RequestKey key() { return key; }
        private PortalPrivacyLedger.TimedParties grant() { return grant; }
    }

    private PortalPrivacyService() {}
}
