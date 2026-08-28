package dev.riftgun.client.screen;

import dev.riftgun.data.PortalPlacementMode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Sprite-backed icon primitives shared by the configuration screen's panes. */
final class PortalGuiIcons {
    private static final int ICON_SIZE = 16;
    // The die's dark face reads left-heavy at mathematical center.
    private static final int RANDOM_RIFT_OPTICAL_X = 1;
    // The diagonal pencil stroke reads low-right unless its padded sprite is lifted and shifted left.
    private static final int EDIT_OPTICAL_X = -3;
    private static final int EDIT_OPTICAL_Y = -5;
    // The prediction arc reads right-heavy at its previous padded position.
    private static final int PREDICTION_OPTICAL_X = -3;
    private static final int PREDICTION_OPTICAL_Y = -4;

    /** Accepts button bounds and centers the standard 16 x 16 random-rift sprite within them. */
    static void drawRandomRiftIcon(GuiGraphicsExtractor graphics, int buttonX, int buttonY,
                                   int buttonWidth, int buttonHeight, boolean enabled) {
        PortalGuiSprites.draw(graphics,
            enabled ? PortalGuiSprites.RANDOM_RIFT_ON : PortalGuiSprites.RANDOM_RIFT_OFF,
            buttonX + (buttonWidth - ICON_SIZE) / 2 + RANDOM_RIFT_OPTICAL_X,
            buttonY + (buttonHeight - ICON_SIZE) / 2);
    }

