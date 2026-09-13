package dev.riftgun.core.config;

/** Client-local shot tuning. Durations are in milliseconds; displacement uses hand-render units. */
public record GunRecoilConfig(
    int kickMillis,
    int recoveryMillis,
    double shotStrength,
    double maxBackwardOffset,
    double maxPitchDegrees,
    int useEquipRecoveryMillis
) {
    public static GunRecoilConfig defaults() {
        return new GunRecoilConfig(35, 400, 0.72, 0.15, 15.0, 200);
    }
}
