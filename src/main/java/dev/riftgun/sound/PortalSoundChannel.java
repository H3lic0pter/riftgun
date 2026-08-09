package dev.riftgun.sound;

/** Independently selectable portal sound roles exposed by the configuration screen. */
public enum PortalSoundChannel {
    SHOT("screen.riftgun.sound.shot"),
    PORTAL("screen.riftgun.sound.portal"),
    TRANSIT("screen.riftgun.sound.transit");

    private final String labelKey;

    PortalSoundChannel(String labelKey) {
        this.labelKey = labelKey;
    }

    public String labelKey() {
        return labelKey;
    }
}
