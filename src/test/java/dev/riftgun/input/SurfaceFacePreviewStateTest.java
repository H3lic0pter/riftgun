package dev.riftgun.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import net.minecraft.core.Direction;
import dev.riftgun.data.PortalPlacementMode;
import org.junit.jupiter.api.Test;

final class SurfaceFacePreviewStateTest {
    @Test
    void exposesAllFacesAndTracksPreviewSelection() {
        SurfaceFacePreviewState state = new SurfaceFacePreviewState(
            Direction.WEST, Direction.SOUTH);

        assertEquals(6, state.choices().size());
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
        assertEquals(Direction.NORTH, state.selectedFace());
        state.select(SurfaceFacePreviewState.Choice.RIGHT);
        assertEquals(Direction.EAST, state.selectedFace());
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
