package dev.riftgun.pairing;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import java.util.UUID;
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

/** Immutable, owner- and gun-bound replacement for a dormant pairing entity. */
public record PortalPairingPendingEndpoint(
    UUID ownerId,
    UUID gunId,
    ResourceKey<Level> dimension,
    PortalPlacement placement,
    PortalPairingEndpoint endpoint,
    long startedAt,
    int durationTicks
) {
    private static final String OWNER = "Owner";
    private static final String GUN = "Gun";
    private static final String DIMENSION = "Dimension";
    private static final String ENDPOINT = "Endpoint";
    private static final String STARTED_AT = "StartedAt";
    private static final String DURATION_TICKS = "DurationTicks";
    private static final String X = "X";
    private static final String Y = "Y";
    private static final String Z = "Z";
    private static final String ORIENTATION = "Orientation";
    private static final String GEOMETRY = "Geometry";
    private static final String YAW = "Yaw";
    private static final String ANCHOR = "Anchor";
    private static final String ANCHOR_FACE = "AnchorFace";

    public PortalPairingPendingEndpoint {
        if (ownerId == null || gunId == null || dimension == null || placement == null
            || endpoint == null || endpoint == PortalPairingEndpoint.NONE
            || startedAt < 0L || durationTicks < 1) {
            throw new IllegalArgumentException("valid pending pairing marker required");
        }
    }

    public boolean pairEndpoint() {
        return endpoint == PortalPairingEndpoint.A || endpoint == PortalPairingEndpoint.B;
    }

    public boolean entityTarget() {
        return endpoint == PortalPairingEndpoint.ENTITY_TARGET;
    }

    public boolean validFor(UUID expectedOwner, UUID expectedGun, long now) {
        return ownerId.equals(expectedOwner) && gunId.equals(expectedGun) && !expired(now);
    }

    public boolean expired(long now) {
        return durationTicks != Integer.MAX_VALUE
            && now >= startedAt
            && now - startedAt >= durationTicks;
    }

    public PortalPairingPendingEndpoint restart(long now) {
        return new PortalPairingPendingEndpoint(ownerId, gunId, dimension, placement,
            endpoint, now, durationTicks);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        Nbt.putUUID(tag, OWNER, ownerId);
        Nbt.putUUID(tag, GUN, gunId);
//? if >=1.21.11 {
        /*tag.putString(DIMENSION, dimension.identifier().toString());
*///?} else {
        tag.putString(DIMENSION, dimension.location().toString());
//?}
        tag.putString(ENDPOINT, endpoint.name());
        tag.putLong(STARTED_AT, startedAt);
        tag.putInt(DURATION_TICKS, durationTicks);
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
        if (!Nbt.hasUUID(tag, OWNER) || !Nbt.hasUUID(tag, GUN)
            || !Nbt.contains(tag, STARTED_AT) || !Nbt.contains(tag, DURATION_TICKS)) return null;
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
            long startedAt = Nbt.getLong(tag, STARTED_AT);
            int durationTicks = Nbt.getInt(tag, DURATION_TICKS);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || startedAt < 0L || durationTicks < 1) return null;

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
                Nbt.getUUID(tag, OWNER), Nbt.getUUID(tag, GUN),
                ResourceKey.create(Registries.DIMENSION, dimensionId), placement, endpoint,
                startedAt, durationTicks);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
