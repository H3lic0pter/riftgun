package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.RiftGun;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.client.screen.ModeRadialScreen;
import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.module.PortalGunModuleSettings;
import dev.riftgun.pairing.PortalPairingPendingEndpoint;
import dev.riftgun.pairing.PortalPairingPendingEndpoints;
import dev.riftgun.pairing.PortalPairingPreviewGeometry;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacementPreviewCache;
import dev.riftgun.portal.PortalPlacementPreviewGeometry;
import dev.riftgun.service.PortalPlacementCapabilities;
import dev.riftgun.service.FrontHorizontalPortalPlacement;
import dev.riftgun.service.RemotePortalPlacementResolver;
import dev.riftgun.service.SurfaceFacePlacementPlanner;
import dev.riftgun.service.PortalSupportArea;
import dev.riftgun.network.SurfaceFaceRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Objects;

/** Client-only MVP for the REMOTE placement footprint. */
@EventBusSubscriber(modid = RiftGun.MOD_ID, value = Dist.CLIENT)
public final class PortalPlacementPreview {
    private static final int COLOR = 0xD9F0F0F0;
    private static final PortalPlacementPreviewCache CACHE = new PortalPlacementPreviewCache();
    private static PortalPairingPendingEndpoint pendingEndpoint;
    private static List<PortalPairingPreviewGeometry.ColoredSegment> pendingSegments = List.of();
    private static List<PortalPairingPreviewGeometry.ColoredSegment> entityTargetSegments = List.of();
    private static Object levelIdentity;

    public static void tick(Minecraft minecraft) {
        if (minecraft.level != levelIdentity) {
            levelIdentity = minecraft.level;
            CACHE.clear();
            clearPending();
            clearEntityTarget();
        }
        tickPending(minecraft);
        tickEntityTarget(minecraft);
        long tick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (tickPrecision(minecraft, tick)) return;
        PortalPlacementPreviewCache.Input input = input(minecraft);
        if (input == null) {
            CACHE.clear();
            return;
        }
        tick = minecraft.level.getGameTime();
        if (!CACHE.shouldRefresh(tick, input)) return;
        PortalPlacement placement = RemotePortalPlacementResolver.resolve(
            minecraft.level, minecraft.player, input.range(), input.aperture(),
            PortalPlacementCapabilities.DEFAULT_DOWNSHOT_MINIMUM_PITCH,
            PortalPlacementCapabilities.DEFAULT_MINIMUM_FLOATING_PORTAL_EXPOSURE).orElse(null);
        CACHE.update(tick, input, placement);
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        List<PortalPlacementPreviewGeometry.Segment> segments = CACHE.segments();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
            || segments.isEmpty() && pendingSegments.isEmpty()
                && entityTargetSegments.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType renderType = RenderType.lines();
        draw(poses.last(), buffers.getBuffer(renderType), segments);
        drawColored(poses.last(), buffers.getBuffer(renderType), pendingSegments);
        drawColored(poses.last(), buffers.getBuffer(renderType), entityTargetSegments);
        buffers.endBatch(renderType);
        poses.popPose();
    }

