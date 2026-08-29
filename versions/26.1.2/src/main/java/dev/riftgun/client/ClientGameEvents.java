package dev.riftgun.client;

import dev.riftgun.client.recipe.FluidRecipeCache;
import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.RiftGun;
import dev.riftgun.client.render.PortalSplashEmitter;
import dev.riftgun.client.render.PortalPlacementPreview;
import dev.riftgun.client.external.ClientMapWaypointIntegration;
import dev.riftgun.external.ExternalDestinationSelection;
import dev.riftgun.config.ClientConfig;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.fuel.PortalGunMode;
import dev.riftgun.fuel.PortalGunTank;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRegistry;
import dev.riftgun.module.PortalModules;
import dev.riftgun.module.PortalGunModuleSettings;
import dev.riftgun.module.PortalGunModules;
import dev.riftgun.portal.CoordinateNoteItem;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = RiftGun.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private static boolean connected;
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void recipesReceived(RecipesReceivedEvent event) {
        // Mirror AE2: keep the server-synced recipe map and advertised types,
        // gated on our type, before JEI (ListenerPriority.LOWEST) registers.
        FluidRecipeCache.setFrom(event.getRecipeMap(), event.getRecipeTypes());
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean nowConnected = minecraft.getConnection() != null;
        if (connected && !nowConnected) ClientMapWaypointIntegration.clear();
        connected = nowConnected;
        if (nowConnected) refreshJourneyMapSelection(minecraft);
        GuiCaptureHarness.tick(minecraft);
        PortalSplashEmitter.tick(minecraft);
        PortalPlacementPreview.tick(minecraft);
        ModeRadialInput.tick(minecraft);
        while (ClientModEvents.OPEN_CONFIG.consumeClick()) {
            if (minecraft.player != null && minecraft.getConnection() != null) {
                PortalNetworking.sendShortcutRequest(PortalAction.OPEN_GUI);
            }
        }
        while (ClientModEvents.FORCE_FRONT.consumeClick()) {
            sendForcedOpen(minecraft, PortalPlacementMode.FRONT);
        }
        while (ClientModEvents.FORCE_SURFACE.consumeClick()) {
            sendForcedOpen(minecraft, PortalPlacementMode.SURFACE);
        }
        while (ClientModEvents.CLOSE_PORTALS.consumeClick()) {
            if (minecraft.player != null && minecraft.getConnection() != null) {
                PortalNetworking.sendRequest(PortalAction.CLOSE_PORTALS);
            }
        }
        while (ClientModEvents.ENTITY_RELOCATION.consumeClick()) {
            if (minecraft.player != null && minecraft.getConnection() != null) {
                PortalNetworking.sendShortcutRequest(PortalAction.RELOCATE_ENTITY);
            }
        }
        while (ClientModEvents.PORTAL_PAIRING_OPERATION.consumeClick()) {
            if (minecraft.player != null && minecraft.getConnection() != null) {
                PortalNetworking.sendShortcutRequest(PortalAction.PLACE_PAIRING_ENDPOINT,
                    tag -> tag.putBoolean("EndpointA", minecraft.player.isShiftKeyDown()));
            }
        }
        while (ClientModEvents.TOGGLE_FUNCTION_MODE.consumeClick()) {
            if (minecraft.player != null && minecraft.getConnection() != null) {
                PortalNetworking.sendShortcutRequest(PortalAction.TOGGLE_FUNCTION_MODE);
            }
        }
    }

    @SubscribeEvent
    public static void hideSurfacePreviewBlockHighlight(ExtractBlockOutlineRenderStateEvent event) {
        if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.ModeRadialScreen screen
            && screen.surfaceFacePreviewOpen()) {
            event.setCanceled(true);
        }
    }

    private static void sendForcedOpen(Minecraft minecraft, PortalPlacementMode mode) {
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        PortalNetworking.sendShortcutRequest(PortalAction.OPEN_SELECTED,
            tag -> tag.putString("PlacementMode", mode.name()));
    }

    @SubscribeEvent
    public static void mouseScrolled(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null
            || !minecraft.player.isShiftKeyDown() || event.getScrollDeltaY() == 0.0) return;
        var gun = minecraft.player.getMainHandItem();
        if (!gun.is(RiftContent.PORTAL_GUN.get())
            || PortalClientState.data().settings().placementMode() != PortalPlacementMode.REMOTE
            || PortalGunModules.activeCount(gun, PortalModuleKind.REMOTE,
                PortalClientState.moduleRules()) <= 0
            || !PortalGunModuleSettings.get(gun,
                PortalClientState.data().settings().smartDistance())
                .portalPairing().remote().scrollAdjustmentEnabled()) return;
        event.setCanceled(true);
        PortalNetworking.sendShortcutRequest(PortalAction.ADJUST_SURFACE_RANGE,
            tag -> tag.putInt("Step", event.getScrollDeltaY() > 0.0 ? 1 : -1));
    }

    private static void refreshJourneyMapSelection(Minecraft minecraft) {
        if (!ClientMapWaypointIntegration.journeyMapDirty()) return;
        var dimensions = minecraft.getConnection().levels().stream()
            .map(key -> key.identifier().toString())
            .collect(java.util.stream.Collectors.toSet());
        if (ClientMapWaypointIntegration.refreshJourneyMapIfDirty(dimensions,
            ClientConfig.VALUES.maximumMapWaypoints.get())
            && ClientMapWaypointIntegration.reconcileSelection()) {
            PortalNetworking.sendRequest(PortalAction.CLEAR_EXTERNAL_DESTINATION);
        }
    }

    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof CoordinateNoteItem) {
            CoordinateNoteTooltipStyle.removeItalics(event.getToolTip());
            return;
        }
        if (event.getItemStack().is(PortalModules.BASIC_MODULE.get())
            || event.getItemStack().is(PortalModules.ADVANCED_BASIC_MODULE.get())) {
            String description = event.getItemStack().is(PortalModules.ADVANCED_BASIC_MODULE.get())
                ? "tooltip.riftgun.advanced_basic_module.description"
                : "tooltip.riftgun.basic_module.description";
            event.getToolTip().add(Component.translatable(description)
                .withStyle(style -> style.withColor(0xA9D6A2)));
            event.getToolTip().add(Component.translatable("tooltip.riftgun.basic_module.not_installable")
                .withStyle(ChatFormatting.GRAY));
            return;
        }
        var module = PortalModuleRegistry.find(event.getItemStack());
        if (module.isPresent()) {
            if (!Minecraft.getInstance().hasShiftDown()) {
                event.getToolTip().add(Component.translatable("tooltip.riftgun.module.hold_shift")
                    .withStyle(ChatFormatting.GRAY));
                return;
            }
            var definition = module.get();
            String descriptionKey = definition.kind() == PortalModuleKind.MATTER_ANCHOR
                && !PortalClientState.moduleRules().matterAnchorPreventsDespawn()
                ? "tooltip.riftgun.module.matter_anchor_module.damage_only_description"
                : definition.descriptionKey();
            event.getToolTip().add(Component.translatable(descriptionKey)
                .withStyle(style -> style.withColor(0xA9D6A2)));
            if (definition.kind() == PortalModuleKind.REMOTE) {
                event.getToolTip().add(Component.translatable(
                    "tooltip.riftgun.module.remote_module.scroll_control")
                    .withStyle(ChatFormatting.GRAY));
            }
            if (definition.kind() == PortalModuleKind.PORTAL_PAIRING) {
                event.getToolTip().add(Component.translatable(
                    "tooltip.riftgun.module.portal_pairing_module.use")
                    .withStyle(ChatFormatting.GRAY));
                event.getToolTip().add(Component.translatable(
                    "tooltip.riftgun.module.portal_pairing_module.shift_use")
                    .withStyle(ChatFormatting.GRAY));
            }
            event.getToolTip().add(Component.translatable("tooltip.riftgun.module.limit",
                definition.maximumCount(PortalClientState.moduleRules()))
                .withStyle(style -> style.withColor(0xE5A39C)));
            if (definition.kind() == PortalModuleKind.RESERVOIR_EXPANSION) {
                event.getToolTip().add(Component.translatable("tooltip.riftgun.module.reservoir_warning")
                    .withStyle(ChatFormatting.GOLD));
            }
            if (definition.kind() == PortalModuleKind.MODULE_BAY_EXPANSION) {
                event.getToolTip().add(Component.translatable("tooltip.riftgun.module.module_bay_warning")
                    .withStyle(ChatFormatting.GOLD));
            }
            if (definition.kind() == PortalModuleKind.ENTITY_RELOCATION) {
                event.getToolTip().add(Component.translatable(
                    "tooltip.riftgun.module.entity_relocation_fuel_warning")
                    .withStyle(ChatFormatting.DARK_RED));
            }
            return;
        }
        if (event.getItemStack().is(RiftContent.PRIVACY_TERMINAL_ITEM.get())) {
            if (!Minecraft.getInstance().hasShiftDown()) {
                event.getToolTip().add(Component.translatable("tooltip.riftgun.module.hold_shift")
                    .withStyle(ChatFormatting.GRAY));
                return;
            }
            event.getToolTip().add(Component.translatable("tooltip.riftgun.privacy_terminal.description")
                .withStyle(style -> style.withColor(0xA9D6A2)));
            return;
        }
        if (!event.getItemStack().is(RiftContent.PORTAL_GUN.get())) return;
        PortalGunTank tank = new PortalGunTank(event.getItemStack());
        var fluid = tank.getFluid();
        if (PortalGunMode.bucketMode(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("tooltip.riftgun.bucket_mode",
                Component.translatable("screen.riftgun.on")).withStyle(ChatFormatting.GRAY));
        }
        if (!fluid.isEmpty()) {
            int fluidRgb = dev.riftgun.fuel.PortalFuelProfiles.resolve(fluid.getFluid())
                .map(dev.riftgun.fuel.PortalFuelProfile::rgb).orElse(0xA7A39C);
            event.getToolTip().add(Component.translatable("tooltip.riftgun.fluid",
                fluid.getHoverName(), fluid.getAmount(), tank.nominalCapacity())
                .withStyle(style -> style.withColor(fluidRgb)));
            if (fluid.getAmount() > tank.nominalCapacity()) {
                event.getToolTip().add(Component.translatable("screen.riftgun.overfilled")
                    .withStyle(ChatFormatting.GOLD));
            }
        }
        if (dev.riftgun.fuel.PortalFuelManager.hasInfiniteFuel(event.getItemStack())) {
            int infiniteRgb = dev.riftgun.fuel.PortalFuelProfiles.resolve(fluid.getFluid())
                .map(dev.riftgun.fuel.PortalFuelProfile::rgb)
                .orElse(dev.riftgun.fuel.PortalFuelProfiles.DIMENSIONAL_RGB);
            event.getToolTip().add(Component.translatable("screen.riftgun.zero_point_fuel_active")
                .withStyle(style -> style.withColor(infiniteRgb)));
        }
        PortalPlayerData data = PortalClientState.data();
        UUID selectedId = data.selectedDestinationId();
        ExternalDestinationSelection external = ClientMapWaypointIntegration.selected();
        if (external != null) {
            event.getToolTip().add(Component.translatable("tooltip.riftgun.target", external.name())
                .withStyle(ChatFormatting.AQUA));
            event.getToolTip().add(Component.translatable("tooltip.riftgun.group",
                external.source().displayName()).withStyle(ChatFormatting.GRAY));
            String dimension = DimensionLabelState.label(external.dimensionId())
                .orElse(external.dimensionId());
            event.getToolTip().add(Component.translatable("tooltip.riftgun.dimension", dimension)
                .withStyle(ChatFormatting.GRAY));
            return;
        }
        if (selectedId == null) {
            event.getToolTip().add(Component.translatable("tooltip.riftgun.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        Destination destination = data.destination(selectedId).orElse(null);
        if (destination == null) return;
        String group = destination.groupId().equals(PortalPlayerData.DEFAULT_GROUP_ID)
            ? Component.translatable("screen.riftgun.default_group").getString()
            : data.group(destination.groupId()).map(value -> value.name())
                .orElseGet(() -> Component.translatable("screen.riftgun.default_group").getString());
        event.getToolTip().add(Component.translatable("tooltip.riftgun.target", destination.name()).withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.translatable("tooltip.riftgun.group", group).withStyle(ChatFormatting.GRAY));
        String dimensionId = destination.dimension().identifier().toString();
        String dimension = DimensionLabelState.label(dimensionId).orElse(dimensionId);
        event.getToolTip().add(Component.translatable("tooltip.riftgun.dimension", dimension).withStyle(ChatFormatting.GRAY));
    }

    private ClientGameEvents() {}
}
