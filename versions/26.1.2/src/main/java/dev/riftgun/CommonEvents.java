package dev.riftgun;

import dev.riftgun.fuel.PortalFuelProfileReloadListener;
import dev.riftgun.lifecycle.RiftLifecycle;
import dev.riftgun.recipe.RiftGunRecipes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = RiftGun.MOD_ID)
public final class CommonEvents {
    @SubscribeEvent
    public static void addReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(net.minecraft.resources.Identifier.fromNamespaceAndPath(
            "riftgun", "portal_fuel_profiles"),
            new PortalFuelProfileReloadListener(event.getRegistryAccess()));
    }

    @SubscribeEvent
    public static void serverAboutToStart(ServerAboutToStartEvent event) {
        RiftLifecycle.serverStarting(event.getServer());
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            RiftLifecycle.tagsUpdated(event.getRegistries());
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer replacement) {
            RiftLifecycle.playerCloned(original, replacement);
        }
    }

    @SubscribeEvent
    public static void datapackSync(OnDatapackSyncEvent event) {
        // Advertise the fluid recipe type so the server includes it (and its
        // recipes) in the RecipeContentPayload sent to NeoForge clients; without
        // this the synced recipe map on the client never carries our recipes and
        // JEI cannot display the fluid transmutations.
        event.sendRecipes(RiftGunRecipes.FLUID_TRANSMUTATION_TYPE.get());
    }

    @SubscribeEvent
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RiftLifecycle.playerJoined(player);
        }
    }

    @SubscribeEvent
    public static void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RiftLifecycle.playerLeft(player);
        }
    }

    @SubscribeEvent
    public static void playerDeath(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RiftLifecycle.playerRespawned(player);
        }
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RiftLifecycle.playerChangedDimension(player);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        RiftLifecycle.serverTick(event.getServer());
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Pre event) {
        RiftLifecycle.levelTickBeforeEntities(event.getLevel());
    }

    @SubscribeEvent
    public static void entityJoined(EntityJoinLevelEvent event) {
        RiftLifecycle.entityJoined(event.getEntity());
    }

    @SubscribeEvent
    public static void entityLeft(EntityLeaveLevelEvent event) {
        RiftLifecycle.entityLeft(event.getEntity());
    }

    @SubscribeEvent
    public static void projectileImpact(ProjectileImpactEvent event) {
        if (RiftLifecycle.projectileImpact(event.getProjectile(), event.getRayTraceResult())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        RiftLifecycle.serverStopped(event.getServer());
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        RiftLifecycle.serverStopping(event.getServer());
    }

    private CommonEvents() {}
}
