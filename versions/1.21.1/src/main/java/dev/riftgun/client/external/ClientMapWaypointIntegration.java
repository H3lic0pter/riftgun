package dev.riftgun.client.external;

import com.mojang.logging.LogUtils;
import dev.riftgun.client.compat.journeymap.JourneyMapExternalDestinationAdapter;
import dev.riftgun.client.compat.xaero.XaeroExternalDestinationAdapter;
import dev.riftgun.config.ClientConfig;
import dev.riftgun.external.ExternalDestinationSelection;
import dev.riftgun.external.ExternalDestinationSource;
import dev.riftgun.external.client.ClientExternalDestinationAdapter;
import dev.riftgun.external.client.ClientExternalDestinationCatalog;
import dev.riftgun.external.client.ExternalDestination;
import dev.riftgun.external.client.ExternalDestinationReadResult;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

public final class ClientMapWaypointIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ClientExternalDestinationCatalog CATALOG = new ClientExternalDestinationCatalog();
    private static final Map<ExternalDestinationSource, ClientExternalDestinationAdapter> ADAPTERS =
        new EnumMap<>(ExternalDestinationSource.class);
    private static final Set<ExternalDestinationSource> WARNED = EnumSet.noneOf(ExternalDestinationSource.class);
    private static final Set<ExternalDestinationSource> EXPANDED = EnumSet.allOf(ExternalDestinationSource.class);
    private static boolean initialized;
    private static ExternalDestinationSelection selected;
    private static boolean journeyMapDirty;

    public static ClientExternalDestinationCatalog catalog() { initialize(); return CATALOG; }
    public static boolean installed(ExternalDestinationSource source) { return ModList.get().isLoaded(source.modId()); }
    public static boolean anyInstalled() { return installed(ExternalDestinationSource.JOURNEYMAP) || installed(ExternalDestinationSource.XAERO_MINIMAP); }
    public static void refresh(Set<String> dimensions, int limit) {
        initialize();
        for (ExternalDestinationSource source : ExternalDestinationSource.values()) refreshSource(source, dimensions, limit);
        journeyMapDirty = false;
    }
    public static void refreshJourneyMapIfDirty(Set<String> dimensions, int limit) {
        if (!journeyMapDirty) return;
        refreshSource(ExternalDestinationSource.JOURNEYMAP, dimensions, limit);
        journeyMapDirty = false;
    }
    public static void markJourneyMapDirty() { journeyMapDirty = true; }
    public static ExternalDestinationSelection selected() { return selected; }
    public static void select(ExternalDestination destination) {
        selected = new ExternalDestinationSelection(destination.source(), destination.stableId(), destination.name(),
            destination.dimensionId(), destination.x(), destination.y(), destination.z());
    }
    public static void clearSelection() { selected = null; }
    public static boolean reconcileSelection() {
        if (selected == null) return false;
        boolean present = CATALOG.destinations(selected.source()).stream()
            .anyMatch(value -> value.stableId().equals(selected.stableId()) && value.selectable());
        if (present) return false;
        selected = null;
        return true;
    }
    public static void clear() { CATALOG.clear(); selected = null; }
    public static boolean enabled(ExternalDestinationSource source) {
        return source == ExternalDestinationSource.JOURNEYMAP
            ? ClientConfig.VALUES.journeyMapWaypointsEnabled.get()
            : ClientConfig.VALUES.xaeroWaypointsEnabled.get();
    }
    public static boolean expanded(ExternalDestinationSource source) { return EXPANDED.contains(source); }
    public static void expanded(ExternalDestinationSource source, boolean value) {
        if (value) EXPANDED.add(source); else EXPANDED.remove(source);
    }
    private static void initialize() {
        if (initialized) return;
        initialized = true;
        if (installed(ExternalDestinationSource.JOURNEYMAP)) ADAPTERS.put(ExternalDestinationSource.JOURNEYMAP, new JourneyMapExternalDestinationAdapter());
        if (installed(ExternalDestinationSource.XAERO_MINIMAP)) ADAPTERS.put(ExternalDestinationSource.XAERO_MINIMAP, new XaeroExternalDestinationAdapter());
    }
    private static String installedVersion(ExternalDestinationSource source) {
        return ModList.get().getModContainerById(source.modId()).map(value -> value.getModInfo().getVersion().toString()).orElse("");
    }
    private static void refreshSource(ExternalDestinationSource source, Set<String> dimensions, int limit) {
        ClientExternalDestinationAdapter adapter = ADAPTERS.get(source);
        if (adapter == null) return;
        if (!enabled(source)) { CATALOG.clear(source); if (selected != null && selected.source() == source) selected = null; return; }
        ExternalDestinationReadResult result;
        try { result = adapter.read(installedVersion(source)); }
        catch (LinkageError | RuntimeException exception) {
            result = new ExternalDestinationReadResult(source, ExternalDestinationReadResult.Status.INCOMPATIBLE,
                installedVersion(source), exception.getClass().getSimpleName(), List.of());
            if (WARNED.add(source)) LOGGER.warn("RiftGun disabled {} waypoint integration for version {}", source.displayName(), installedVersion(source), exception);
        }
        CATALOG.replace(result, dimensions, limit);
    }
    private ClientMapWaypointIntegration() {}
}
