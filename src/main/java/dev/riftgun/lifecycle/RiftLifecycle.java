package dev.riftgun.lifecycle;

import dev.riftgun.core.runtime.RiftRuntime;
import dev.riftgun.crisis.PortalCrisisRegistry;
import dev.riftgun.crisis.PortalCrisisTestOverrides;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.diagnostics.TransitDiagnostics;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.ProjectilePortalIndex;
import dev.riftgun.relocation.EntityRelocationExitImmunity;
import dev.riftgun.relocation.EntityRelocationManager;
import dev.riftgun.service.PortalPrivacyService;
import dev.riftgun.sound.PortalSounds;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

/** Loader-neutral entry points for the server lifecycle and gameplay events. */
public final class RiftLifecycle {
    public static void serverStarting(MinecraftServer server) {
        PortalCrisisRegistry.freeze();
    }

    public static void serverTick(MinecraftServer server) {
        RiftRuntime.current().motionHistory().tick(server);
        PortalPrivacyService.tick(server);
        EntityRelocationManager.tick(server);
        EntityRelocationExitImmunity.tick(server.overworld().getGameTime());
        TransitDiagnostics.tick(server);
        ProjectilePortalIndex.tick(server);
    }

    public static void playerJoined(ServerPlayer player) {
        updatePrediction(player);
        PortalNetworking.sendSnapshot(player, false);
    }

    public static void playerLeft(ServerPlayer player) {
        UUID playerId = player.getUUID();
        RiftRuntime.current().motionHistory().remove(playerId);
        PortalCrisisTestOverrides.clear(playerId);
        closeOwned(player);
    }

    public static void playerCloned(ServerPlayer original, ServerPlayer replacement) {
        PortalDataStore.copy(original, replacement);
        RiftRuntime.current().motionHistory().remove(original.getUUID());
        updatePrediction(replacement);
    }

    public static void playerRespawned(ServerPlayer player) {
        RiftRuntime.current().motionHistory().reset(player);
        closeOwned(player);
    }

    public static void playerChangedDimension(ServerPlayer player) {
        RiftRuntime.current().motionHistory().reset(player);
    }

    public static void entityJoined(Entity entity) {
        if (entity instanceof Projectile projectile && !entity.level().isClientSide()) {
            ProjectilePortalIndex.track(projectile);
        }
    }

    public static void entityLeft(Entity entity) {
        if (entity instanceof Projectile projectile && !entity.level().isClientSide()) {
            ProjectilePortalIndex.untrack(projectile);
        }
    }

    public static boolean projectileImpact(Projectile projectile, HitResult hit) {
        return !projectile.level().isClientSide()
            && ProjectilePortalIndex.tryTransit(projectile, hit);
    }

    public static void serverStopping(MinecraftServer server) {
        PortalSounds.beginServerShutdown();
        EntityRelocationManager.cancelAll(server);
    }

    public static void serverStopped(MinecraftServer server) {
        PortalPrivacyService.reset();
        PortalCrisisTestOverrides.reset();
        PortalSounds.endServerShutdown();
        EntityRelocationManager.reset();
        TransitDiagnostics.reset();
        EntityRelocationExitImmunity.reset();
        ProjectilePortalIndex.reset();
    }

    private static void updatePrediction(ServerPlayer player) {
        RiftRuntime.current().motionHistory().setPredictionEnabled(player,
            PortalDataStore.load(player).settings().predictionMode() != PortalPredictionMode.OFF);
    }

    private static void closeOwned(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) PortalEntity.closeOwnedPortals(server, player.getUUID());
    }

    private RiftLifecycle() {}
}
