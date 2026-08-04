package dev.riftgun.data;

public enum DestinationSort {
    RECENT,
    NAME,
    CREATED,
    DISTANCE;

    public DestinationSort next() {
        DestinationSort[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}

