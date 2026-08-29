package dev.riftgun.client.render;

import dev.riftgun.client.PortalClientState;
import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingEndpoint;
import dev.riftgun.pairing.PortalPairingPreviewGeometry;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.service.PortalGunIdentity;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Selects the held gun's dormant entity-relocation target for marker rendering. */
final class PortalPairingEntityTargetPreview {
    static List<PortalPairingPreviewGeometry.ColoredSegment> segments(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) return List.of();
        ItemStack gun = heldGun(minecraft);
        if (gun.isEmpty()) return List.of();
        int smartDistance = PortalClientState.data().settings().smartDistance();
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            gun, smartDistance, PortalClientState.moduleRules());
        if (capabilities.functionMode() != PortalFunctionMode.PORTAL_PAIRING
            || capabilities.effectivePlacementMode(
                PortalClientState.data().settings().placementMode())
                != PortalPlacementMode.ENTITY_RELOCATION) return List.of();
        UUID gunId = PortalGunIdentity.existing(gun);
        if (gunId == null) return List.of();
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof PortalEntity portal)
                || !portal.pairingDormant()
                || portal.pairingEndpoint() != PortalPairingEndpoint.ENTITY_TARGET
                || !gunId.equals(portal.pairingGunId())) continue;
            return PortalPairingPreviewGeometry.entityTargetSegments(portal.placement());
        }
        return List.of();
    }

    private static ItemStack heldGun(Minecraft minecraft) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.is(RiftContent.PORTAL_GUN.get())) return mainHand;
        ItemStack offhand = minecraft.player.getOffhandItem();
        return offhand.is(RiftContent.PORTAL_GUN.get()) ? offhand : ItemStack.EMPTY;
    }

    private PortalPairingEntityTargetPreview() {}
}
