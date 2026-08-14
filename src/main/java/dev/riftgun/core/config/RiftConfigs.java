package dev.riftgun.core.config;

/** Common access point installed and refreshed by the active loader adapter. */
public final class RiftConfigs {
    private static final ConfigStore<RiftConfig> SERVER =
        new ConfigStore<>(RiftConfig.defaults());
    private static final ConfigStore<ClientVisualConfig> CLIENT =
        new ConfigStore<>(ClientVisualConfig.defaults());

    public static RiftConfig server() {
        return SERVER.current();
    }

    public static void publishServer(RiftConfig snapshot) {
        SERVER.publish(snapshot);
    }

    public static ClientVisualConfig client() {
        return CLIENT.current();
    }

    public static void publishClient(ClientVisualConfig snapshot) {
        CLIENT.publish(snapshot);
    }

    private RiftConfigs() {}
}
