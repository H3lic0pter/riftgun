package dev.riftgun.pairing;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Immutable, gun-owned replacement for an unpaired A/B portal entity. */
public record PortalPairingPendingEndpoint(
    ResourceKey<Level> dimension,
    PortalPlacement placement,
    PortalPairingEndpoint endpoint
) {
    private static final String DIMENSION = "Dimension";
    private static final String ENDPOINT = "Endpoint";
    private static final String X = "X";
    private static final String Y = "Y";
    private static final String Z = "Z";
    private static final String ORIENTATION = "Orientation";
    private static final String GEOMETRY = "Geometry";
    private static final String YAW = "Yaw";
    private static final String ANCHOR = "Anchor";
    private static final String ANCHOR_FACE = "AnchorFace";

    public PortalPairingPendingEndpoint {
        if (dimension == null || placement == null
            || endpoint != PortalPairingEndpoint.A && endpoint != PortalPairingEndpoint.B) {
            throw new IllegalArgumentException("pending A/B endpoint required");
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
//? if >=1.21.11 {
        /*tag.putString(DIMENSION, dimension.identifier().toString());
*///?} else {
        tag.putString(DIMENSION, dimension.location().toString());
//?}
        tag.putString(ENDPOINT, endpoint.name());
        tag.putDouble(X, placement.center().x);
        tag.putDouble(Y, placement.center().y);
        tag.putDouble(Z, placement.center().z);
        tag.putString(ORIENTATION, placement.orientation().name());
        tag.putString(GEOMETRY, placement.geometry().name());
        tag.putFloat(YAW, placement.yaw());
        if (placement.anchored()) {
            tag.putLong(ANCHOR, placement.anchor().asLong());
            tag.putString(ANCHOR_FACE, placement.anchorFace().name());
        }
        return tag;
    }

    public static @Nullable PortalPairingPendingEndpoint load(CompoundTag tag) {
//? if >=1.21.11 {
        /*Identifier dimensionId = Identifier.tryParse(Nbt.getString(tag, DIMENSION));
*///?} else {
        ResourceLocation dimensionId = ResourceLocation.tryParse(Nbt.getString(tag, DIMENSION));
//?}
        if (dimensionId == null) return null;
        try {
            PortalPairingEndpoint endpoint = PortalPairingEndpoint.valueOf(Nbt.getString(tag, ENDPOINT));
            PortalOrientation orientation = PortalOrientation.valueOf(Nbt.getString(tag, ORIENTATION));
            PortalGeometry geometry = PortalGeometry.valueOf(Nbt.getString(tag, GEOMETRY));
            double x = Nbt.getDouble(tag, X);
            double y = Nbt.getDouble(tag, Y);
            double z = Nbt.getDouble(tag, Z);
            float yaw = Nbt.getFloat(tag, YAW);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw)) return null;

            BlockPos anchor = null;
            Direction face = null;
            if (Nbt.contains(tag, ANCHOR) || Nbt.contains(tag, ANCHOR_FACE)) {
                if (!Nbt.contains(tag, ANCHOR) || !Nbt.contains(tag, ANCHOR_FACE)) return null;
                anchor = BlockPos.of(Nbt.getLong(tag, ANCHOR));
                face = Direction.valueOf(Nbt.getString(tag, ANCHOR_FACE));
            }
            PortalPlacement placement = new PortalPlacement(
                new Vec3(x, y, z), orientation, geometry, yaw, anchor, face);
            return new PortalPairingPendingEndpoint(
                ResourceKey.create(Registries.DIMENSION, dimensionId), placement, endpoint);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
