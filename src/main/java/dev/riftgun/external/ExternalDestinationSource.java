package dev.riftgun.external;

public enum ExternalDestinationSource {
    JOURNEYMAP("journeymap", "JourneyMap"),
    XAERO_MINIMAP("xaerominimap", "Xaero's Minimap");

    private final String modId;
    private final String displayName;

    ExternalDestinationSource(String modId, String displayName) {
        this.modId = modId;
        this.displayName = displayName;
    }

    public String modId() {
        return modId;
    }

    public String displayName() {
        return displayName;
    }
}
