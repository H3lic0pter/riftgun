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
import dev.riftgun.portal.PortalPlacementPreviewCache;
import dev.riftgun.portal.PortalPlacementPreviewGeometry;
import dev.riftgun.service.PortalPlacementCapabilities;
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
    private static List<PortalPlacementPreviewGeometry.Segment> pendingSegments = List.of();
    private static Object levelIdentity;

    public static void tick(Minecraft minecraft) {
        if (minecraft.level != levelIdentity) {
            levelIdentity = minecraft.level;
            CACHE.clear();
            clearPending();
        }
        tickPending(minecraft);
        long tick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        if (tickSurfaceFace(minecraft, tick)) return;
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
            || segments.isEmpty() && pendingSegments.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType renderType = RenderType.lines();
        draw(poses.last(), buffers.getBuffer(renderType), segments);
        draw(poses.last(), buffers.getBuffer(renderType), pendingSegments);
        buffers.endBatch(renderType);
        poses.popPose();
    }

    private static void tickPending(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            clearPending();
            return;
        }
        ItemStack gun = heldGun(minecraft);
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
            capabilities.configuredSurfaceRange(), capabilities.aperture(),
            minecraft.player.getXRot(), minecraft.player.getYRot());
    }

    private static boolean tickSurfaceFace(Minecraft minecraft, long tick) {
        if (!(minecraft.screen instanceof ModeRadialScreen screen)
            || !screen.surfaceFacePreviewOpen()) return false;
        ItemStack gun = heldGun(minecraft);
        if (gun.isEmpty()) {
            CACHE.clear();
            return true;
        }
        int smartDistance = PortalClientState.data().settings().smartDistance();
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            gun, smartDistance, PortalClientState.moduleRules());
        BlockPos anchor = screen.surfaceAnchor();
        var face = screen.selectedSurfaceFace();
        int range = capabilities.configuredSurfaceRange();
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
            vertex(vertices, pose, matrix, segment.from(), direction);
            vertex(vertices, pose, matrix, segment.to(), direction);
        }
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, Matrix4f matrix,
                               Vec3 point, Vec3 direction) {
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor(COLOR)
            .setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z);
    }

    private PortalPlacementPreview() {}
}
