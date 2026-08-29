package dev.riftgun.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import net.minecraft.core.Direction;
import dev.riftgun.data.PortalPlacementMode;
import org.junit.jupiter.api.Test;

final class SurfaceFacePreviewStateTest {
    @Test
    void exposesAllFacesAndTracksPreviewSelection() {
        SurfaceFacePreviewState state = new SurfaceFacePreviewState(
            Direction.WEST, Direction.SOUTH);

        assertEquals(List.of(
            SurfaceFacePreviewState.Choice.UP,
            SurfaceFacePreviewState.Choice.FRONT,
            SurfaceFacePreviewState.Choice.RIGHT,
            SurfaceFacePreviewState.Choice.DOWN,
            SurfaceFacePreviewState.Choice.BACK,
            SurfaceFacePreviewState.Choice.LEFT), state.choices());
        assertEquals(SurfaceFacePreviewState.Frame.RELATIVE, state.frame());
        assertEquals(Direction.WEST, state.selectedFace());
        state.select(SurfaceFacePreviewState.Choice.RIGHT);
        assertEquals(Direction.SOUTH, state.selectedFace());
        state.select(SurfaceFacePreviewState.Choice.DOWN);
        assertEquals(Direction.DOWN, state.selectedFace());
    }

    @Test
    void rightClickFrameToggleReinterpretsHorizontalChoicesAsCardinals() {
        SurfaceFacePreviewState state = new SurfaceFacePreviewState(
            Direction.WEST, Direction.SOUTH);

        state.select(SurfaceFacePreviewState.Choice.FRONT);
        state.toggleFrame();

        assertEquals(SurfaceFacePreviewState.Frame.ABSOLUTE, state.frame());
        assertEquals(Direction.SOUTH, state.selectedFace());
        state.select(SurfaceFacePreviewState.Choice.RIGHT);
        assertEquals(Direction.EAST, state.selectedFace());
        state.select(SurfaceFacePreviewState.Choice.BACK);
        assertEquals(Direction.NORTH, state.selectedFace());
        state.select(SurfaceFacePreviewState.Choice.LEFT);
        assertEquals(Direction.WEST, state.selectedFace());
    }

    @Test
    void acceptsAConfiguredPermutationAndFallsBackFromDuplicates() {
        assertEquals(List.of(
            SurfaceFacePreviewState.Choice.DOWN,
            SurfaceFacePreviewState.Choice.LEFT,
            SurfaceFacePreviewState.Choice.BACK,
            SurfaceFacePreviewState.Choice.UP,
            SurfaceFacePreviewState.Choice.RIGHT,
            SurfaceFacePreviewState.Choice.FRONT),
            SurfaceFacePreviewState.configuredChoices(List.of(
                "bottom", "left", "back", "top", "right", "front")));

        assertEquals(new SurfaceFacePreviewState().choices(),
            SurfaceFacePreviewState.configuredChoices(List.of(
                "top", "front", "right", "bottom", "back", "back")));
    }

    @Test
    void topAndBottomFacesUseCapturedPlayerHeadingForRelativeFront() {
        SurfaceFacePreviewState state = new SurfaceFacePreviewState(
            Direction.UP, Direction.SOUTH);

        assertEquals(Direction.UP, state.selectedFace());
        state.select(SurfaceFacePreviewState.Choice.FRONT);
        assertEquals(Direction.SOUTH, state.selectedFace());
        state.select(SurfaceFacePreviewState.Choice.LEFT);
        assertEquals(Direction.EAST, state.selectedFace());
    }

    @Test
    void dedicatedPreviewOnlyOpensForSurfaceCapableModes() {
        assertEquals(true, SurfaceFacePreviewState.canOpen(PortalPlacementMode.SURFACE));
        assertEquals(true, SurfaceFacePreviewState.canOpen(PortalPlacementMode.SMART));
        assertEquals(false, SurfaceFacePreviewState.canOpen(PortalPlacementMode.FRONT));
        assertEquals(false, SurfaceFacePreviewState.canOpen(PortalPlacementMode.REMOTE));
        assertEquals(false, SurfaceFacePreviewState.canOpen(PortalPlacementMode.ENTITY_RELOCATION));
    }
}
