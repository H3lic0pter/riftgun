package dev.riftgun.client;

import dev.riftgun.client.screen.ModeRadialScreen;
import dev.riftgun.input.RadialRequestState;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.network.PrecisionPlacementRequest;
import dev.riftgun.network.SurfaceFaceRequest;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.service.PortalPlacementCapabilities;
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
    private static PrecisionPlacementRequest pendingPrecisionRequest;

    public static void tick(Minecraft minecraft) {
        boolean cycleDown = keyDown(ClientModEvents.CYCLE_PLACEMENT);
        boolean radialDown = keyDown(ClientModEvents.OPEN_MODE_RADIAL);
        boolean surfacePreviewDown = keyDown(ClientModEvents.OPEN_PRECISION_PLACEMENT);

        if (minecraft.screen instanceof ModeRadialScreen screen) {
            if (pendingSource != null
                && !sourceDown(pendingSource, cycleDown, radialDown, surfacePreviewDown)) {
                if (pendingSource != Source.PRECISION_PREVIEW
                    && REQUEST.release() == RadialRequestState.ReleaseResult.COMMIT) {
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
                pendingPrecisionRequest = null;
            }
            remember(cycleDown, radialDown, surfacePreviewDown);
            return;
        }
        if (minecraft.screen != null || minecraft.player == null || minecraft.getConnection() == null) {
            reset(cycleDown, radialDown, surfacePreviewDown);
            return;
        }

        if (pendingSource != null) {
            if (!sourceDown(pendingSource, cycleDown, radialDown, surfacePreviewDown)) {
                REQUEST.release();
            }
            remember(cycleDown, radialDown, surfacePreviewDown);
            return;
        }
        if (surfacePreviewDown && !surfacePreviewWasDown) {
            pendingPrecisionRequest = capturePrecisionTarget(minecraft);
            if (pendingPrecisionRequest != null) request(Source.PRECISION_PREVIEW);
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
                keyDown(ClientModEvents.OPEN_PRECISION_PLACEMENT)));
        if (result == RadialRequestState.AcknowledgeResult.IGNORE) return;
        if (minecraft.screen == null) {
            if (pendingSource == Source.PRECISION_PREVIEW) {
                minecraft.setScreen(new ModeRadialScreen(pendingPrecisionRequest));
            } else {
                minecraft.setScreen(new ModeRadialScreen());
            }
        }
        if (minecraft.screen instanceof ModeRadialScreen screen) screen.refreshFromServer();
        if (pendingSource != Source.PRECISION_PREVIEW
            && result == RadialRequestState.AcknowledgeResult.COMMIT
            && minecraft.screen instanceof ModeRadialScreen screen) {
            screen.commitAndClose();
            suppressUntilRelease = true;
        }
    }

    public static void rejectFromServer(int requestId) {
        if (pendingSource == null || !REQUEST.reject(requestId)) return;
        pendingSource = null;
        pendingPrecisionRequest = null;
        suppressUntilRelease = true;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ModeRadialScreen screen) screen.rejectAndClose();
    }

    public static void cancelFromScreen() {
        pendingSource = null;
        pendingPrecisionRequest = null;
        REQUEST.cancel();
        suppressUntilRelease = true;
    }

    public static void confirmFromScreen() {
        pendingSource = null;
        pendingPrecisionRequest = null;
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
                tag.putBoolean("PrecisionPreview", source == Source.PRECISION_PREVIEW);
            });
    }

    private static boolean sourceDown(Source source, boolean cycleDown, boolean radialDown,
                                      boolean surfacePreviewDown) {
        return switch (source) {
            case CYCLE -> cycleDown;
            case DEDICATED -> radialDown;
            case PRECISION_PREVIEW -> surfacePreviewDown;
        };
    }

    private static boolean keyDown(KeyMapping mapping) {
        InputConstants.Key key = mapping.getKey();
        if (key.getValue() == InputConstants.UNKNOWN.getValue()) return false;
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, key.getValue());
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return mapping.isDown();
    }

    private static PrecisionPlacementRequest capturePrecisionTarget(Minecraft minecraft) {
        if (!Nbt.getBoolean(PortalClientState.gun(), "PrecisionPlacementInstalled")) return null;
        PortalPlacementMode mode = PortalClientState.data().settings().placementMode();
        if (mode == PortalPlacementMode.REMOTE
            && !Nbt.getBoolean(PortalClientState.gun(), "RemoteInstalled")) {
            mode = PortalPlacementMode.FRONT;
        }
        if (mode == PortalPlacementMode.ENTITY_RELOCATION) return null;
        if (mode == PortalPlacementMode.FRONT || mode == PortalPlacementMode.REMOTE) {
            return PrecisionPlacementRequest.floating(defaultOrientation(minecraft));
        }
        int range = mode == PortalPlacementMode.SMART
            ? Math.max(1, PortalClientState.data().settings().smartDistance())
            : Math.max(1, Nbt.getInt(PortalClientState.gun(), "MaximumSurfaceRange"));
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

    private static PortalOrientation defaultOrientation(Minecraft minecraft) {
        float pitch = minecraft.player.getXRot();
        float threshold = PortalPlacementCapabilities.DEFAULT_DOWNSHOT_MINIMUM_PITCH;
        return pitch >= threshold ? PortalOrientation.TOP
            : pitch <= -threshold ? PortalOrientation.BOTTOM : PortalOrientation.VERTICAL;
    }

    private static void remember(boolean cycleDown, boolean radialDown, boolean surfacePreviewDown) {
        cycleWasDown = cycleDown;
        radialWasDown = radialDown;
        surfacePreviewWasDown = surfacePreviewDown;
    }

    private static void reset(boolean cycleDown, boolean radialDown, boolean surfacePreviewDown) {
        pendingSource = null;
        pendingPrecisionRequest = null;
        REQUEST.cancel();
        cycleHeldTicks = 0;
        remember(cycleDown, radialDown, surfacePreviewDown);
    }

    private enum Source { CYCLE, DEDICATED, PRECISION_PREVIEW }

    private ModeRadialInput() {}
}
