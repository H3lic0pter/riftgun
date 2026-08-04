package dev.riftgun.service;

public record SafetyReport(int flags) {
    public static final int COLLISION = 1;
    public static final int NO_SUPPORT = 1 << 1;
    public static final int HAZARD = 1 << 2;
    public static final SafetyReport SAFE = new SafetyReport(0);

    public boolean safe() {
        return flags == 0;
    }

    public boolean collision() {
        return (flags & COLLISION) != 0;
    }

    public boolean noSupport() {
        return (flags & NO_SUPPORT) != 0;
    }

    public boolean hazard() {
        return (flags & HAZARD) != 0;
    }
}

