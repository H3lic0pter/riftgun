package dev.riftgun.input;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import dev.riftgun.data.PortalPlacementMode;
import net.minecraft.core.Direction;

/** Shared, render-agnostic state for the radial's surface-face preview page. */
public final class SurfaceFacePreviewState {
    public enum Frame { RELATIVE, ABSOLUTE }
    public enum Choice { UP, DOWN, FRONT, RIGHT, BACK, LEFT }

    private static final List<Choice> DEFAULT_CHOICES = List.of(
        Choice.UP, Choice.FRONT, Choice.RIGHT,
        Choice.DOWN, Choice.BACK, Choice.LEFT);
    private final List<Choice> choices;
    private final Direction relativeFront;
    private final Direction relativeRight;
    private Choice selectedChoice = Choice.FRONT;
    private Frame frame = Frame.RELATIVE;

    public SurfaceFacePreviewState() {
        this(Direction.NORTH, Direction.NORTH, List.of());
    }

    public SurfaceFacePreviewState(Direction referenceFace, Direction horizontalReference) {
        this(referenceFace, horizontalReference, List.of());
    }

    public SurfaceFacePreviewState(Direction referenceFace, Direction horizontalReference,
                                   List<String> configuredOrder) {
        choices = configuredChoices(configuredOrder);
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
        return choices;
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
            case FRONT -> frame == Frame.ABSOLUTE ? Direction.SOUTH : relativeFront;
            case RIGHT -> frame == Frame.ABSOLUTE ? Direction.EAST : relativeRight;
            case BACK -> frame == Frame.ABSOLUTE ? Direction.NORTH : relativeFront.getOpposite();
            case LEFT -> frame == Frame.ABSOLUTE ? Direction.WEST : relativeRight.getOpposite();
        };
    }

    public static List<Choice> configuredChoices(List<String> configuredOrder) {
        if (configuredOrder == null || configuredOrder.size() != Choice.values().length) {
            return DEFAULT_CHOICES;
        }
        List<Choice> parsed = new ArrayList<>(configuredOrder.size());
        EnumSet<Choice> unique = EnumSet.noneOf(Choice.class);
        for (String token : configuredOrder) {
            Choice choice = parseChoice(token);
            if (choice == null || !unique.add(choice)) return DEFAULT_CHOICES;
            parsed.add(choice);
        }
        return List.copyOf(parsed);
    }

    private static Choice parseChoice(String token) {
        if (token == null) return null;
        return switch (token.trim().toUpperCase(Locale.ROOT)) {
            case "TOP", "UP" -> Choice.UP;
            case "FRONT" -> Choice.FRONT;
            case "RIGHT" -> Choice.RIGHT;
            case "BOTTOM", "DOWN" -> Choice.DOWN;
            case "BACK" -> Choice.BACK;
            case "LEFT" -> Choice.LEFT;
            default -> null;
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
