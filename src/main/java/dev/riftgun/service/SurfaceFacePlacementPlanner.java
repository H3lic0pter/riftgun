package dev.riftgun.service;

import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Authoritative and preview-safe planner for one explicitly selected anchor face. */
public final class SurfaceFacePlacementPlanner {
    private static final double SURFACE_OFFSET = PortalPlacement.DEPTH * 0.5 + 0.002;

    public static Result resolve(SurfaceFaceSelection selection, PortalAperture aperture,
                                 float playerYaw, AABB playerBounds, Probe probe,
                                 Validation validation) {
        if (!validation.lineOfSight()) return Result.failure("message.riftgun.surface_invalid");
        if (validation.distance() > validation.maximumRange()) {
            return Result.failure("message.riftgun.surface_out_of_range");
        }
        Vec3 faceCenter = Vec3.atCenterOf(selection.anchor()).add(normal(selection.face()).scale(0.5));
        return resolveAttached(selection, aperture, playerYaw, playerBounds, probe, faceCenter);
    }

    /** Shared attached placement; callers supply the original ranking point after validating their input. */
    static Result resolveAttached(SurfaceFaceSelection selection, PortalAperture aperture,
                                  float playerYaw, AABB playerBounds, Probe probe, Vec3 rankingPoint) {
        BlockPos anchor = selection.anchor();
        Direction face = selection.face();
        if (!probe.anchorSolid(anchor)) return Result.failure("message.riftgun.surface_invalid");

        if (PortalAperturePolicy.expanded(aperture)
                && !(aperture == PortalAperture.EXPANDED_ADAPTIVE
                    && PortalSupportArea.prefersStandardAperture(anchor, face, probe::anchorSolid))) {
            PortalPlacement expanded = face.getAxis().isVertical()
                ? expandedHorizontal(selection, playerYaw, playerBounds, probe, aperture, rankingPoint)
                : expandedVertical(selection, playerBounds, probe, aperture, rankingPoint);
            if (expanded != null) return Result.success(expanded);
        }
        return standard(selection, playerYaw, playerBounds, probe);
    }

    private static Result standard(SurfaceFaceSelection selection, float playerYaw,
                                   AABB playerBounds, Probe probe) {
        BlockPos anchor = selection.anchor();
        Direction face = selection.face();
        Vec3 normal = normal(face);
        if (face.getAxis().isVertical()) {
            PortalPlacement placement = new PortalPlacement(
                Vec3.atCenterOf(anchor).add(normal.scale(0.5 + SURFACE_OFFSET)),
                face == Direction.UP ? PortalOrientation.TOP : PortalOrientation.BOTTOM,
                PortalGeometry.HORIZONTAL, playerYaw, anchor, face);
            return probe.blocked(placement)
                ? Result.failure("message.riftgun.surface_obstructed") : Result.success(placement);
        }

        double x = anchor.getX() + 0.5 + normal.x * (0.5 + SURFACE_OFFSET);
        double z = anchor.getZ() + 0.5 + normal.z * (0.5 + SURFACE_OFFSET);
        float yaw = yawFromNormal(normal);
        List<SidePortalCandidateSelector.Candidate> candidates = new ArrayList<>(2);
        PortalPlacement above = new PortalPlacement(new Vec3(x, anchor.getY() + 1.0, z),
            PortalOrientation.VERTICAL, PortalGeometry.SURFACE_VERTICAL, yaw, anchor, face);
        PortalPlacement below = new PortalPlacement(new Vec3(x, anchor.getY(), z),
            PortalOrientation.VERTICAL, PortalGeometry.SURFACE_VERTICAL, yaw, anchor, face);
        if (!probe.blocked(above)) candidates.add(new SidePortalCandidateSelector.Candidate(
            above, 1 + probe.backingBlocks(anchor.above())));
        if (!probe.blocked(below)) candidates.add(new SidePortalCandidateSelector.Candidate(
            below, 1 + probe.backingBlocks(anchor.below())));
        if (!candidates.isEmpty()) {
            return Result.success(SidePortalCandidateSelector.choose(candidates, playerBounds));
        }
        PortalPlacement compact = new PortalPlacement(
            new Vec3(x, anchor.getY() + 0.5, z), PortalOrientation.VERTICAL,
            PortalGeometry.SURFACE_COMPACT, yaw, anchor, face);
        return probe.blocked(compact)
            ? Result.failure("message.riftgun.surface_obstructed") : Result.success(compact);
    }

