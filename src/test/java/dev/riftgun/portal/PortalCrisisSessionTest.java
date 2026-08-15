package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.crisis.PortalCrisisConfigurationSnapshot;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalCrisisSessionTest {
    @Test
    void linkedSessionsShareEvaluationAndExitQuota() {
        PortalCrisisSession entry = unstableSession();
        PortalCrisisSession exit = unstableSession();
        UUID player = UUID.randomUUID();

        assertTrue(entry.reserve(player, exit, 8));
        assertFalse(exit.reserve(player, entry, 8));
        assertTrue(entry.canCreateExit(exit, 1));
        entry.commitExit(exit);
        assertFalse(entry.canCreateExit(exit, 1));
        assertFalse(exit.canCreateExit(entry, 1));
    }

    @Test
    void deferredCopyAndNbtRoundTripPreserveTheSession() {
        PortalCrisisSession source = unstableSession();
        UUID evaluated = UUID.randomUUID();
        source.reserve(evaluated, null, 8);
        source.commitExit(null);

        PortalCrisisSession deferred = new PortalCrisisSession();
        deferred.configure(source.configuration());
        source.copyPairStateTo(deferred, 8);
        CompoundTag saved = new CompoundTag();
        deferred.save(saved);

        PortalCrisisSession restored = new PortalCrisisSession();
        restored.load(saved, 8);
        assertTrue(restored.configuration().unstable());
        assertFalse(restored.reserve(evaluated, null, 8));
        assertFalse(restored.canCreateExit(null, 1));
    }

    @Test
    void stableSessionsNeverReserveACrisisRoll() {
        assertFalse(new PortalCrisisSession().reserve(UUID.randomUUID(), null, 8));
    }

    @Test
    void returnLinkageRoundTripRetainsOnlyTheAssignedPlayer() {
        UUID playerId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        //? if >=1.21.11 {
        /*ResourceKey<Level> dimension = ResourceKey.create(
            Registries.DIMENSION, Identifier.fromNamespaceAndPath("riftgun", "test"));
        *///?} else {
        ResourceKey<Level> dimension = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("riftgun", "test"));
        //?}
        PortalCrisisSession source = new PortalCrisisSession();
        source.configureReturn(playerId,
            new PortalExitTarget(UUID.randomUUID(), dimension, new Vec3(1.5, 64.0, -2.5), 90.0F),
            parentId, dimension);

        CompoundTag saved = new CompoundTag();
        source.save(saved);
        PortalCrisisSession restored = new PortalCrisisSession();
        restored.load(saved, 8);

        assertTrue(restored.isReturnExit());
        assertTrue(restored.allowsReturn(playerId, false));
        assertFalse(restored.allowsReturn(playerId, true));
        assertFalse(restored.allowsReturn(UUID.randomUUID(), false));
        assertNotNull(restored.returnTarget());
        assertEquals(dimension, restored.returnTarget().dimension());
    }

    private static PortalCrisisSession unstableSession() {
        PortalCrisisSession session = new PortalCrisisSession();
        session.configure(new PortalCrisisConfigurationSnapshot(true, Map.of()));
        return session;
    }
}
