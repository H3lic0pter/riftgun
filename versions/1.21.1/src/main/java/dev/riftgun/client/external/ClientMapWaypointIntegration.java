package dev.riftgun.client.external;

import com.mojang.logging.LogUtils;
import dev.riftgun.client.compat.journeymap.JourneyMapExternalDestinationAdapter;
import dev.riftgun.client.compat.xaero.XaeroExternalDestinationAdapter;
import dev.riftgun.config.ClientConfig;
import dev.riftgun.external.ExternalDestinationSelection;
import dev.riftgun.external.ExternalDestinationSource;
import dev.riftgun.external.client.ClientExternalDestinationCoordinator;
import dev.riftgun.external.client.ClientExternalDestinationCatalog;
import dev.riftgun.external.client.ExternalDestination;
import java.util.Set;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/** Version adapter for the shared waypoint integration coordinator. */
public final class ClientMapWaypointIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ClientExternalDestinationCoordinator COORDINATOR =
        new ClientExternalDestinationCoordinator();
    private static boolean initialized;

    public static ClientExternalDestinationCatalog catalog() {
        initialize();
        return COORDINATOR.catalog();
    }

    public static boolean installed(ExternalDestinationSource source) {
        return ModList.get().isLoaded(source.modId());
    }

    public static boolean anyInstalled() {
        return installed(ExternalDestinationSource.JOURNEYMAP)
            || installed(ExternalDestinationSource.XAERO_MINIMAP);
    }

    public static void refresh(Set<String> dimensions, int limit) {
        initialize();
        for (ExternalDestinationSource source : ExternalDestinationSource.values()) {
            refreshSource(source, dimensions, limit);
        }
        COORDINATOR.markJourneyMapClean();
    }

    public static boolean refreshJourneyMapIfDirty(Set<String> dimensions, int limit) {
        if (!COORDINATOR.journeyMapDirty()) return false;
        refreshSource(ExternalDestinationSource.JOURNEYMAP, dimensions, limit);
        COORDINATOR.markJourneyMapClean();
        return true;
    }

    public static boolean journeyMapDirty() {
        return COORDINATOR.journeyMapDirty();
    }

    public static void markJourneyMapDirty() {
        COORDINATOR.markJourneyMapDirty();
    }

    public static ExternalDestinationSelection selected() {
        return COORDINATOR.selected();
    }

    public static void select(ExternalDestination destination) {
        COORDINATOR.select(destination);
    }

    public static void clearSelection() {
        COORDINATOR.clearSelection();
    }

    public static boolean reconcileSelection() {
        return COORDINATOR.reconcileSelection();
    }

    public static void clear() {
        COORDINATOR.clear();
    }

    public static boolean enabled(ExternalDestinationSource source) {
        return source == ExternalDestinationSource.JOURNEYMAP
            ? ClientConfig.VALUES.journeyMapWaypointsEnabled.get()
            : ClientConfig.VALUES.xaeroWaypointsEnabled.get();
    }

    private static void initialize() {
        if (initialized) return;
        initialized = true;
        installAdapter(ExternalDestinationSource.JOURNEYMAP);
        installAdapter(ExternalDestinationSource.XAERO_MINIMAP);
    }

    private static void installAdapter(ExternalDestinationSource source) {
        if (!installed(source)) return;
        COORDINATOR.install(source, () -> switch (source) {
            case JOURNEYMAP -> new JourneyMapExternalDestinationAdapter();
            case XAERO_MINIMAP -> new XaeroExternalDestinationAdapter();
        }, installedVersion(source), ClientMapWaypointIntegration::reportFailure);
    }

    private static void refreshSource(ExternalDestinationSource source,
                                      Set<String> dimensions, int limit) {
        COORDINATOR.refresh(source, enabled(source), installedVersion(source),
            dimensions, limit, ClientMapWaypointIntegration::reportFailure);
    }

    private static String installedVersion(ExternalDestinationSource source) {
        return ModList.get().getModContainerById(source.modId())
            .map(value -> value.getModInfo().getVersion().toString())
            .orElse("");
    }

    private static void reportFailure(ExternalDestinationSource source, Throwable exception) {
        LOGGER.warn("RiftGun disabled {} waypoint integration for version {}",
            source.displayName(), installedVersion(source), exception);
    }

    private ClientMapWaypointIntegration() {}
}
