package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.riftgun.RiftGun;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.client.PortalPreviewGunState;
import dev.riftgun.client.screen.ModeRadialScreen;
import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.pairing.PortalPairingPendingEndpoint;
import dev.riftgun.pairing.PortalPairingPreviewGeometry;
import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.portal.PortalPlacementPreviewEngine;
import dev.riftgun.portal.PortalPlacementPreviewGeometry;
import dev.riftgun.portal.PortalPreviewCoordinates;
import dev.riftgun.service.FrontPortalPlacementPlanner;
import dev.riftgun.service.PortalFaceExposure;
import dev.riftgun.service.PortalPlacementCapabilities;
import dev.riftgun.service.PortalSupportArea;
import dev.riftgun.service.RemotePortalPlacementResolver;
import dev.riftgun.service.SurfaceFacePlacementPlanner;
import dev.riftgun.service.SurfaceFaceSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;

import java.util.List;

/** 1.21.1 Minecraft adapter for the shared placement-preview engine. */
@EventBusSubscriber(modid = RiftGun.MOD_ID, value = Dist.CLIENT)
public final class PortalPlacementPreview {
    private static final int COLOR = 0xD9F0F0F0;
    private static final PortalPlacementPreviewEngine ENGINE = new PortalPlacementPreviewEngine();

    public static void tick(Minecraft minecraft) {
        ENGINE.tick(input(minecraft), new MinecraftResolver(minecraft));
    }

    public static PortalPlacement currentPlacement() {
        return ENGINE.currentPlacement();
    }

    private static PortalPlacementPreviewEngine.TickInput input(Minecraft minecraft) {
        boolean worldReady = minecraft.level != null && minecraft.player != null;
        long tick = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        PortalPreviewGunState source = previewGun(minecraft, tick);
        PortalPlacementPreviewEngine.PlayerView player = !worldReady ? null
            : new PortalPlacementPreviewEngine.PlayerView(
                minecraft.player.getEyePosition(), minecraft.player.getLookAngle(),
                minecraft.player.getXRot(), minecraft.player.getYRot());
        PortalPlacementPreviewEngine.Gun gun = source == null ? null
            : new PortalPlacementPreviewEngine.Gun(source.functionMode(), source.placementMode(),
                source.smartFallback(), source.maximumSurfaceRange(), source.smartDistance(),
                source.remoteDistance(), source.aperture(), source.remote(),
                source.remotePlacementPreview(), source.pending());
        return new PortalPlacementPreviewEngine.TickInput(minecraft.level, tick, worldReady,
            worldReady && minecraft.screen == null && !minecraft.isPaused(),
            worldReady && minecraft.player.isShiftKeyDown(), player, gun, precisionTarget(minecraft));
    }

    private static @Nullable PortalPlacementPreviewEngine.PrecisionTarget precisionTarget(
        Minecraft minecraft) {
        if (!(minecraft.screen instanceof ModeRadialScreen screen)
            || !screen.surfaceFacePreviewOpen() && !screen.floatingOrientationPreviewOpen()) {
            return null;
        }
        return screen.floatingOrientationPreviewOpen()
            ? new PortalPlacementPreviewEngine.PrecisionTarget.Floating(
                screen.selectedFloatingOrientation())
            : new PortalPlacementPreviewEngine.PrecisionTarget.Surface(
                screen.surfaceAnchor(), screen.selectedSurfaceFace());
    }