    static void drawBucketIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.BUCKET_OFF : PortalGuiSprites.BUCKET_ON, x - 3, y - 3);
    }

    static void drawDrainIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.DRAIN_OFF : PortalGuiSprites.DRAIN_ON, x - 3, y - 3);
    }

    /** x/y are the intended top-left of the visible artwork; offsets compensate PNG padding. */
    static void drawPlacementModeIcon(GuiGraphicsExtractor graphics, int x, int y, PortalPlacementMode mode) {
        Identifier sprite = switch (mode) {
            case SMART -> PortalGuiSprites.PLACEMENT_SMART;
            case FRONT -> PortalGuiSprites.PLACEMENT_FRONT;
            case REMOTE -> PortalGuiSprites.PLACEMENT_REMOTE;
            case SURFACE -> PortalGuiSprites.PLACEMENT_SURFACE;
            case ENTITY_RELOCATION -> PortalGuiSprites.PLACEMENT_ENTITY_RELOCATION;
        };
        PortalGuiSprites.draw(graphics, sprite,
            x - (mode == PortalPlacementMode.FRONT ? 4 : 3), y - 3);
    }

    static void drawPredictionIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.PREDICTION_OFF : PortalGuiSprites.PREDICTION_ON,
            x + PREDICTION_OPTICAL_X, y + PREDICTION_OPTICAL_Y);
    }

    /** Accepts button bounds and mathematically centers the 16 x 16 function sprite. */
    static void drawFunctionModeIcon(GuiGraphicsExtractor graphics, int buttonX, int buttonY,
                                     int buttonWidth, int buttonHeight, boolean pairing) {
        PortalGuiSprites.draw(graphics,
            pairing ? PortalGuiSprites.FUNCTION_PAIRING : PortalGuiSprites.FUNCTION_COORDINATE,
            buttonX + (buttonWidth - ICON_SIZE) / 2,
            buttonY + (buttonHeight - ICON_SIZE) / 2);
    }

    static void drawGunSettingsIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.CONFIGURE_GUN, x - 3, y - 2);
    }

    static void drawModuleBayIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.MODULE_BAY, x - 2, y - 2);
    }

    static void drawPortalCloseIcon(GuiGraphicsExtractor graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.PORTAL_CLOSE, x - 2, y - 2);
    }

    static void drawSmartDistanceIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.SMART_DISTANCE, x - 2, y - 2);
    }

    static void drawPortalDurationIcon(GuiGraphicsExtractor graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.PORTAL_DURATION, x - 2, y - 2);
    }

    static void drawApertureIcon(GuiGraphicsExtractor graphics, int x, int y, boolean enabled) {
        PortalGuiSprites.draw(graphics, enabled
            ? PortalGuiSprites.APERTURE_ON : PortalGuiSprites.APERTURE_OFF, x - 2, y - 2);
    }

    static void drawFallGuardIcon(GuiGraphicsExtractor graphics, int x, int y, boolean enabled) {
        PortalGuiSprites.draw(graphics, enabled
            ? PortalGuiSprites.FALL_GUARD_ON : PortalGuiSprites.FALL_GUARD_OFF, x - 2, y - 2);
    }

    static void drawSurfaceRangeIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.SURFACE_RANGE, x - 3, y - 2);
    }

    static void drawEntityAccessIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.ENTITY_ACCESS, x - 2, y - 2);
    }

    static void drawPigIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.PASSIVE_TRANSIT_OFF : PortalGuiSprites.PASSIVE_TRANSIT_ON, x - 1, y - 2);
    }

    static void drawZombieIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.HOSTILE_TRANSIT_OFF : PortalGuiSprites.HOSTILE_TRANSIT_ON, x - 2, y - 2);
    }

    static void drawDragonIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.BOSS_TRANSIT_OFF : PortalGuiSprites.BOSS_TRANSIT_ON, x - 1, y - 2);
    }

    static void drawPlayerTargetIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.PLAYER_TARGET_OFF : PortalGuiSprites.PLAYER_TARGET_ON, x - 2, y - 2);
    }

    static void drawPlayerExcludeIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.PLAYER_EXCLUDE_OFF : PortalGuiSprites.PLAYER_EXCLUDE_ON, x - 2, y - 2);
    }

    static void drawPlayerRefreshIcon(GuiGraphicsExtractor graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.PLAYER_REFRESH, x - 4, y - 4);
    }

    static void drawEntityFallGuardIcon(GuiGraphicsExtractor graphics, int x, int y, boolean enabled) {
        PortalGuiSprites.draw(graphics, enabled
            ? PortalGuiSprites.ENTITY_FALL_GUARD_ON : PortalGuiSprites.ENTITY_FALL_GUARD_OFF, x, y);
    }

    static void drawProjectileTransitIcon(GuiGraphicsExtractor graphics, int x, int y, boolean enabled) {
        PortalGuiSprites.draw(graphics, enabled
            ? PortalGuiSprites.PROJECTILE_TRANSIT_ON : PortalGuiSprites.PROJECTILE_TRANSIT_OFF, x, y);
    }

    static void drawEntityRelocationIcon(GuiGraphicsExtractor graphics, int x, int y, boolean enabled) {
        PortalGuiSprites.draw(graphics, enabled
            ? PortalGuiSprites.ENTITY_RELOCATION_ON : PortalGuiSprites.ENTITY_RELOCATION_OFF,
            x - 2, y - 2);
    }

    static void drawEntityRelocationSmartIcon(GuiGraphicsExtractor graphics, int x, int y, boolean enabled) {
        PortalGuiSprites.draw(graphics, enabled
            ? PortalGuiSprites.ENTITY_RELOCATION_SMART_ON : PortalGuiSprites.ENTITY_RELOCATION_SMART_OFF,
            x - 2, y - 2);
    }

    static void drawEntityRelocationConfigIcon(GuiGraphicsExtractor graphics, int x, int y, boolean enabled) {
        PortalGuiSprites.draw(graphics, enabled
            ? PortalGuiSprites.ENTITY_RELOCATION_ON : PortalGuiSprites.ENTITY_RELOCATION_OFF,
            x - 2, y - 2);
    }

    static void drawEyeIcon(GuiGraphicsExtractor graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.VISUALS, x - 3, y - 4);
    }

    static void drawSoundIcon(GuiGraphicsExtractor graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.SOUNDS, x - 3, y - 4);
    }

    static void drawDownIcon(GuiGraphicsExtractor graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.DROPDOWN, x - 4, y - 6);
    }

    static void drawBackIcon(GuiGraphicsExtractor graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.BACK, x - 3, y - 5);
    }

    static void drawResetIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.RESET_OFF : PortalGuiSprites.RESET_ON, x - 4, y - 4);
    }

    static void drawSwirlIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.SWIRL, x - 4, y - 4);
    }

    static void drawDisclosure(GuiGraphicsExtractor graphics, int x, int y, boolean expanded) {
        PortalGuiSprites.draw(graphics, expanded
            ? PortalGuiSprites.GROUP_EXPANDED : PortalGuiSprites.GROUP_COLLAPSED,
            x - (expanded ? 4 : 6), y - (expanded ? 6 : 4));
    }

    static void drawDragHandle(GuiGraphicsExtractor graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.DRAG_HANDLE, x - 5, y - 4);
    }

    static void drawDestinationDragDot(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.DESTINATION_DOT_OFF : PortalGuiSprites.DESTINATION_DOT_ON, x - 7, y - 7);
    }

    static void drawStar(GuiGraphicsExtractor graphics, int x, int y, boolean filled) {
        PortalGuiSprites.draw(graphics, filled ? PortalGuiSprites.STAR_ON : PortalGuiSprites.STAR_OFF,
            x - 4, y - 4);
    }

    static void drawCross(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.DELETE, x - 4, y - 4);
    }

    static void drawPencil(GuiGraphicsExtractor graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.EDIT,
            x + EDIT_OPTICAL_X, y + EDIT_OPTICAL_Y);
    }

    private PortalGuiIcons() {}
}
