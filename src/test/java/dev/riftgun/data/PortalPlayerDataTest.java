package dev.riftgun.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

final class PortalPlayerDataTest {
    @Test
    void defaultNamesRemainMonotonicAcrossPersistence() {
        PortalPlayerData data = new PortalPlayerData();
        assertEquals("Location1", data.nextLocationName());
        assertEquals("Location2", data.nextLocationName());

        PortalPlayerData restored = PortalPlayerData.load(data.save());
        assertEquals("Location3", restored.nextLocationName());
        assertTrue(restored.expandedGroups().contains(PortalPlayerData.DEFAULT_GROUP_ID));
    }

    @Test
    void repairsUnknownGroupsWithoutChangingDestinationIdentity() {
        PortalPlayerData data = new PortalPlayerData();
        UUID id = UUID.randomUUID();
        data.destinations().add(new Destination(id, "Home", UUID.randomUUID(), Level.OVERWORLD,
            1.25, 64.0, -8.5, 90.0F, 10L, 0L, true));
        data.selectedDestinationId(id);

        PortalPlayerData restored = PortalPlayerData.load(data.save());
        Destination destination = restored.destination(id).orElseThrow();
        assertEquals(PortalPlayerData.DEFAULT_GROUP_ID, destination.groupId());
        assertEquals(id, restored.selectedDestinationId());
        assertEquals(1.25, destination.x());
    }

    @Test
    void settingsRoundTripAndOldDataKeepsConfirmationDefaults() {
        assertEquals(PortalPredictionMode.OFF, PortalPlayerSettings.defaults().predictionMode());
        PortalPlayerSettings configured = new PortalPlayerSettings(false, false, false, false, true, false,
            DestinationSort.NAME, PortalPlacementMode.SURFACE, 14, PortalPredictionMode.PROJECTION);
        PortalPlayerSettings restored = PortalPlayerSettings.load(configured.save());
        assertFalse(restored.safetyCheckEnabled());
        assertFalse(restored.confirmDeletion());
        assertFalse(restored.confirmDiscardedChanges());
        assertFalse(restored.confirmClearFluid());
        assertFalse(restored.soundsEnabled());
        assertEquals(DestinationSort.NAME, restored.sort());
        assertEquals(PortalPlacementMode.SURFACE, restored.placementMode());
        assertEquals(14, restored.smartDistance());
        assertEquals(PortalPredictionMode.PROJECTION, restored.predictionMode());

        CompoundTag legacy = new CompoundTag();
        legacy.putBoolean("SafetyCheck", false);
        PortalPlayerSettings migrated = PortalPlayerSettings.load(legacy);
        assertTrue(migrated.confirmDeletion());
        assertTrue(migrated.confirmDiscardedChanges());
        assertTrue(migrated.confirmClearFluid());
        assertEquals(PortalPlacementMode.SMART, migrated.placementMode());
        assertEquals(PortalPlayerSettings.DEFAULT_SMART_DISTANCE, migrated.smartDistance());
        assertEquals(PortalPredictionMode.OFF, migrated.predictionMode());
    }

    @Test
    void configuredPrivacyDefaultOnlyAppliesWhenNoValueWasSaved() {
        PortalPlayerData fresh = PortalPlayerData.load(new CompoundTag(), TargetPrivacy.REQUEST);
        assertEquals(TargetPrivacy.REQUEST, fresh.targetPrivacy());

        fresh.targetPrivacy(TargetPrivacy.PRIVATE);
        PortalPlayerData restored = PortalPlayerData.load(fresh.save(), TargetPrivacy.PUBLIC);
        assertEquals(TargetPrivacy.PRIVATE, restored.targetPrivacy());
    }

    @Test
    void lastOpenSafetyResultPersistsAndCanBeClearedPerDestination() {
        PortalPlayerData data = new PortalPlayerData();
        UUID unsafe = UUID.randomUUID();
        UUID safe = UUID.randomUUID();
        data.destinations().add(new Destination(unsafe, "Unsafe", PortalPlayerData.DEFAULT_GROUP_ID,
            Level.OVERWORLD, 1, 64, 1, 0, 0, 0, false));
        data.destinations().add(new Destination(safe, "Safe", PortalPlayerData.DEFAULT_GROUP_ID,
            Level.OVERWORLD, 2, 64, 2, 0, 0, 0, false));
        data.recordSafetyResult(unsafe, false);
        data.recordSafetyResult(safe, true);

        PortalPlayerData restored = PortalPlayerData.load(data.save());
        assertEquals(DestinationSafetyResult.UNSAFE, restored.safetyResult(unsafe));
        assertEquals(DestinationSafetyResult.SAFE, restored.safetyResult(safe));

        restored.clearSafetyResult(unsafe);
        assertEquals(DestinationSafetyResult.UNKNOWN, restored.safetyResult(unsafe));
        assertEquals(DestinationSafetyResult.SAFE, restored.safetyResult(safe));
    }

    @Test
    void movingDestinationClearsSafetyButPresentationChangesKeepIt() {
        PortalPlayerData data = new PortalPlayerData();
        UUID id = UUID.randomUUID();
        Destination original = new Destination(id, "Original", PortalPlayerData.DEFAULT_GROUP_ID,
            Level.OVERWORLD, 1, 64, 1, 0, 0, 0, false);
        data.destinations().add(original);
        data.recordSafetyResult(id, false);

        data.replaceDestination(original.withDetails("Renamed", UUID.randomUUID(), Level.OVERWORLD,
            1, 64, 1, 90));
        assertEquals(DestinationSafetyResult.UNSAFE, data.safetyResult(id));

        Destination renamed = data.destination(id).orElseThrow();
        data.replaceDestination(renamed.withDetails(renamed.name(), renamed.groupId(), Level.NETHER,
            1, 64, 1, renamed.yaw()));
        assertEquals(DestinationSafetyResult.UNKNOWN, data.safetyResult(id));
    }

    @Test
    void regroupingPreservesDestinationStateAndSafetyHistory() {
        PortalPlayerData data = new PortalPlayerData();
        UUID id = UUID.randomUUID();
        UUID targetGroup = UUID.randomUUID();
        Destination original = new Destination(id, "Pinned", PortalPlayerData.DEFAULT_GROUP_ID,
            Level.NETHER, 12.5, 70, -4.25, 135, 10, 20, true);
        data.destinations().add(original);
        data.recordSafetyResult(id, false);

        data.replaceDestination(original.withGroup(targetGroup));

        assertEquals(original.withGroup(targetGroup), data.destination(id).orElseThrow());
        assertEquals(DestinationSafetyResult.UNSAFE, data.safetyResult(id));
    }
}
