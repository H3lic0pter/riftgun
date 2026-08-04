package dev.riftgun.client;

import dev.riftgun.RiftGun;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
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
        while (ClientModEvents.OPEN_CONFIG.consumeClick()) {
            if (minecraft.player != null && minecraft.getConnection() != null) {
                PortalNetworking.sendRequest(PortalAction.OPEN_GUI);
            }
        }
    }

    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(RiftGun.PORTAL_GUN.get())) return;
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
