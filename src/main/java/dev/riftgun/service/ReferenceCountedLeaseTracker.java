package dev.riftgun.service;

import java.util.HashMap;
import java.util.Map;

/** Tracks shared ownership of an underlying resource that has no native reference count. */
final class ReferenceCountedLeaseTracker<K> {
    private final Map<K, Integer> counts = new HashMap<>();

    /** @return true when the caller must acquire the underlying resource. */
    boolean acquire(K key) {
        int count = counts.getOrDefault(key, 0) + 1;
        counts.put(key, count);
        return count == 1;
    }

    /** @return true when the caller must release the underlying resource. */
    boolean release(K key) {
        Integer count = counts.get(key);
        if (count == null) throw new IllegalStateException("Lease was not acquired: " + key);
        if (count > 1) {
            counts.put(key, count - 1);
            return false;
        }
        counts.remove(key);
        return true;
    }

    int count(K key) {
        return counts.getOrDefault(key, 0);
    }

    boolean isEmpty() {
        return counts.isEmpty();
    }
}
