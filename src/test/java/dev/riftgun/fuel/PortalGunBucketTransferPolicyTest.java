package dev.riftgun.fuel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class PortalGunBucketTransferPolicyTest {
    @AfterEach
    void restoreOverflowPolicy() {
        PortalGunBucketTransferPolicy.OVERFLOW_POLICY = new WholeSourceOverflowPolicy();
    }

    @Test
    void extractsBeforeTryingToInsert() {
        List<String> calls = new ArrayList<>();

        boolean transferred = PortalGunBucketTransferPolicy.extractFirst(
            amount -> {
                calls.add("extract:" + amount);
                return true;
            },
            amount -> {
                calls.add("insert:" + amount);
                return true;
            });

        assertTrue(transferred);
        assertEquals(List.of("extract:1000"), calls);
    }

    @Test
    void insertsOnlyAfterExtractionFails() {
        List<String> calls = new ArrayList<>();

        boolean transferred = PortalGunBucketTransferPolicy.extractFirst(
            amount -> {
                calls.add("extract:" + amount);
                return false;
            },
            amount -> {
                calls.add("insert:" + amount);
                return true;
            });

        assertTrue(transferred);
        assertEquals(List.of("extract:1000", "insert:1000"), calls);
    }

    @Test
    void triesAllExtractionViewsBeforeAnyInsertion() {
        List<String> calls = new ArrayList<>();

        boolean transferred = PortalGunBucketTransferPolicy.extractFirst(
            amount -> {
                calls.add("sided-extract");
                return false;
            },
            amount -> {
                calls.add("unsided-extract");
                return true;
            },
            amount -> {
                calls.add("sided-insert");
                return true;
            },
            amount -> {
                calls.add("unsided-insert");
                return true;
            });

        assertTrue(transferred);
        assertEquals(List.of("sided-extract", "unsided-extract"), calls);
    }

    @Test
    void externalFillStopsAtNominalCapacityWithoutWorldScoopOverflow() {
        assertEquals(500, PortalGunBucketTransferPolicy.acceptedExternalFill(7500, 8000, 4000));
        assertEquals(0, PortalGunBucketTransferPolicy.acceptedExternalFill(8500, 8000, 1000));
        assertEquals(1000, PortalGunBucketTransferPolicy.acceptedExternalFill(6000, 8000, 1000));
        assertFalse(PortalGunBucketTransferPolicy.extractFirst(amount -> false, amount -> false));
    }
}
