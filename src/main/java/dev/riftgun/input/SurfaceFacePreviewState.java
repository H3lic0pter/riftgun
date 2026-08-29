package dev.riftgun.input;

import java.util.List;
import dev.riftgun.data.PortalPlacementMode;
import net.minecraft.core.Direction;

/** Shared, render-agnostic state for the radial's surface-face preview page. */
public final class SurfaceFacePreviewState {
    public enum Frame { RELATIVE, ABSOLUTE }
    public enum Choice { UP, DOWN, FRONT, RIGHT, BACK, LEFT }

    private static final List<Choice> CHOICES = List.of(Choice.values());
    private final Direction relativeFront;
    private final Direction relativeRight;
    private Choice selectedChoice = Choice.FRONT;
    private Frame frame = Frame.RELATIVE;

    public SurfaceFacePreviewState() {
        this(Direction.NORTH, Direction.NORTH);
    }

    public SurfaceFacePreviewState(Direction referenceFace, Direction horizontalReference) {
        boolean viewingSideFace = referenceFace.getAxis().isHorizontal();
        relativeFront = viewingSideFace ? referenceFace : requireHorizontal(horizontalReference);
        // A side face's outward normal points toward the viewer, so its screen-right
        // direction has the opposite handedness from the viewer's own heading.
        relativeRight = viewingSideFace
            ? counterClockwise(relativeFront) : clockwise(relativeFront);
        selectedChoice = referenceFace == Direction.UP ? Choice.UP
            : referenceFace == Direction.DOWN ? Choice.DOWN : Choice.FRONT;
    }

    public List<Choice> choices() {
        return CHOICES;
    }

    public static boolean canOpen(PortalPlacementMode mode) {
        return mode == PortalPlacementMode.SURFACE || mode == PortalPlacementMode.SMART;
    }

    public Direction selectedFace() {
        return resolve(selectedChoice);
    }

    public Choice selectedChoice() {
        return selectedChoice;
    }

    public void select(Choice choice) {
        selectedChoice = choice;
    }

    public Frame frame() {
        return frame;
    }

    public void toggleFrame() {
        frame = frame == Frame.RELATIVE ? Frame.ABSOLUTE : Frame.RELATIVE;
    }

    public Direction resolve(Choice choice) {
        return switch (choice) {
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            case FRONT -> frame == Frame.ABSOLUTE ? Direction.NORTH : relativeFront;
            case RIGHT -> frame == Frame.ABSOLUTE ? Direction.EAST : relativeRight;
            case BACK -> (frame == Frame.ABSOLUTE ? Direction.NORTH : relativeFront).getOpposite();
            case LEFT -> frame == Frame.ABSOLUTE ? Direction.WEST : relativeRight.getOpposite();
        };
    }

    private static Direction clockwise(Direction direction) {
        return switch (requireHorizontal(direction)) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalStateException("Horizontal direction required");
        };
    }

    private static Direction counterClockwise(Direction direction) {
        return clockwise(direction).getOpposite();
    }

    private static Direction requireHorizontal(Direction direction) {
        return direction.getAxis().isHorizontal() ? direction : Direction.NORTH;
    }
}
