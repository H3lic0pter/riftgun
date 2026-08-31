package dev.riftgun.client;

import dev.riftgun.client.screen.ModeRadialScreen;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.network.PrecisionPlacementRequest;
import dev.riftgun.network.SurfaceFaceRequest;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.service.PortalPlacementCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Minecraft-version facade for the shared radial input state machine. */
final class ModeRadialClientAccess {
    static Keys keys() {
        return new Keys(
            ClientKeyState.down(ClientModEvents.CYCLE_PLACEMENT),
            ClientKeyState.down(ClientModEvents.OPEN_MODE_RADIAL),
            ClientKeyState.down(ClientModEvents.OPEN_PRECISION_PLACEMENT),
            ClientKeyState.down(ClientModEvents.PORTAL_PAIRING_OPERATION),
            Minecraft.getInstance().hasAltDown());
    }

    static boolean radialScreenOpen() {
        return Minecraft.getInstance().screen instanceof ModeRadialScreen;
    }

    static boolean blockedOrUnavailable() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen != null || minecraft.player == null
            || minecraft.getConnection() == null;
    }

    static PrecisionPlacementRequest capturePrecisionTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
            || !PortalClientState.gun().precisionInstalled()) {
            return null;
        }
        PortalPlacementMode mode = PortalClientState.data().settings().placementMode();
        if (mode == PortalPlacementMode.REMOTE
            && !PortalClientState.gun().remoteInstalled()) {
            mode = PortalPlacementMode.FRONT;
        }
        if (mode == PortalPlacementMode.ENTITY_RELOCATION) return null;
        if (mode == PortalPlacementMode.FRONT || mode == PortalPlacementMode.REMOTE) {
            return PrecisionPlacementRequest.floating(defaultOrientation(minecraft));
        }
        int range = mode == PortalPlacementMode.SMART
            ? Math.max(1, PortalClientState.data().settings().smartDistance())
            : Math.max(1, PortalClientState.gun().maximumSurfaceRange());
        Vec3 eye = minecraft.player.getEyePosition();
        HitResult raw = minecraft.level.clip(new ClipContext(eye,
            eye.add(minecraft.player.getLookAngle().scale(range)),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
        if (raw instanceof BlockHitResult hit && raw.getType() == HitResult.Type.BLOCK) {
            return PrecisionPlacementRequest.surface(
                new SurfaceFaceRequest(hit.getBlockPos(), hit.getDirection()));
        }
        return mode == PortalPlacementMode.SMART
            ? PrecisionPlacementRequest.floating(defaultOrientation(minecraft)) : null;
    }

    static void sendOpenRequest(int requestId, boolean precisionPreview) {
        PortalNetworking.sendShortcutRequest(PortalAction.OPEN_MODE_RADIAL, tag -> {
            tag.putInt("RadialRequestId", requestId);
            tag.putBoolean("PrecisionPreview", precisionPreview);
        });
    }

    static void sendCycleRequest() {
        boolean reverse = Minecraft.getInstance().options.keyShift.isDown();
        PortalNetworking.sendShortcutRequest(PortalAction.CYCLE_PLACEMENT_MODE,
            tag -> tag.putBoolean("Reverse", reverse));
    }

    static void sendToggleFunctionRequest() {
        PortalNetworking.sendShortcutRequest(PortalAction.TOGGLE_FUNCTION_MODE);
    }

    static void commitPairingShortcut() {
        if (Minecraft.getInstance().screen instanceof ModeRadialScreen screen) {
            screen.commitPairingShortcut();
        }
    }

    static void openOrRefresh(PrecisionPlacementRequest precisionRequest) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            minecraft.setScreen(precisionRequest == null
                ? new ModeRadialScreen() : new ModeRadialScreen(precisionRequest));
        }
        if (minecraft.screen instanceof ModeRadialScreen screen) screen.refreshFromServer();
    }

    static void commitAndClose(boolean precisionPreview) {
        if (!(Minecraft.getInstance().screen instanceof ModeRadialScreen screen)) return;
        if (precisionPreview) screen.closeFromShortcutRelease();
        else screen.commitAndClose();
    }

    static void rejectAndClose() {
        if (Minecraft.getInstance().screen instanceof ModeRadialScreen screen) {
            screen.rejectAndClose();
        }
    }

    static boolean sneakDown() {
        return ClientKeyState.down(Minecraft.getInstance().options.keyShift);
    }

    private static PortalOrientation defaultOrientation(Minecraft minecraft) {
        float pitch = minecraft.player.getXRot();
        float threshold = PortalPlacementCapabilities.DEFAULT_DOWNSHOT_MINIMUM_PITCH;
        return pitch >= threshold ? PortalOrientation.TOP
            : pitch <= -threshold ? PortalOrientation.BOTTOM : PortalOrientation.VERTICAL;
    }

    record Keys(boolean cycleDown, boolean radialDown, boolean precisionDown,
                boolean pairingOperationDown, boolean altDown) {}

    private ModeRadialClientAccess() {}
}
