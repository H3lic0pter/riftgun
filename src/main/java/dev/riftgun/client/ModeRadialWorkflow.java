package dev.riftgun.client;

import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PrecisionPlacementRequest;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.state.PortalGunViewState;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/** Shared command construction for both version-specific radial screen adapters. */
public final class ModeRadialWorkflow {
    public static @Nullable Command precision(
        ModeRadialController controller, @Nullable PortalPlacement preview,
        boolean pairingShortcut, boolean endpointA
    ) {
        if (controller.cancelled() || !controller.precisionPreviewOnly()) return null;
        PrecisionPlacementRequest request = PrecisionPlacementRequest.fromIntent(
            controller.selectedPrecisionIntent(preview, pairingShortcut));
        return new PrecisionCommand(request, endpointA, pairingShortcut);
    }

    public static @Nullable Command radial(
        ModeRadialController controller, PortalGunViewState gun
    ) {
        if (controller.cancelled()) return null;
        return new RadialCommand(controller.functionMode(), controller.selectedRadialMode(gun));
    }

    public static PortalAction remoteDistanceAction() {
        return PortalAction.SET_GUN_MODULE_SETTINGS;
    }

    public static void writeRemoteDistance(CompoundTag tag, int value) {
        tag.putString("Setting", "RemoteDistance");
        tag.putInt("Value", value);
    }

    public sealed interface Command permits PrecisionCommand, RadialCommand {
        PortalAction action();
        void writeTo(CompoundTag tag);
    }

    public record PrecisionCommand(PrecisionPlacementRequest precision, boolean endpointA,
                                   boolean pairingShortcut) implements Command {
        @Override
        public PortalAction action() {
            return PortalAction.OPEN_SELECTED_PRECISION;
        }

        @Override
        public void writeTo(CompoundTag tag) {
            precision.writeTo(tag);
            tag.putBoolean("EndpointA", endpointA);
            tag.putBoolean("PairingShortcut", pairingShortcut);
        }
    }

    public record RadialCommand(PortalFunctionMode functionMode,
                                @Nullable ModeRadialController.RadialSelection radial)
        implements Command {
        @Override
        public PortalAction action() {
            return PortalAction.SET_RADIAL_MODE;
        }

        @Override
        public void writeTo(CompoundTag tag) {
            tag.putString("FunctionMode", functionMode.name());
            if (radial != null) {
                tag.putString("Page", radial.page().name());
                tag.putString("Mode", radial.mode().name());
            }
        }
    }

    private ModeRadialWorkflow() {}
}
