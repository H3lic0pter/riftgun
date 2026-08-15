package dev.riftgun.lifecycle;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.core.runtime.RiftRuntime;
import dev.riftgun.crisis.PortalCrisisRegistry;
import dev.riftgun.crisis.PortalCrisisTestOverrides;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.diagnostics.TransitDiagnostics;
import dev.riftgun.entity.SpecialEntityTransitPolicies;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.SweptPortalIndex;
import dev.riftgun.relocation.EntityRelocationExitImmunity;
import dev.riftgun.relocation.EntityRelocationManager;
import dev.riftgun.service.PortalPrivacyService;
import dev.riftgun.sound.PortalSounds;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Loader-neutral entry points for the server lifecycle and gameplay events. */
public final class RiftLifecycle {
    public static void serverStarting(MinecraftServer server) {
        PortalCrisisRegistry.freeze();
        SpecialEntityTransitPolicies.rebuild(server.registryAccess());
    }

    public static void tagsUpdated(RegistryAccess access) {
        SpecialEntityTransitPolicies.rebuild(access);
        SweptPortalIndex.reconcileSpecialEntities();
    }

    public static void serverTick(MinecraftServer server) {
        RiftRuntime.current().motionHistory().tick(server);
        PortalPrivacyService.tick(server);
        EntityRelocationManager.tick(server);
        EntityRelocationExitImmunity.tick(server.overworld().getGameTime());
        TransitDiagnostics.tick(server);
        SweptPortalIndex.serverTick();
    }

    public static void levelTickBeforeEntities(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            SweptPortalIndex.beforeEntityTicks(serverLevel);
        }
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
        if (entity.level().isClientSide()) return;
        if (entity instanceof Projectile projectile) SweptPortalIndex.track(projectile);
        SweptPortalIndex.trackSpecialEntity(entity);
    }

    public static void entityLeft(Entity entity) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof Projectile projectile) SweptPortalIndex.untrack(projectile);
        SweptPortalIndex.untrackSpecialEntity(entity);
    }

    public static boolean projectileImpact(Projectile projectile, HitResult hit) {
        return !projectile.level().isClientSide()
            && SweptPortalIndex.tryTransit(projectile, hit);
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
        SweptPortalIndex.reset();
        SpecialEntityTransitPolicies.reset();
    }

    private static void updatePrediction(ServerPlayer player) {
        RiftRuntime.current().motionHistory().setPredictionEnabled(player,
            PortalDataStore.load(player).settings().predictionMode() != PortalPredictionMode.OFF);
    }

    private static void closeOwned(ServerPlayer player) {
//? if >=1.21.11 {
        /*MinecraftServer server = player.level().getServer();
*///?} else {
        MinecraftServer server = player.getServer();
//?}
        if (server != null) PortalEntity.closeOwnedPortals(server, player.getUUID());
    }

    private RiftLifecycle() {}
}