    private static @Nullable PortalPlacement expandedVertical(
        SurfaceFaceSelection selection, AABB playerBounds, Probe probe, PortalAperture aperture, Vec3 rankingPoint
    ) {
        BlockPos anchor = selection.anchor();
        Direction face = selection.face();
        Direction lateral = face.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
        Vec3 normal = normal(face);
        Vec3 lateralVector = normal(lateral);
        float yaw = yawFromNormal(normal);
        List<SidePortalCandidateSelector.Candidate> candidates = new ArrayList<>(4);
        for (int lateralOffset = -1; lateralOffset <= 0; lateralOffset++) {
            for (int verticalOffset = -1; verticalOffset <= 0; verticalOffset++) {
                BlockPos origin = anchor.relative(lateral, lateralOffset).offset(0, verticalOffset, 0);
                PortalPlacement placement = new PortalPlacement(Vec3.atCenterOf(origin)
                    .add(lateralVector.scale(0.5)).add(0.0, 0.5, 0.0)
                    .add(normal.scale(0.5 + SURFACE_OFFSET)), PortalOrientation.VERTICAL,
                    PortalAperturePolicy.attachedVertical(), yaw, origin, face);
                if ((!aperture.requiresFullSupport() || probe.expandedSupport(placement)) && !probe.blocked(placement)) {
                    candidates.add(new SidePortalCandidateSelector.Candidate(
                        aperture.requiresFullSupport() ? placement : PortalSupportArea.allowOverhang(placement, anchor),
                        aperture.requiresFullSupport() ? 0
                            : PortalSupportArea.expandedBackingBlocks(origin, face, probe::backingBlocks)));
                }
            }
        }
        return candidates.isEmpty() ? null : ExpandedPortalCandidateSelector.chooseAttached(
            candidates, rankingPoint, playerBounds, aperture);
    }

    private static @Nullable PortalPlacement expandedHorizontal(
        SurfaceFaceSelection selection, float playerYaw, AABB playerBounds, Probe probe,
        PortalAperture aperture, Vec3 rankingPoint
    ) {
        BlockPos anchor = selection.anchor();
        Direction face = selection.face();
        Vec3 normal = normal(face);
        PortalOrientation orientation = face == Direction.UP
            ? PortalOrientation.TOP : PortalOrientation.BOTTOM;
        List<SidePortalCandidateSelector.Candidate> candidates = new ArrayList<>(4);
        for (int xOffset = -1; xOffset <= 0; xOffset++) {
            for (int zOffset = -1; zOffset <= 0; zOffset++) {
                BlockPos origin = anchor.offset(xOffset, 0, zOffset);
                PortalPlacement placement = new PortalPlacement(Vec3.atCenterOf(origin)
                    .add(0.5, 0.0, 0.5).add(normal.scale(0.5 + SURFACE_OFFSET)),
                    orientation, PortalAperturePolicy.horizontal(), playerYaw, origin, face);
                if ((!aperture.requiresFullSupport() || probe.expandedSupport(placement)) && !probe.blocked(placement)) {
                    candidates.add(new SidePortalCandidateSelector.Candidate(
                        aperture.requiresFullSupport() ? placement : PortalSupportArea.allowOverhang(placement, anchor),
                        aperture.requiresFullSupport() ? 0
                            : PortalSupportArea.expandedBackingBlocks(origin, face, probe::backingBlocks)));
                }
            }
        }
        return candidates.isEmpty() ? null : ExpandedPortalCandidateSelector.chooseAttached(
            candidates, rankingPoint, playerBounds, aperture);
    }

    private static Vec3 normal(Direction direction) {
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private static float yawFromNormal(Vec3 normal) {
        return (float) Math.toDegrees(Math.atan2(-normal.x, normal.z));
    }

    public interface Probe {
        boolean anchorSolid(BlockPos position);
        boolean blocked(PortalPlacement placement);
        int backingBlocks(BlockPos position);
        boolean expandedSupport(PortalPlacement placement);
    }

    public record Validation(double distance, double maximumRange, boolean lineOfSight) {}

    public record Result(@Nullable PortalPlacement placement, @Nullable String errorKey) {
        public static Result success(PortalPlacement placement) { return new Result(placement, null); }
        public static Result failure(String errorKey) { return new Result(null, errorKey); }
        public boolean successful() { return placement != null; }
    }

    private SurfaceFacePlacementPlanner() {}
}
