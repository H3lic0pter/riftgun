package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.RiftGun;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.client.PortalPreviewGunState;
import dev.riftgun.client.screen.ModeRadialScreen;
import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.pairing.PortalPairingPendingEndpoint;
import dev.riftgun.pairing.PortalPairingPreviewGeometry;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacementPreviewCache;
import dev.riftgun.portal.PortalPlacementPreviewGeometry;
import dev.riftgun.service.PortalPlacementCapabilities;
import dev.riftgun.service.FrontPortalPlacementPlanner;
import dev.riftgun.service.PortalFaceExposure;
import dev.riftgun.service.RemotePortalPlacementResolver;
import dev.riftgun.service.SurfaceFacePlacementPlanner;
import dev.riftgun.service.PortalSupportArea;
import dev.riftgun.service.SurfaceFaceSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Objects;

/** Client renderer for live placement previews and lightweight pairing markers. */
@EventBusSubscriber(modid = RiftGun.MOD_ID, value = Dist.CLIENT)
public final class PortalPlacementPreview {
    private static final int COLOR = 0xD9F0F0F0;
    private static final PortalPlacementPreviewCache CACHE = new PortalPlacementPreviewCache();
    private static PortalPairingPendingEndpoint pendingEndpoint;
    private static PortalPairingPendingEndpoint entityTargetEndpoint;
    private static List<PortalPairingPreviewGeometry.ColoredSegment> pendingSegments = List.of();
    private static List<PortalPairingPreviewGeometry.ColoredSegment> entityTargetSegments = List.of();
    private static Object levelIdentity;
    private static PreviewContext previewContext = PreviewContext.NONE;

    public static void tick(Minecraft minecraft) {
        if (minecraft.level != levelIdentity) {
            levelIdentity = minecraft.level;
            CACHE.clear();
            clearPending();
            clearEntityTarget();
        }
        long tick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        PortalPreviewGunState gun = previewGun(minecraft, tick);
        PreviewContext nextContext = previewContext(minecraft, gun);
        if (nextContext != previewContext) {
            previewContext = nextContext;
            CACHE.clear();
        }
        tickPending(minecraft, gun);
        tickEntityTarget(minecraft, gun);
        if (tickPrecision(minecraft, tick, gun)) return;
        if (tickShiftRoutedPreview(minecraft, tick, gun)) return;
        PortalPlacementPreviewCache.Input input = input(minecraft, gun);
        if (input == null) {
            CACHE.clear();
            return;
        }
        tick = minecraft.level.getGameTime();
        if (!CACHE.shouldRefresh(tick, input)) return;
        updateRemotePreview(minecraft, tick, input, null);
    }

    public static PortalPlacement currentPlacement() {
        return previewContext == PreviewContext.PRECISION ? CACHE.placement() : null;
    }

