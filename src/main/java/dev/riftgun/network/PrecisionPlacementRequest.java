package dev.riftgun.network;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.portal.PortalOrientation;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/** Wire contract for one explicit precision-placement choice. */
public record PrecisionPlacementRequest(Kind kind, @Nullable SurfaceFaceRequest surface,
                                        PortalOrientation orientation) {
    public PrecisionPlacementRequest {
        if (kind == null || orientation == null || kind == Kind.SURFACE && surface == null
            || kind == Kind.FLOATING && surface != null) {
            throw PortalRequestFields.error("message.riftgun.invalid_request");
        }
    }

    public static PrecisionPlacementRequest surface(SurfaceFaceRequest request) {
        return new PrecisionPlacementRequest(Kind.SURFACE, request, PortalOrientation.VERTICAL);
    }

    public static PrecisionPlacementRequest floating(PortalOrientation orientation) {
        return new PrecisionPlacementRequest(Kind.FLOATING, null, orientation);
    }

    public void writeTo(CompoundTag tag) {
        tag.putString("PrecisionKind", kind.name());
        if (kind == Kind.SURFACE) surface.writeTo(tag);
        else tag.putString("Orientation", orientation.name());
    }

    public static PrecisionPlacementRequest decode(CompoundTag tag) {
        try {
            Kind kind = Kind.valueOf(Nbt.getString(tag, "PrecisionKind"));
            return kind == Kind.SURFACE
                ? surface(SurfaceFaceRequest.decode(tag))
                : floating(PortalOrientation.valueOf(Nbt.getString(tag, "Orientation")));
        } catch (IllegalArgumentException ignored) {
            throw PortalRequestFields.error("message.riftgun.invalid_request");
        }
    }

    public enum Kind { SURFACE, FLOATING }
}
