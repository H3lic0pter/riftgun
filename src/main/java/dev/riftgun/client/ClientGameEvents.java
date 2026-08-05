package dev.riftgun.client;

import dev.riftgun.RiftGun;
import dev.riftgun.client.render.PortalSplashEmitter;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.fuel.PortalGunMode;
import dev.riftgun.fuel.PortalGunTank;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = RiftGun.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        GuiCaptureHarness.tick(minecraft);
        PortalSplashEmitter.tick(minecraft);
        while (ClientModEvents.OPEN_CONFIG.consumeClick()) {
            if (minecraft.player != null && minecraft.getConnection() != null) {
                PortalNetworking.sendRequest(PortalAction.OPEN_GUI);
            }
        }
        while (ClientModEvents.CYCLE_PLACEMENT.consumeClick()) {
            if (minecraft.player != null && minecraft.getConnection() != null) {
                PortalNetworking.sendRequest(PortalAction.CYCLE_PLACEMENT_MODE);
            }
        }
        while (ClientModEvents.FORCE_FRONT.consumeClick()) {
            sendForcedOpen(minecraft, PortalPlacementMode.FRONT);
        }
        while (ClientModEvents.FORCE_SURFACE.consumeClick()) {
            sendForcedOpen(minecraft, PortalPlacementMode.SURFACE);
        }
    }

    private static void sendForcedOpen(Minecraft minecraft, PortalPlacementMode mode) {
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        PortalNetworking.sendRequest(PortalAction.OPEN_SELECTED,
            tag -> tag.putString("PlacementMode", mode.name()));
    }

    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(RiftGun.PORTAL_GUN.get())) return;
        PortalGunTank tank = new PortalGunTank(event.getItemStack());
        var fluid = tank.getFluid();
        event.getToolTip().add(Component.translatable("tooltip.riftgun.bucket_mode",
            Component.translatable(PortalGunMode.bucketMode(event.getItemStack())
                ? "screen.riftgun.on" : "screen.riftgun.off")).withStyle(ChatFormatting.GRAY));
        if (!fluid.isEmpty()) {
            int fluidRgb = dev.riftgun.fuel.PortalFuelProfiles.resolve(fluid)
                .map(dev.riftgun.fuel.PortalFuelProfile::rgb).orElse(0xA7A39C);
            event.getToolTip().add(Component.translatable("tooltip.riftgun.fluid",
                fluid.getHoverName(), fluid.getAmount(), PortalGunTank.NOMINAL_CAPACITY)
                .withStyle(style -> style.withColor(fluidRgb)));
            if (fluid.getAmount() > PortalGunTank.NOMINAL_CAPACITY) {
                event.getToolTip().add(Component.translatable("screen.riftgun.overfilled")
                    .withStyle(ChatFormatting.GOLD));
            }
        }
        PortalPlayerData data = PortalClientState.data();
        UUID selectedId = data.selectedDestinationId();
        if (selectedId == null) {
            event.getToolTip().add(Component.translatable("tooltip.riftgun.no_target").withStyle(ChatFormatting.GRAY));
            return;
        }
        Destination destination = data.destination(selectedId).orElse(null);
        if (destination == null) return;
        String group = destination.groupId().equals(PortalPlayerData.DEFAULT_GROUP_ID)
            ? "Default"
            : data.group(destination.groupId()).map(value -> value.name()).orElse("Default");
        event.getToolTip().add(Component.translatable("tooltip.riftgun.target", destination.name()).withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.translatable("tooltip.riftgun.group", group).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable("tooltip.riftgun.dimension", destination.dimension().location()).withStyle(ChatFormatting.GRAY));
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.level().dimension().equals(destination.dimension())) {
            double distance = minecraft.player.position().distanceTo(destination.position());
            event.getToolTip().add(Component.translatable("tooltip.riftgun.distance",
                String.format(Locale.ROOT, "%.1f", distance)).withStyle(ChatFormatting.GRAY));
        }
    }

    private ClientGameEvents() {}
}