    private static PreviewContext previewContext(Minecraft minecraft, PortalPreviewGunState gun) {
        if (minecraft.screen instanceof ModeRadialScreen screen
            && (screen.surfaceFacePreviewOpen() || screen.floatingOrientationPreviewOpen())) {
            return PreviewContext.PRECISION;
        }
        if (minecraft.player != null && minecraft.screen == null && gun != null
            && minecraft.player.isShiftKeyDown()
            && usesShiftRoutedPreview(gun)) {
            return PreviewContext.SHIFT_ROUTED;
        }
        return input(minecraft, gun) == null ? PreviewContext.NONE : PreviewContext.REMOTE;
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

    private static void tickPending(Minecraft minecraft, PortalPreviewGunState gun) {
        if (minecraft.level == null || minecraft.player == null) {
            clearPending();
            return;
        }
        if (gun == null || gun.placementMode() == PortalPlacementMode.ENTITY_RELOCATION) {
            clearPending();
            return;
        }
        PortalPairingPendingEndpoint next = gun.pending();
        if (next == null || !next.pairEndpoint()
            || !minecraft.level.dimension().equals(next.dimension())
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

    private static void tickEntityTarget(Minecraft minecraft, PortalPreviewGunState gun) {
        PortalPairingPendingEndpoint next = gun != null
            && gun.functionMode() == dev.riftgun.pairing.PortalFunctionMode.PORTAL_PAIRING
            && gun.placementMode() == PortalPlacementMode.ENTITY_RELOCATION
            ? gun.pending() : null;
        if (next == null || !next.entityTarget() || minecraft.level == null
            || !minecraft.level.dimension().equals(next.dimension())
            || !minecraft.level.hasChunkAt(BlockPos.containing(next.placement().center()))) {
            clearEntityTarget();
            return;
        }
        if (Objects.equals(next, entityTargetEndpoint)) return;
        entityTargetEndpoint = next;
        entityTargetSegments = PortalPairingPreviewGeometry.entityTargetSegments(next.placement());
    }

    private static void clearEntityTarget() {
        entityTargetEndpoint = null;
        entityTargetSegments = List.of();
    }

    private static PortalPlacementPreviewCache.Input input(Minecraft minecraft,
                                                            PortalPreviewGunState gun) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null
            || minecraft.isPaused()) return null;
        if (gun == null || !gun.remotePlacementPreview() || !gun.remote()
            || gun.placementMode() != PortalPlacementMode.REMOTE) return null;
        return new PortalPlacementPreviewCache.Input(
            minecraft.player.getEyePosition(), minecraft.player.getLookAngle(),
            gun.remoteDistance(), gun.aperture(),
            minecraft.player.getXRot(), minecraft.player.getYRot());
    }

    private static boolean tickPrecision(Minecraft minecraft, long tick,
                                         PortalPreviewGunState gun) {
        if (!(minecraft.screen instanceof ModeRadialScreen screen)
            || !screen.surfaceFacePreviewOpen() && !screen.floatingOrientationPreviewOpen()) return false;
        if (gun == null) {
            CACHE.clear();
            return true;
        }
        if (screen.floatingOrientationPreviewOpen()) {
            PortalOrientation orientation = screen.selectedFloatingOrientation();
            PortalPlacementMode mode = gun.placementMode();
            if (mode == PortalPlacementMode.SMART) {
                mode = gun.smartFallback() == dev.riftgun.pairing.PortalFloatingFallback.REMOTE
                    ? PortalPlacementMode.REMOTE : PortalPlacementMode.FRONT;
            }
            int range = mode == PortalPlacementMode.REMOTE ? gun.remoteDistance() : 2;
            PortalPlacementPreviewCache.Input input = new PortalPlacementPreviewCache.Input(
                minecraft.player.getEyePosition(), minecraft.player.getLookAngle(), range,
                gun.aperture(), minecraft.player.getXRot(), minecraft.player.getYRot(),
                orientation);
            if (!CACHE.shouldRefresh(tick, input)) return true;
            if (mode == PortalPlacementMode.REMOTE) {
                updateRemotePreview(minecraft, tick, input, orientation);
            } else {
                CACHE.update(tick, input, frontPreview(minecraft, gun, orientation));
            }
            return true;
        }
        BlockPos anchor = screen.surfaceAnchor();
        var face = screen.selectedSurfaceFace();
        int range = gun.maximumSurfaceRange();
        PortalPlacementPreviewCache.Input input = new PortalPlacementPreviewCache.Input(
            minecraft.player.getEyePosition(), minecraft.player.getLookAngle(), range,
            gun.aperture(), minecraft.player.getXRot(), minecraft.player.getYRot(),
            anchor, face);
        if (!CACHE.shouldRefresh(tick, input)) return true;
        Vec3 faceCenter = Vec3.atCenterOf(anchor).add(new Vec3(
            face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
        CACHE.updateSurface(tick, input, surfacePreview(minecraft, gun,
            new SurfaceFaceSelection(anchor, face),
            minecraft.player.getEyePosition().distanceTo(faceCenter), range));
        return true;
    }

    private static boolean tickShiftRoutedPreview(Minecraft minecraft, long tick,
                                                   PortalPreviewGunState gun) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null
            || gun == null || !minecraft.player.isShiftKeyDown()) return false;
        if (!usesShiftRoutedPreview(gun)) return false;
        boolean smartRemote = gun.remote() && gun.placementMode() == PortalPlacementMode.SMART
            && gun.smartFallback() == dev.riftgun.pairing.PortalFloatingFallback.REMOTE;
        int surfaceRange = smartRemote ? gun.smartDistance() : gun.maximumSurfaceRange();
        Vec3 eye = minecraft.player.getEyePosition();
        HitResult raw = minecraft.level.clip(new ClipContext(eye,
            eye.add(minecraft.player.getLookAngle().scale(gun.maximumSurfaceRange())),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
        if (raw instanceof BlockHitResult hit && raw.getType() == HitResult.Type.BLOCK
            && eye.distanceTo(hit.getLocation()) <= surfaceRange) {
            SurfaceFaceSelection selection = new SurfaceFaceSelection(
                hit.getBlockPos(), hit.getDirection());
            PortalPlacementPreviewCache.Input input = new PortalPlacementPreviewCache.Input(
                eye, minecraft.player.getLookAngle(), surfaceRange, gun.aperture(),
                minecraft.player.getXRot(), minecraft.player.getYRot(),
                hit.getBlockPos(), hit.getDirection());
            if (!CACHE.shouldRefresh(tick, input)) return true;
            PortalPlacement surface = surfacePreview(minecraft, gun, selection,
                eye.distanceTo(hit.getLocation()), gun.maximumSurfaceRange());
            if (surface != null) {
                CACHE.updateSurface(tick, input, surface);
                return true;
            }
        }
        int range = gun.remote() ? gun.remoteDistance() : gun.maximumSurfaceRange();
        PortalPlacementPreviewCache.Input input = new PortalPlacementPreviewCache.Input(
            eye, minecraft.player.getLookAngle(), range, gun.aperture(),
            minecraft.player.getXRot(), minecraft.player.getYRot());
        if (CACHE.shouldRefresh(tick, input)) updateRemotePreview(minecraft, tick, input, null);
        return true;
    }

    private static boolean usesShiftRoutedPreview(PortalPreviewGunState gun) {
        boolean pairingEntity = gun.functionMode()
            == dev.riftgun.pairing.PortalFunctionMode.PORTAL_PAIRING
            && gun.placementMode() == PortalPlacementMode.ENTITY_RELOCATION;
        boolean smartRemote = gun.remote() && gun.placementMode() == PortalPlacementMode.SMART
            && gun.smartFallback() == dev.riftgun.pairing.PortalFloatingFallback.REMOTE;
        return pairingEntity || smartRemote;
    }

    private static PortalPlacement surfacePreview(Minecraft minecraft, PortalPreviewGunState gun,
                                                   SurfaceFaceSelection selection, double distance,
                                                   int maximumRange) {
        return SurfaceFacePlacementPlanner.resolve(selection, gun.aperture(),
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
            }, new SurfaceFacePlacementPlanner.Validation(distance, maximumRange, true)).placement();
    }

    private static PortalPlacement frontPreview(Minecraft minecraft,
                                                PortalPreviewGunState gun,
                                                PortalOrientation orientation) {
        return FrontPortalPlacementPlanner.resolve(minecraft.player.position(),
            minecraft.player.getBoundingBox(), Vec3.ZERO, minecraft.player.getYRot(),
            orientation, gun.aperture(), PortalPlacementCapabilities.DEFAULT_FRONT_DISTANCE,
            minecraft.level.getMinBuildHeight(),
            PortalPlacementCapabilities.DEFAULT_MINIMUM_FLOATING_PORTAL_EXPOSURE,
            (placement, exposure) -> PortalFaceExposure.hasMinimumExposure(
            minecraft.level, placement, exposure)).placement();
    }

    private static void updateRemotePreview(Minecraft minecraft, long tick,
                                            PortalPlacementPreviewCache.Input input,
                                            PortalOrientation orientation) {
        long started = System.nanoTime();
        PortalPlacement placement = RemotePortalPlacementResolver.resolve(
            minecraft.level, minecraft.player, input.range(), input.aperture(),
            PortalPlacementCapabilities.DEFAULT_DOWNSHOT_MINIMUM_PITCH, orientation,
            PortalPlacementCapabilities.DEFAULT_MINIMUM_FLOATING_PORTAL_EXPOSURE).orElse(null);
        CACHE.updateMeasured(tick, input, placement, System.nanoTime() - started);
    }

    private static ItemStack heldGun(Minecraft minecraft) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.is(RiftContent.PORTAL_GUN.get())) return mainHand;
        ItemStack offhand = minecraft.player.getOffhandItem();
        return offhand.is(RiftContent.PORTAL_GUN.get()) ? offhand : ItemStack.EMPTY;
    }

    private static PortalPreviewGunState previewGun(Minecraft minecraft, long now) {
        if (minecraft.level == null || minecraft.player == null) return null;
        if (minecraft.screen instanceof ModeRadialScreen screen
            && (screen.surfaceFacePreviewOpen() || screen.floatingOrientationPreviewOpen())) {
            return PortalPreviewGunState.fromSnapshot(PortalClientState.gun(), PortalClientState.data(),
                minecraft.player.getUUID(), now);
        }
        return PortalPreviewGunState.fromStack(heldGun(minecraft), PortalClientState.data(),
            PortalClientState.moduleRules(), minecraft.player.getUUID(), now);
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

    private enum PreviewContext { NONE, REMOTE, PRECISION, SHIFT_ROUTED }

    private PortalPlacementPreview() {}
}