    private static ItemStack heldGun(Minecraft minecraft) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.is(RiftContent.PORTAL_GUN.get())) return mainHand;
        ItemStack offhand = minecraft.player.getOffhandItem();
        return offhand.is(RiftContent.PORTAL_GUN.get()) ? offhand : ItemStack.EMPTY;
    }

    private static @Nullable PortalPreviewGunState previewGun(Minecraft minecraft, long now) {
        if (minecraft.level == null || minecraft.player == null) return null;
        if (minecraft.screen instanceof ModeRadialScreen screen
            && (screen.surfaceFacePreviewOpen() || screen.floatingOrientationPreviewOpen())) {
            return PortalPreviewGunState.fromSnapshot(PortalClientState.gun(), PortalClientState.data(),
                minecraft.player.getUUID(), now);
        }
        return PortalPreviewGunState.fromStack(heldGun(minecraft), PortalClientState.data(),
            PortalClientState.moduleRules(), minecraft.player.getUUID(), now);
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        boolean placementPass = event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES;
        boolean pairingPass = event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL;
        if (!placementPass && !pairingPass) return;
        PortalPlacementPreviewEngine.Frame frame = ENGINE.frame();
        if (frame.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        PoseStack poses = event.getPoseStack();
        PoseStack.Pose pose = poses.last();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        if (placementPass) {
            if (!frame.segments().isEmpty()) {
                RenderType placementLines = RenderType.lines();
                drawLines(pose, buffers.getBuffer(placementLines), camera, frame.segments(), COLOR);
                buffers.endBatch(placementLines);
            }
            return;
        }

        if (!frame.pendingSegments().isEmpty() || !frame.entityTargetSegments().isEmpty()) {
            Matrix4fStack modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix().mul(event.getModelViewMatrix());
            RenderSystem.applyModelViewMatrix();
            RenderType pairingMarker = PortalRenderTypes.pairingMarker();
            try {
                VertexConsumer vertices = buffers.getBuffer(pairingMarker);
                drawColored(pose, vertices, camera, frame.pendingSegments());
                drawColored(pose, vertices, camera, frame.entityTargetSegments());
                buffers.endBatch(pairingMarker);
            } finally {
                modelView.popMatrix();
                RenderSystem.applyModelViewMatrix();
            }
        }
    }

    private static void drawLines(PoseStack.Pose pose, VertexConsumer vertices, Vec3 camera,
                                  List<PortalPlacementPreviewGeometry.Segment> segments,
                                  int color) {
        for (PortalPlacementPreviewGeometry.Segment segment : segments) {
            drawLine(pose, vertices, camera, segment, color);
        }
    }

    private static void drawLine(PoseStack.Pose pose, VertexConsumer vertices, Vec3 camera,
                                 PortalPlacementPreviewGeometry.Segment segment, int color) {
        Vec3 direction = segment.to().subtract(segment.from()).normalize();
        lineVertex(vertices, pose, camera, segment.from(), direction, color);
        lineVertex(vertices, pose, camera, segment.to(), direction, color);
    }

    private static void drawColored(
        PoseStack.Pose pose, VertexConsumer vertices, Vec3 camera,
        List<PortalPairingPreviewGeometry.ColoredSegment> segments
    ) {
        for (PortalPairingPreviewGeometry.ColoredSegment colored : segments) {
            drawLine(pose, vertices, camera, colored.geometry(), colored.color());
        }
    }

    private static void lineVertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 camera,
                                   Vec3 point, Vec3 direction, int color) {
        vertices.addVertex(pose,
                PortalPreviewCoordinates.relativeTo(camera.x, point.x),
                PortalPreviewCoordinates.relativeTo(camera.y, point.y),
                PortalPreviewCoordinates.relativeTo(camera.z, point.z))
            .setColor(color)
            .setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z);
    }

    private static final class MinecraftResolver implements PortalPlacementPreviewEngine.Resolver {
        private final Minecraft minecraft;

        private MinecraftResolver(Minecraft minecraft) {
            this.minecraft = minecraft;
        }

        @Override
        public boolean markerVisible(PortalPairingPendingEndpoint endpoint) {
            return minecraft.level != null
                && minecraft.level.dimension().equals(endpoint.dimension())
                && minecraft.level.hasChunkAt(BlockPos.containing(endpoint.placement().center()));
        }

        @Override
        public @Nullable PortalPlacementPreviewEngine.SurfaceHit surfaceHit(int maximumRange) {
            if (minecraft.level == null || minecraft.player == null) return null;
            Vec3 eye = minecraft.player.getEyePosition();
            HitResult raw = minecraft.level.clip(new ClipContext(eye,
                eye.add(minecraft.player.getLookAngle().scale(maximumRange)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
            if (!(raw instanceof BlockHitResult hit) || raw.getType() != HitResult.Type.BLOCK) {
                return null;
            }
            return new PortalPlacementPreviewEngine.SurfaceHit(
                new SurfaceFaceSelection(hit.getBlockPos(), hit.getDirection()),
                eye.distanceTo(hit.getLocation()));
        }

        @Override
        public @Nullable PortalPlacement surface(PortalPlacementPreviewEngine.Gun gun,
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
                }, new SurfaceFacePlacementPlanner.Validation(distance, maximumRange, true))
                .placement();
        }

        @Override
        public @Nullable PortalPlacement front(PortalPlacementPreviewEngine.Gun gun,
                                               PortalOrientation orientation) {
            return FrontPortalPlacementPlanner.resolve(minecraft.player.position(),
                minecraft.player.getBoundingBox(), Vec3.ZERO, minecraft.player.getYRot(),
                orientation, gun.aperture(), PortalPlacementCapabilities.DEFAULT_FRONT_DISTANCE,
                minecraft.level.getMinBuildHeight(),
                PortalPlacementCapabilities.DEFAULT_MINIMUM_FLOATING_PORTAL_EXPOSURE,
                (placement, exposure) -> PortalFaceExposure.hasMinimumExposure(
                    minecraft.level, placement, exposure)).placement();
        }

        @Override
        public @Nullable PortalPlacement remote(int range, PortalAperture aperture,
                                                @Nullable PortalOrientation orientation) {
            return RemotePortalPlacementResolver.resolve(minecraft.level, minecraft.player,
                range, aperture, PortalPlacementCapabilities.DEFAULT_DOWNSHOT_MINIMUM_PITCH,
                orientation, PortalPlacementCapabilities.DEFAULT_MINIMUM_FLOATING_PORTAL_EXPOSURE)
                .orElse(null);
        }
    }

    private PortalPlacementPreview() {}
}
