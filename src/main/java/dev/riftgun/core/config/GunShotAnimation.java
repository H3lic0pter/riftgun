package dev.riftgun.core.config;

/** First-person shot presentation. Third-person swings are independent of this preference. */
public enum GunShotAnimation {
    OFF, RECOIL, SWING;

    public GunShotAnimation next() {
        return switch (this) {
            case OFF -> RECOIL;
            case RECOIL -> SWING;
            case SWING -> OFF;
        };
    }
}
