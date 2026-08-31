package dev.riftgun.client;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.navigation.DimensionalTraversalMode;
import dev.riftgun.network.PortalAction;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/** Shared request and coordinate-default workflow for dimensional navigation screens. */
public final class DimensionalNavigationWorkflow {
    public static @Nullable Command begin(
        DimensionalNavigationController controller, ExactFields fields,
        @Nullable UUID selectedDestination
    ) {
        if (controller.saving()) return null;
        if (controller.mode() == DimensionalTraversalMode.AUTOMATIC_SEARCH) {
            return new Command(PortalAction.OPEN_DIMENSIONAL_RIFT, controller.dimension(),
                null, controller.group(), true);
        }
        controller.beginSave(selectedDestination);
        return new Command(PortalAction.CREATE_DIMENSIONAL_COORDINATE, controller.dimension(),
            fields, controller.group(), false);
    }

    public static Coordinates coordinateDefaults(
        List<DimensionLabelState.DimensionInfo> dimensions, String targetDimension, double sourceScale,
        double playerX, double playerY, double playerZ, float playerYaw
    ) {
        double targetScale = sourceScale;
        for (DimensionLabelState.DimensionInfo dimension : dimensions) {
            if (dimension.id().equals(targetDimension)) {
                targetScale = dimension.coordinateScale();
                break;
            }
        }
        return new Coordinates(format(playerX * sourceScale / targetScale), format(playerY),
            format(playerZ * sourceScale / targetScale), format(playerYaw));
    }

    public record ExactFields(String name, String x, String y, String z, String yaw) {
        public ExactFields {
            name = safe(name);
            x = safe(x);
            y = safe(y);
            z = safe(z);
            yaw = safe(yaw);
        }
    }

    public record Coordinates(String x, String y, String z, String yaw) {}

    public record Command(PortalAction action, String dimension, @Nullable ExactFields fields,
                          UUID group, boolean closesScreen) {
        public void writeTo(CompoundTag tag) {
            tag.putString("Dimension", dimension);
            if (fields == null) return;
            tag.putString("Name", fields.name());
            tag.putString("X", fields.x());
            tag.putString("Y", fields.y());
            tag.putString("Z", fields.z());
            tag.putString("Yaw", fields.yaw());
            Nbt.putUUID(tag, "Group", group);
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private DimensionalNavigationWorkflow() {}
}
