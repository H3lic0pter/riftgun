package dev.riftgun.external.client;

import dev.riftgun.external.ExternalDestinationSource;

/** Optional-map boundary. Implementations are the only classes allowed to expose map-mod types. */
public interface ClientExternalDestinationAdapter {
    ExternalDestinationSource source();

    ExternalDestinationReadResult read(String installedVersion);
}
