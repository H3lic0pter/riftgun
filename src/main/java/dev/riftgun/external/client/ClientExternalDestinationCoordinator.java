package dev.riftgun.external.client;

import dev.riftgun.external.ExternalDestinationSelection;
import dev.riftgun.external.ExternalDestinationSource;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Version-neutral state and failure handling for optional waypoint integrations. */
public final class ClientExternalDestinationCoordinator {
    private final ClientExternalDestinationCatalog catalog = new ClientExternalDestinationCatalog();
    private final Map<ExternalDestinationSource, ClientExternalDestinationAdapter> adapters =
        new EnumMap<>(ExternalDestinationSource.class);
    private final Set<ExternalDestinationSource> warned =
        EnumSet.noneOf(ExternalDestinationSource.class);
    private ExternalDestinationSelection selected;
    private boolean journeyMapDirty;

    public ClientExternalDestinationCatalog catalog() {
        return catalog;
    }

    public void install(ExternalDestinationSource source,
                        Supplier<ClientExternalDestinationAdapter> factory,
                        String installedVersion,
                        BiConsumer<ExternalDestinationSource, Throwable> failureReporter) {
        try {
            adapters.put(source, factory.get());
        } catch (LinkageError | RuntimeException exception) {
            disable(source, installedVersion, exception, failureReporter);
        }
    }

    public void refresh(ExternalDestinationSource source, boolean enabled,
                        String installedVersion, Set<String> dimensions, int limit,
                        BiConsumer<ExternalDestinationSource, Throwable> failureReporter) {
        ClientExternalDestinationAdapter adapter = adapters.get(source);
        if (adapter == null) return;
        if (!enabled) {
            catalog.clear(source);
            if (selected != null && selected.source() == source) selected = null;
            return;
        }
        try {
            catalog.replace(adapter.read(installedVersion), dimensions, limit);
        } catch (LinkageError | RuntimeException exception) {
            disable(source, installedVersion, exception, failureReporter);
        }
    }

    public ExternalDestinationSelection selected() {
        return selected;
    }

    public void select(ExternalDestination destination) {
        selected = new ExternalDestinationSelection(
            destination.source(), destination.stableId(), destination.name(),
            destination.dimensionId(), destination.x(), destination.y(), destination.z());
    }

    public void clearSelection() {
        selected = null;
    }

    public boolean reconcileSelection() {
        if (selected == null) return false;
        boolean present = catalog.destinations(selected.source()).stream()
            .anyMatch(value -> value.stableId().equals(selected.stableId()) && value.selectable());
        if (present) return false;
        selected = null;
        return true;
    }

    public void clear() {
        catalog.clear();
        selected = null;
    }

    public boolean journeyMapDirty() {
        return journeyMapDirty;
    }

    public void markJourneyMapDirty() {
        journeyMapDirty = true;
    }

    public void markJourneyMapClean() {
        journeyMapDirty = false;
    }

    private void disable(ExternalDestinationSource source, String installedVersion,
                         Throwable exception,
                         BiConsumer<ExternalDestinationSource, Throwable> failureReporter) {
        adapters.remove(source);
        catalog.replace(new ExternalDestinationReadResult(source,
            ExternalDestinationReadResult.Status.INCOMPATIBLE, installedVersion,
            exception.getClass().getSimpleName(), List.of()), Set.of(), 1);
        if (warned.add(source)) failureReporter.accept(source, exception);
    }
}
