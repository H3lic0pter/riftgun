package dev.riftgun.client;

import dev.riftgun.client.screen.ModeRadialScreen;
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
    private static boolean suppressUntilRelease;

    public static void tick(Minecraft minecraft) {
        boolean cycleDown = keyDown(ClientModEvents.CYCLE_PLACEMENT);
        boolean radialDown = keyDown(ClientModEvents.OPEN_MODE_RADIAL);
        if (minecraft.screen instanceof ModeRadialScreen screen) {
            if (pendingSource != null && !sourceDown(pendingSource, cycleDown, radialDown)) {
                screen.commitAndClose();
                suppressUntilRelease = true;
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
            if (!sourceDown(pendingSource, cycleDown, radialDown)) pendingSource = null;
            remember(cycleDown, radialDown);
            return;
        }
        if (radialDown && !radialWasDown) request(Source.DEDICATED);
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

    public static void openFromServer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (pendingSource != null && minecraft.screen == null
            && sourceDown(pendingSource, keyDown(ClientModEvents.CYCLE_PLACEMENT),
                keyDown(ClientModEvents.OPEN_MODE_RADIAL))) {
            minecraft.setScreen(new ModeRadialScreen());
        }
    }

    public static void cancelFromScreen() {
        suppressUntilRelease = true;
    }

    private static void request(Source source) {
        pendingSource = source;
        PortalNetworking.sendShortcutRequest(PortalAction.OPEN_MODE_RADIAL);
    }

    private static boolean sourceDown(Source source, boolean cycleDown, boolean radialDown) {
        return source == Source.CYCLE ? cycleDown : source == Source.DEDICATED && radialDown;
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

    private static void remember(boolean cycleDown, boolean radialDown) {
        cycleWasDown = cycleDown;
        radialWasDown = radialDown;
    }

    private static void reset(boolean cycleDown, boolean radialDown) {
        pendingSource = null;
        cycleHeldTicks = 0;
        remember(cycleDown, radialDown);
    }

    private enum Source { CYCLE, DEDICATED }

    private ModeRadialInput() {}
}
