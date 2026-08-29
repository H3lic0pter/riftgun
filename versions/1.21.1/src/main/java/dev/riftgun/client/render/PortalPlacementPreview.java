package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.RiftGun;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.portal.PortalPlacementPreviewCache;
import dev.riftgun.portal.PortalPlacementPreviewGeometry;
import dev.riftgun.service.PortalPlacementCapabilities;
import dev.riftgun.service.RemotePortalPlacementResolver;
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

/** Client-only MVP for the REMOTE placement footprint. */
@EventBusSubscriber(modid = RiftGun.MOD_ID, value = Dist.CLIENT)
public final class PortalPlacementPreview {
    private static final int COLOR = 0xD9F0F0F0;
    private static final PortalPlacementPreviewCache CACHE = new PortalPlacementPreviewCache();
    private static Object levelIdentity;

    public static void tick(Minecraft minecraft) {
        if (minecraft.level != levelIdentity) {
            levelIdentity = minecraft.level;
            CACHE.clear();
        }
        PortalPlacementPreviewCache.Input input = input(minecraft);
        if (input == null) {
            CACHE.clear();
            return;
        }
        long tick = minecraft.level.getGameTime();
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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || segments.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        PoseStack poses = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType renderType = RenderType.lines();
        draw(poses.last(), buffers.getBuffer(renderType), segments);
        buffers.endBatch(renderType);
        poses.popPose();
    }

    private static PortalPlacementPreviewCache.Input input(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null
            || minecraft.isPaused()) return null;
        ItemStack gun = heldGun(minecraft);
        if (gun.isEmpty()) return null;
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(gun,
            PortalClientState.data().settings().smartDistance(), PortalClientState.moduleRules());
        if (!capabilities.remote()
            || capabilities.effectivePlacementMode(PortalClientState.data().settings().placementMode())
                != PortalPlacementMode.REMOTE) return null;
        return new PortalPlacementPreviewCache.Input(
            minecraft.player.getEyePosition(), minecraft.player.getLookAngle(),
            capabilities.configuredSurfaceRange(), capabilities.aperture(),
            minecraft.player.getXRot(), minecraft.player.getYRot());
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