    private static void tickPending(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            clearPending();
            return;
        }
        ItemStack gun = heldGun(minecraft);
        if (!portalEndpointMode(minecraft, gun)) {
            clearPending();
            return;
        }
        PortalPairingPendingEndpoint next = gun.isEmpty()
            ? null : PortalPairingPendingEndpoints.get(gun);
        if (next == null || !minecraft.level.dimension().equals(next.dimension())
            || !minecraft.level.hasChunkAt(BlockPos.containing(next.placement().center()))) {
            clearPending();
            return;
        }
        if (Objects.equals(next, pendingEndpoint)) return;
        pendingEndpoint = next;
        pendingSegments = PortalPairingPreviewGeometry.segments(
            next.placement(), next.endpoint());
    }

    private static void clearPending() {
        pendingEndpoint = null;
        pendingSegments = List.of();
    }

    private static void tickEntityTarget(Minecraft minecraft) {
        entityTargetSegments = PortalPairingEntityTargetPreview.segments(minecraft);
    }

    private static void clearEntityTarget() {
        entityTargetSegments = List.of();
    }

    private static boolean portalEndpointMode(Minecraft minecraft, ItemStack gun) {
        if (gun.isEmpty()) return false;
        int smartDistance = PortalClientState.data().settings().smartDistance();
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            gun, smartDistance, PortalClientState.moduleRules());
        return capabilities.effectivePlacementMode(
                PortalClientState.data().settings().placementMode())
                != PortalPlacementMode.ENTITY_RELOCATION;
    }

    private static PortalPlacementPreviewCache.Input input(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null
            || minecraft.isPaused()) return null;
        ItemStack gun = heldGun(minecraft);
        if (gun.isEmpty()) return null;
        int smartDistance = PortalClientState.data().settings().smartDistance();
        if (!PortalGunModuleSettings.get(gun, smartDistance)
            .portalPairing().remote().placementPreviewEnabled()) return null;
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(gun,
            smartDistance, PortalClientState.moduleRules());
        if (!capabilities.remote()
            || capabilities.effectivePlacementMode(PortalClientState.data().settings().placementMode())
                != PortalPlacementMode.REMOTE) return null;
        return new PortalPlacementPreviewCache.Input(
            minecraft.player.getEyePosition(), minecraft.player.getLookAngle(),
            capabilities.remoteDistance(), capabilities.aperture(),
            minecraft.player.getXRot(), minecraft.player.getYRot());
    }

    private static boolean tickPrecision(Minecraft minecraft, long tick) {
        if (!(minecraft.screen instanceof ModeRadialScreen screen)
            || !screen.surfaceFacePreviewOpen() && !screen.floatingOrientationPreviewOpen()) return false;
        ItemStack gun = heldGun(minecraft);
        if (gun.isEmpty()) {
            CACHE.clear();
            return true;
        }
        int smartDistance = PortalClientState.data().settings().smartDistance();
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            gun, smartDistance, PortalClientState.moduleRules());
        if (screen.floatingOrientationPreviewOpen()) {
            PortalOrientation orientation = screen.selectedFloatingOrientation();
            PortalPlacementMode mode = capabilities.effectivePlacementMode(
                PortalClientState.data().settings().placementMode());
            if (mode == PortalPlacementMode.SMART) {
                mode = capabilities.activeSmartFallback() == dev.riftgun.pairing.PortalFloatingFallback.REMOTE
                    ? PortalPlacementMode.REMOTE : PortalPlacementMode.FRONT;
            }
            int range = mode == PortalPlacementMode.REMOTE ? capabilities.remoteDistance() : 2;
            PortalPlacementPreviewCache.Input input = new PortalPlacementPreviewCache.Input(
                minecraft.player.getEyePosition(), minecraft.player.getLookAngle(), range,
                capabilities.aperture(), minecraft.player.getXRot(), minecraft.player.getYRot(),
                orientation);
            if (!CACHE.shouldRefresh(tick, input)) return true;
            PortalPlacement placement = mode == PortalPlacementMode.REMOTE
                ? RemotePortalPlacementResolver.resolve(
                    minecraft.level, minecraft.player, capabilities.remoteDistance(),
                    capabilities.aperture(), PortalPlacementCapabilities.DEFAULT_DOWNSHOT_MINIMUM_PITCH,
                    orientation, PortalPlacementCapabilities.DEFAULT_MINIMUM_FLOATING_PORTAL_EXPOSURE)
                    .orElse(null)
                : frontPreview(minecraft, capabilities, orientation);
            CACHE.update(tick, input, placement);
            return true;
        }
        BlockPos anchor = screen.surfaceAnchor();
        var face = screen.selectedSurfaceFace();
        int range = capabilities.maximumSurfaceRange();
        PortalPlacementPreviewCache.Input input = new PortalPlacementPreviewCache.Input(
            minecraft.player.getEyePosition(), minecraft.player.getLookAngle(), range,
            capabilities.aperture(), minecraft.player.getXRot(), minecraft.player.getYRot(),
            anchor, face);
        if (!CACHE.shouldRefresh(tick, input)) return true;
        Vec3 faceCenter = Vec3.atCenterOf(anchor).add(new Vec3(
            face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
        SurfaceFacePlacementPlanner.Result result = SurfaceFacePlacementPlanner.resolve(
            new SurfaceFaceRequest(anchor, face), capabilities.aperture(),
            minecraft.player.getYRot(), minecraft.player.getBoundingBox(),
            new SurfaceFacePlacementPlanner.Probe() {
                @Override public boolean anchorSolid(BlockPos position) {
                    return !minecraft.level.getBlockState(position)
                        .getCollisionShape(minecraft.level, position).isEmpty();
                }
                @Override public boolean blocked(PortalPlacement placement) {
                    return minecraft.level.getBlockCollisions(null,
                        placement.bounds().deflate(0.002)).iterator().hasNext();
                }
                @Override public int backingBlocks(BlockPos position) {
                    return minecraft.level.getBlockState(position)
                        .getCollisionShape(minecraft.level, position).isEmpty() ? 0 : 1;
                }
                @Override public boolean expandedSupport(PortalPlacement placement) {
                    return PortalSupportArea.hasFullExpandedSupport(minecraft.level, placement);
                }
            }, new SurfaceFacePlacementPlanner.Validation(
                minecraft.player.getEyePosition().distanceTo(faceCenter), range, true));
        CACHE.update(tick, input, result.placement());
        return true;
    }

    private static PortalPlacement frontPreview(Minecraft minecraft,
                                                PortalGunCapabilities capabilities,
                                                PortalOrientation orientation) {
        PortalGeometry geometry = capabilities.aperture() == dev.riftgun.portal.PortalAperture.EXPANDED
            ? orientation == PortalOrientation.VERTICAL
                ? PortalGeometry.FLOATING_EXPANDED : PortalGeometry.HORIZONTAL_EXPANDED
            : orientation == PortalOrientation.VERTICAL
                ? PortalGeometry.FLOATING_VERTICAL : PortalGeometry.HORIZONTAL;
        Vec3 center;
        if (orientation == PortalOrientation.VERTICAL) {
            Vec3 look = Vec3.directionFromRotation(0.0F, minecraft.player.getYRot()).normalize();
            center = minecraft.player.position().add(look.scale(
                PortalPlacementCapabilities.DEFAULT_FRONT_DISTANCE))
                .add(0.0, geometry.height() * 0.5, 0.0);
            return new PortalPlacement(center, orientation, geometry,
                minecraft.player.getYRot() + 180.0F, null, null);
        }
        center = FrontHorizontalPortalPlacement.center(
            minecraft.player.getBoundingBox(), Vec3.ZERO, orientation);
        return new PortalPlacement(center, orientation, geometry,
            minecraft.player.getYRot(), null, null);
    }

    private static ItemStack heldGun(Minecraft minecraft) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.is(RiftContent.PORTAL_GUN.get())) return mainHand;
        ItemStack offhand = minecraft.player.getOffhandItem();
        return offhand.is(RiftContent.PORTAL_GUN.get()) ? offhand : ItemStack.EMPTY;
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer vertices,
                             List<PortalPlacementPreviewGeometry.Segment> segments) {
        Matrix4f matrix = pose.pose();
        for (PortalPlacementPreviewGeometry.Segment segment : segments) {
            Vec3 direction = segment.to().subtract(segment.from()).normalize();
            vertex(vertices, pose, matrix, segment.from(), direction, COLOR);
            vertex(vertices, pose, matrix, segment.to(), direction, COLOR);
        }
    }

    private static void drawColored(PoseStack.Pose pose, VertexConsumer vertices,
                                    List<PortalPairingPreviewGeometry.ColoredSegment> segments) {
        Matrix4f matrix = pose.pose();
        for (PortalPairingPreviewGeometry.ColoredSegment colored : segments) {
            var segment = colored.geometry();
            Vec3 direction = segment.to().subtract(segment.from()).normalize();
            vertex(vertices, pose, matrix, segment.from(), direction, colored.color());
            vertex(vertices, pose, matrix, segment.to(), direction, colored.color());
        }
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, Matrix4f matrix,
                               Vec3 point, Vec3 direction, int color) {
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor(color)
            .setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z);
    }

    private PortalPlacementPreview() {}
}
