package dev.riftgun.client;

import dev.riftgun.client.screen.ModeRadialScreen;
import dev.riftgun.input.RadialRequestState;
import dev.riftgun.input.SurfaceFacePreviewState;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.network.SurfaceFaceRequest;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Owns tap/hold and dedicated preview behavior without coupling the screen to key bindings. */
public final class ModeRadialInput {
    private static final int HOLD_TICKS = 6;
    private static boolean cycleWasDown;
    private static boolean radialWasDown;
    private static boolean surfacePreviewWasDown;
    private static int cycleHeldTicks;
    private static Source pendingSource;
    private static final RadialRequestState REQUEST = new RadialRequestState();
    private static boolean suppressUntilRelease;
    private static SurfaceFaceRequest pendingSurfaceRequest;

    public static void tick(Minecraft minecraft) {
        boolean cycleDown = keyDown(ClientModEvents.CYCLE_PLACEMENT);
        boolean radialDown = keyDown(ClientModEvents.OPEN_MODE_RADIAL);
        boolean surfacePreviewDown = keyDown(ClientModEvents.OPEN_SURFACE_FACE_PREVIEW);
        if (minecraft.screen instanceof ModeRadialScreen screen) {
            if (pendingSource != null
                && !sourceDown(pendingSource, cycleDown, radialDown, surfacePreviewDown)) {
                if (REQUEST.release() == RadialRequestState.ReleaseResult.COMMIT) {
                    screen.commitAndClose();
                    suppressUntilRelease = true;
                }
            }
            remember(cycleDown, radialDown, surfacePreviewDown);
            return;
        }
        if (suppressUntilRelease) {
            if (!cycleDown && !radialDown && !surfacePreviewDown) {
                suppressUntilRelease = false;
                pendingSource = null;
                pendingSurfaceRequest = null;
            }
            remember(cycleDown, radialDown, surfacePreviewDown);
            return;
        }
        if (minecraft.screen != null || minecraft.player == null || minecraft.getConnection() == null) {
            reset(cycleDown, radialDown, surfacePreviewDown);
            return;
        }
        if (pendingSource != null) {
            if (!sourceDown(pendingSource, cycleDown, radialDown, surfacePreviewDown)) REQUEST.release();
            remember(cycleDown, radialDown, surfacePreviewDown);
            return;
        }
        if (surfacePreviewDown && !surfacePreviewWasDown) {
            if (SurfaceFacePreviewState.canOpen(
                PortalClientState.data().settings().placementMode())) {
                pendingSurfaceRequest = captureSurfaceTarget(minecraft);
                if (pendingSurfaceRequest != null) request(Source.SURFACE_FACE_PREVIEW);
            }
        } else if (radialDown && !radialWasDown) {
            request(Source.DEDICATED);
        }
        if (cycleDown) {
            cycleHeldTicks = cycleWasDown ? cycleHeldTicks + 1 : 1;
            if (cycleHeldTicks == HOLD_TICKS) request(Source.CYCLE);
        } else if (cycleWasDown) {
            if (cycleHeldTicks < HOLD_TICKS) {
                boolean reverse = minecraft.options.keyShift.isDown();
                PortalNetworking.sendShortcutRequest(PortalAction.CYCLE_PLACEMENT_MODE,
                    tag -> tag.putBoolean("Reverse", reverse));
            }
            cycleHeldTicks = 0;
        }
        remember(cycleDown, radialDown, surfacePreviewDown);
    }

    public static void openFromServer(int requestId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (pendingSource == null) return;
        RadialRequestState.AcknowledgeResult result = REQUEST.acknowledge(requestId,
            sourceDown(pendingSource, keyDown(ClientModEvents.CYCLE_PLACEMENT),
                keyDown(ClientModEvents.OPEN_MODE_RADIAL),
                keyDown(ClientModEvents.OPEN_SURFACE_FACE_PREVIEW)));
        if (result == RadialRequestState.AcknowledgeResult.IGNORE) return;
        if (minecraft.screen == null) {
            if (pendingSource == Source.SURFACE_FACE_PREVIEW) {
                minecraft.setScreen(new ModeRadialScreen(pendingSurfaceRequest));
            } else {
                minecraft.setScreen(new ModeRadialScreen());
            }
        }
        if (minecraft.screen instanceof ModeRadialScreen screen) screen.refreshFromServer();
        if (result == RadialRequestState.AcknowledgeResult.COMMIT
            && minecraft.screen instanceof ModeRadialScreen screen) {
            screen.commitAndClose();
            suppressUntilRelease = true;
        }
    }

    public static void rejectFromServer(int requestId) {
        if (pendingSource == null || !REQUEST.reject(requestId)) return;
        pendingSource = null;
        pendingSurfaceRequest = null;
        suppressUntilRelease = true;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ModeRadialScreen screen) screen.rejectAndClose();
    }

    public static void cancelFromScreen() {
        pendingSource = null;
        pendingSurfaceRequest = null;
        REQUEST.cancel();
        suppressUntilRelease = true;
    }

    public static boolean ready() { return pendingSource != null && REQUEST.ready(); }

    /** Reads the configured Sneak binding directly while a Screen owns keyboard input. */
    public static boolean sneakDown() {
        return keyDown(Minecraft.getInstance().options.keyShift);
    }

    private static void request(Source source) {
        pendingSource = source;
        int requestId = REQUEST.begin();
        PortalNetworking.sendShortcutRequest(PortalAction.OPEN_MODE_RADIAL,
            tag -> {
                tag.putInt("RadialRequestId", requestId);
                tag.putBoolean("SurfaceFacePreview", source == Source.SURFACE_FACE_PREVIEW);
            });
    }

    private static boolean sourceDown(Source source, boolean cycleDown, boolean radialDown,
                                      boolean surfacePreviewDown) {
        return switch (source) {
            case CYCLE -> cycleDown;
            case DEDICATED -> radialDown;
            case SURFACE_FACE_PREVIEW -> surfacePreviewDown;
        };
    }

    private static boolean keyDown(KeyMapping mapping) {
        InputConstants.Key key = mapping.getKey();
        if (key.getValue() == InputConstants.UNKNOWN.getValue()) return false;
        var window = Minecraft.getInstance().getWindow();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, key.getValue());
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window.handle(), key.getValue()) == GLFW.GLFW_PRESS;
        }
        return mapping.isDown();
    }

    private static SurfaceFaceRequest captureSurfaceTarget(Minecraft minecraft) {
        int range = Math.max(1, Nbt.getInt(PortalClientState.gun(), "MaximumSurfaceRange"));
        Vec3 eye = minecraft.player.getEyePosition();
        HitResult raw = minecraft.level.clip(new ClipContext(eye,
            eye.add(minecraft.player.getLookAngle().scale(range)),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
        return raw instanceof BlockHitResult hit && raw.getType() == HitResult.Type.BLOCK
            ? new SurfaceFaceRequest(hit.getBlockPos(), hit.getDirection()) : null;
    }

    private static void remember(boolean cycleDown, boolean radialDown, boolean surfacePreviewDown) {
        cycleWasDown = cycleDown;
        radialWasDown = radialDown;
        surfacePreviewWasDown = surfacePreviewDown;
    }

    private static void reset(boolean cycleDown, boolean radialDown, boolean surfacePreviewDown) {
        pendingSource = null;
        pendingSurfaceRequest = null;
        REQUEST.cancel();
        cycleHeldTicks = 0;
        remember(cycleDown, radialDown, surfacePreviewDown);
    }

    private enum Source { CYCLE, DEDICATED, SURFACE_FACE_PREVIEW }

    private ModeRadialInput() {}
}
