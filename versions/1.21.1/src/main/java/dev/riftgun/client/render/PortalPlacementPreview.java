package dev.riftgun.client.render;

import dev.riftgun.client.PortalClientState;
import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.fuel.PortalFuelProfiles;
import dev.riftgun.fuel.PortalGunTank;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.service.PortalShortcutGunMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Local-only approximate outline; the server still recomputes and validates the operation. */
public final class PortalPlacementPreview {
    private static final int EMISSION_INTERVAL_TICKS = 3;
    private static final double SURFACE_OFFSET = 0.08;

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null
            || minecraft.isPaused() || minecraft.player.tickCount % EMISSION_INTERVAL_TICKS != 0) return;
        ItemStack gun = findAvailableGun(minecraft);
        if (gun.isEmpty()) return;
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            gun, PortalClientState.data().settings().smartDistance());
        if (!capabilities.portalPairing()) return;

        PortalPlacementMode mode = PortalClientState.data().settings().placementMode();
        boolean pairing = capabilities.functionMode() == PortalFunctionMode.PORTAL_PAIRING;
        Marker marker = Marker.GENERIC;
        boolean fixedTarget = pairing && mode == PortalPlacementMode.ENTITY_RELOCATION;
        if (fixedTarget) {
            if (!minecraft.player.isShiftKeyDown()) return;
            marker = Marker.TARGET;
        } else {
            if (mode == PortalPlacementMode.SMART
                && capabilities.activeSmartFallback() != PortalFloatingFallback.REMOTE) return;
            if (mode != PortalPlacementMode.REMOTE && mode != PortalPlacementMode.SMART) return;
            if (pairing) marker = minecraft.player.isShiftKeyDown() ? Marker.A : Marker.B;
        }

        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle().normalize();
        double range = capabilities.configuredSurfaceRange();
        HitResult raw = minecraft.level.clip(new ClipContext(eye, eye.add(look.scale(range)),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
        BlockHitResult hit = raw instanceof BlockHitResult block && raw.getType() == HitResult.Type.BLOCK
            ? block : null;
        if (!fixedTarget && mode == PortalPlacementMode.SMART && hit != null
            && eye.distanceTo(hit.getLocation()) <= capabilities.smartDistance()) return;

        PortalPlacement placement = fixedTarget && hit != null
            ? surfacePreview(minecraft, hit)
            : remotePreview(minecraft, capabilities, eye, look, hit, range);
        emit(minecraft, placement, marker, color(gun));
    }

    private static PortalPlacement surfacePreview(Minecraft minecraft, BlockHitResult hit) {
        Direction face = hit.getDirection();
        Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        PortalOrientation orientation = face.getAxis().isVertical()
            ? face == Direction.UP ? PortalOrientation.TOP : PortalOrientation.BOTTOM
            : PortalOrientation.VERTICAL;
        PortalGeometry geometry = face.getAxis().isVertical()
            ? PortalGeometry.HORIZONTAL : PortalGeometry.SURFACE_VERTICAL;
        Vec3 center = hit.getLocation().add(normal.scale(SURFACE_OFFSET));
        return new PortalPlacement(center, orientation, geometry, yawFromNormal(normal), null, null);
    }

    private static PortalPlacement remotePreview(Minecraft minecraft, PortalGunCapabilities capabilities,
                                                 Vec3 eye, Vec3 look, BlockHitResult hit, double range) {
        double distance = hit == null ? range : Math.max(1.5, eye.distanceTo(hit.getLocation()) - 0.18);
        PortalOrientation orientation = horizontalOrientation(minecraft.player.getXRot());
        PortalGeometry geometry = orientation == PortalOrientation.VERTICAL
            ? capabilities.aperture() == PortalAperture.EXPANDED ? PortalGeometry.FLOATING_EXPANDED
                : PortalGeometry.FLOATING_VERTICAL
            : capabilities.aperture() == PortalAperture.EXPANDED ? PortalGeometry.HORIZONTAL_EXPANDED
                : PortalGeometry.HORIZONTAL;
        return new PortalPlacement(eye.add(look.scale(distance)), orientation, geometry,
            minecraft.player.getYRot(), null, null);
    }

    private static PortalOrientation horizontalOrientation(float pitch) {
        float threshold = PortalClientState.horizontalPortalPitch();
        if (pitch >= threshold) return PortalOrientation.TOP;
        if (pitch <= -threshold) return PortalOrientation.BOTTOM;
        return PortalOrientation.VERTICAL;
    }

    private static void emit(Minecraft minecraft, PortalPlacement placement, Marker marker, int color) {
        switch (marker) {
            case GENERIC, A -> ring(minecraft, placement, 1.04, 16, false, color);
            case B -> {
                ring(minecraft, placement, 1.06, 16, true, color);
                ring(minecraft, placement, 0.88, 16, true, color);
            }
            case TARGET -> {
                ring(minecraft, placement, 0.48, 12, false, color);
                point(minecraft, placement, -0.82, 0.0, color);
                point(minecraft, placement, 0.82, 0.0, color);
                point(minecraft, placement, 0.0, -0.82, color);
                point(minecraft, placement, 0.0, 0.82, color);
            }
        }
    }

    private static void ring(Minecraft minecraft, PortalPlacement placement, double scale,
                             int count, boolean segmented, int color) {
        for (int index = 0; index < count; index++) {
            if (segmented && index % 4 == 3) continue;
            double angle = Math.PI * 2.0 * index / count;
            point(minecraft, placement, Math.cos(angle) * scale, Math.sin(angle) * scale, color);
        }
    }

    private static void point(Minecraft minecraft, PortalPlacement placement,
                              double normalizedRight, double normalizedUp, int color) {
        Vec3 position = placement.center()
            .add(placement.right().scale(normalizedRight * placement.geometry().width() * 0.5))
            .add(placement.up().scale(normalizedUp * placement.geometry().height() * 0.5))
            .add(placement.normal().scale(0.04));
        Particle particle = minecraft.particleEngine.createParticle(RiftContent.PORTAL_SPLASH.get(),
            position.x, position.y, position.z, 0.0, 0.0, 0.0);
        if (particle != null) particle.setColor(red(color), green(color), blue(color));
    }

    private static ItemStack findAvailableGun(Minecraft minecraft) {
        if (minecraft.player == null) return ItemStack.EMPTY;
        ItemStack main = minecraft.player.getMainHandItem();
        if (main.is(RiftContent.PORTAL_GUN.get())) return main;
        ItemStack offhand = minecraft.player.getOffhandItem();
        if (offhand.is(RiftContent.PORTAL_GUN.get())) return offhand;
        if (PortalClientState.shortcutGunMode() == PortalShortcutGunMode.HELD_HANDS) return ItemStack.EMPTY;
        var inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(RiftContent.PORTAL_GUN.get())) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static int color(ItemStack gun) {
        return PortalFuelProfiles.resolve(new PortalGunTank(gun).getFluid().getFluid())
            .map(profile -> profile.rgb()).orElse(0xE7A450);
    }

    private static float yawFromNormal(Vec3 normal) {
        return (float) Math.toDegrees(Math.atan2(-normal.x, normal.z));
    }

    private static float red(int color) { return ((color >> 16) & 255) / 255.0F; }
    private static float green(int color) { return ((color >> 8) & 255) / 255.0F; }
    private static float blue(int color) { return (color & 255) / 255.0F; }

    private enum Marker { GENERIC, A, B, TARGET }

    private PortalPlacementPreview() { }
}
