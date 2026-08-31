package dev.riftgun.network;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.service.PrecisionPlacementIntent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Wire contract for one explicit precision-placement choice. */
public record PrecisionPlacementRequest(Kind kind, @Nullable SurfaceFaceRequest surface,
                                        PortalOrientation orientation,
                                        @Nullable PortalPlacement previewPlacement) {
    public PrecisionPlacementRequest {
        if (kind == null || orientation == null || kind == Kind.SURFACE && surface == null
            || kind == Kind.FLOATING && surface != null
            || previewPlacement != null && (kind != Kind.FLOATING || previewPlacement.anchored()
                || previewPlacement.orientation() != orientation
                || !finite(previewPlacement))) {
            throw PortalRequestFields.error("message.riftgun.invalid_request");
        }
    }

    public static PrecisionPlacementRequest surface(SurfaceFaceRequest request) {
        return new PrecisionPlacementRequest(
            Kind.SURFACE, request, PortalOrientation.VERTICAL, null);
    }

    public static PrecisionPlacementRequest floating(PortalOrientation orientation) {
        return new PrecisionPlacementRequest(Kind.FLOATING, null, orientation, null);
    }

    public PrecisionPlacementRequest withPreviewPlacement(@Nullable PortalPlacement placement) {
        return new PrecisionPlacementRequest(kind, surface, orientation, placement);
    }

    public PrecisionPlacementIntent toIntent() {
        PrecisionPlacementIntent intent = kind == Kind.SURFACE
            ? PrecisionPlacementIntent.surface(surface.toSelection())
            : PrecisionPlacementIntent.floating(orientation);
        return intent.withPreviewPlacement(previewPlacement);
    }

    public static PrecisionPlacementRequest fromIntent(PrecisionPlacementIntent intent) {
        PrecisionPlacementRequest request = intent.kind() == PrecisionPlacementIntent.Kind.SURFACE
            ? surface(new SurfaceFaceRequest(intent.surface().anchor(), intent.surface().face()))
            : floating(intent.orientation());
        return request.withPreviewPlacement(intent.previewPlacement());
    }

    public void writeTo(CompoundTag tag) {
        tag.putString("PrecisionKind", kind.name());
        if (kind == Kind.SURFACE) surface.writeTo(tag);
        else tag.putString("Orientation", orientation.name());
        if (previewPlacement != null) {
            CompoundTag preview = new CompoundTag();
            preview.putDouble("X", previewPlacement.center().x);
            preview.putDouble("Y", previewPlacement.center().y);
            preview.putDouble("Z", previewPlacement.center().z);
            preview.putString("Orientation", previewPlacement.orientation().name());
            preview.putString("Geometry", previewPlacement.geometry().name());
            preview.putFloat("Yaw", previewPlacement.yaw());
            tag.put("PreviewPlacement", preview);
        }
    }

    public static PrecisionPlacementRequest decode(CompoundTag tag) {
        try {
            Kind kind = Kind.valueOf(Nbt.getString(tag, "PrecisionKind"));
            if (kind == Kind.SURFACE) return surface(SurfaceFaceRequest.decode(tag));
            PortalOrientation orientation = PortalOrientation.valueOf(
                Nbt.getString(tag, "Orientation"));
            PrecisionPlacementRequest request = floating(orientation);
            if (!Nbt.contains(tag, "PreviewPlacement")) return request;
            CompoundTag preview = Nbt.getCompound(tag, "PreviewPlacement");
            PortalPlacement placement = new PortalPlacement(new Vec3(
                Nbt.getDouble(preview, "X"), Nbt.getDouble(preview, "Y"),
                Nbt.getDouble(preview, "Z")),
                PortalOrientation.valueOf(Nbt.getString(preview, "Orientation")),
                PortalGeometry.valueOf(Nbt.getString(preview, "Geometry")),
                Nbt.getFloat(preview, "Yaw"), null, null);
            return request.withPreviewPlacement(placement);
        } catch (IllegalArgumentException ignored) {
            throw PortalRequestFields.error("message.riftgun.invalid_request");
        }
    }

    public enum Kind { SURFACE, FLOATING }

    private static boolean finite(PortalPlacement placement) {
        return Double.isFinite(placement.center().x)
            && Double.isFinite(placement.center().y)
            && Double.isFinite(placement.center().z)
            && Float.isFinite(placement.yaw());
    }
}
