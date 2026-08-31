package dev.riftgun.client;

import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.navigation.DimensionalTraversalMode;
import dev.riftgun.state.PortalGunViewState;
import java.util.List;
import java.util.UUID;

/** Shared state machine for the two Minecraft-version navigation screen adapters. */
public final class DimensionalNavigationController {
    private final UUID group;
    private String dimension;
    private DimensionalTraversalMode mode;
    private boolean coordinatesEdited;
    private boolean coordinateDefaultsInitialized;
    private boolean dropdownOpen;
    private int dropdownIndex;
    private int dropdownScroll;
    private boolean saving;
    private UUID selectedBeforeSave;

    public DimensionalNavigationController(PortalGunViewState gun, UUID group) {
        this.group = group == null ? PortalPlayerData.DEFAULT_GROUP_ID : group;
        refresh(gun);
    }

    public void refresh(PortalGunViewState gun) {
        dimension = gun.navigation().targetDimension();
        mode = gun.navigation().mode();
    }

    public void ensureKnownDimension(List<DimensionLabelState.DimensionInfo> dimensions,
                                     String fallback) {
        if (indexOf(dimensions, dimension) < 0) dimension = fallback;
    }

    public boolean selectDimension(List<DimensionLabelState.DimensionInfo> dimensions, String id) {
        if (indexOf(dimensions, id) < 0) return false;
        dimension = id;
        dropdownOpen = false;
        return true;
    }

    public boolean selectMode(DimensionalTraversalMode selected, boolean automaticEnabled) {
        if (selected == DimensionalTraversalMode.AUTOMATIC_SEARCH && !automaticEnabled) return false;
        mode = selected;
        return true;
    }

    public void openDropdown(List<DimensionLabelState.DimensionInfo> dimensions) {
        dropdownOpen = true;
        dropdownIndex = Math.max(0, indexOf(dimensions, dimension));
        dropdownScroll = clamp(dropdownIndex - 3, 0, Math.max(0, dimensions.size() - 7));
    }

    public void closeDropdown() {
        dropdownOpen = false;
    }

    public void scrollDropdown(int direction, int dimensionCount) {
        int visible = Math.min(7, dimensionCount);
        dropdownScroll = clamp(dropdownScroll + direction,
            0, Math.max(0, dimensionCount - visible));
    }

    public void moveDropdownSelection(int delta, int dimensionCount) {
        if (dimensionCount <= 0) return;
        dropdownIndex = clamp(dropdownIndex + delta, 0, dimensionCount - 1);
        if (dropdownIndex < dropdownScroll) dropdownScroll = dropdownIndex;
        if (dropdownIndex >= dropdownScroll + 7) dropdownScroll = dropdownIndex - 6;
    }

    public String shiftedDimension(List<DimensionLabelState.DimensionInfo> dimensions, int delta) {
        if (dimensions.isEmpty()) return dimension;
        int current = Math.max(0, indexOf(dimensions, dimension));
        return dimensions.get(Math.floorMod(current + delta, dimensions.size())).id();
    }

    public void beginSave(UUID selectedDestination) {
        saving = true;
        selectedBeforeSave = selectedDestination;
    }

    public SaveOutcome acceptSnapshot(UUID selectedDestination) {
        if (!saving) return SaveOutcome.NOT_SAVING;
        if (selectedDestination != null && !selectedDestination.equals(selectedBeforeSave)) {
            saving = false;
            return SaveOutcome.SAVED;
        }
        saving = false;
        return SaveOutcome.REJECTED;
    }

    public UUID group() { return group; }
    public String dimension() { return dimension; }
    public DimensionalTraversalMode mode() { return mode; }
    public boolean coordinatesEdited() { return coordinatesEdited; }
    public void coordinatesEdited(boolean value) { coordinatesEdited = value; }
    public boolean coordinateDefaultsInitialized() { return coordinateDefaultsInitialized; }
    public void coordinateDefaultsInitialized(boolean value) { coordinateDefaultsInitialized = value; }
    public boolean dropdownOpen() { return dropdownOpen; }
    public int dropdownIndex() { return dropdownIndex; }
    public int dropdownScroll() { return dropdownScroll; }
    public void dropdownScroll(int value) { dropdownScroll = Math.max(0, value); }
    public boolean saving() { return saving; }

    public enum SaveOutcome { NOT_SAVING, SAVED, REJECTED }

    private static int indexOf(List<DimensionLabelState.DimensionInfo> dimensions, String id) {
        for (int index = 0; index < dimensions.size(); index++) {
            if (dimensions.get(index).id().equals(id)) return index;
        }
        return -1;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
