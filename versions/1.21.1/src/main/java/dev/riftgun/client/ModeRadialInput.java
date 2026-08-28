package dev.riftgun.client;

import dev.riftgun.client.screen.ModeRadialScreen;
import dev.riftgun.input.RadialRequestState;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Owns the dual tap/hold behavior without coupling the radial screen to key bindings. */
public final class ModeRadialInput {
    private static final int HOLD_TICKS = 6;
    private static boolean cycleWasDown;
    private static boolean radialWasDown;
    private static int cycleHeldTicks;
    private static Source pendingSource;
    private static final RadialRequestState REQUEST = new RadialRequestState();
    private static boolean suppressUntilRelease;

    public static void tick(Minecraft minecraft) {
        boolean cycleDown = keyDown(ClientModEvents.CYCLE_PLACEMENT);
        boolean radialDown = keyDown(ClientModEvents.OPEN_MODE_RADIAL);

        if (minecraft.screen instanceof ModeRadialScreen screen) {
            if (pendingSource != null && !sourceDown(pendingSource, cycleDown, radialDown)) {
                if (REQUEST.release() == RadialRequestState.ReleaseResult.COMMIT) {
                    screen.commitAndClose();
                    suppressUntilRelease = true;
                }
            }
            remember(cycleDown, radialDown);
            return;
        }
        if (suppressUntilRelease) {
            if (!cycleDown && !radialDown) {
                suppressUntilRelease = false;
                pendingSource = null;
            }
            remember(cycleDown, radialDown);
            return;
        }
        if (minecraft.screen != null || minecraft.player == null || minecraft.getConnection() == null) {
            reset(cycleDown, radialDown);
            return;
        }

        if (pendingSource != null) {
            if (!sourceDown(pendingSource, cycleDown, radialDown)) REQUEST.release();
            remember(cycleDown, radialDown);
            return;
        }
        if (radialDown && !radialWasDown) {
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
        remember(cycleDown, radialDown);
    }

    public static void openFromServer(int requestId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (pendingSource == null) return;
        RadialRequestState.AcknowledgeResult result = REQUEST.acknowledge(requestId,
            sourceDown(pendingSource, keyDown(ClientModEvents.CYCLE_PLACEMENT),
                keyDown(ClientModEvents.OPEN_MODE_RADIAL)));
        if (result == RadialRequestState.AcknowledgeResult.IGNORE) return;
        if (minecraft.screen == null) {
            minecraft.setScreen(new ModeRadialScreen());
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
        suppressUntilRelease = true;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ModeRadialScreen screen) screen.rejectAndClose();
    }

    public static void cancelFromScreen() {
        pendingSource = null;
        REQUEST.cancel();
        suppressUntilRelease = true;
    }

    public static boolean ready() { return pendingSource != null && REQUEST.ready(); }

    private static void request(Source source) {
        pendingSource = source;
        int requestId = REQUEST.begin();
        PortalNetworking.sendShortcutRequest(PortalAction.OPEN_MODE_RADIAL,
            tag -> tag.putInt("RadialRequestId", requestId));
    }

    private static boolean sourceDown(Source source, boolean cycleDown, boolean radialDown) {
        return source == Source.CYCLE ? cycleDown : source == Source.DEDICATED && radialDown;
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

    private static void remember(boolean cycleDown, boolean radialDown) {
        cycleWasDown = cycleDown;
        radialWasDown = radialDown;
    }

    private static void reset(boolean cycleDown, boolean radialDown) {
        pendingSource = null;
        REQUEST.cancel();
        cycleHeldTicks = 0;
        remember(cycleDown, radialDown);
    }

    private enum Source { CYCLE, DEDICATED }

    private ModeRadialInput() {}
}
