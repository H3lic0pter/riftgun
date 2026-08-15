package dev.riftgun.relocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import org.junit.jupiter.api.Test;

final class EntityRelocationExitIndexTest {
    @Test
    void reservesTheStableExitWithTheLongestRemainingHold() {
        EntityRelocationExitIndex index = new EntityRelocationExitIndex();
        EntityRelocationExitIndex.DestinationKey destination = destination(12.0);
        EntityRelocationExitIndex.ExitReference shorter = reference();
        EntityRelocationExitIndex.ExitReference longer = reference();
        index.register(destination, shorter);
        index.register(destination, longer);
        FakeAccess access = new FakeAccess();
        access.states.put(shorter.portalId(), EntityRelocationExitIndex.Candidate.open(20));
        access.states.put(longer.portalId(), EntityRelocationExitIndex.Candidate.open(55));

        EntityRelocationExitIndex.Lease lease = index.reserveStable(destination, 2.4F, access)
            .orElseThrow();

        assertEquals(longer, lease.exit());
        assertEquals(2.4F, access.reservedSides.get(longer.portalId()));
    }

    @Test
    void openingAndClosingExitsAreNotSharedAndPositionIsPartOfIdentity() {
        EntityRelocationExitIndex index = new EntityRelocationExitIndex();
        EntityRelocationExitIndex.DestinationKey destination = destination(12.0);
        EntityRelocationExitIndex.ExitReference opening = reference();
        EntityRelocationExitIndex.ExitReference closing = reference();
        index.register(destination, opening);
        index.register(destination, closing);
        FakeAccess access = new FakeAccess();
        access.states.put(opening.portalId(), EntityRelocationExitIndex.Candidate.opening());
        access.states.put(closing.portalId(), EntityRelocationExitIndex.Candidate.closing());

        assertTrue(index.reserveStable(destination, 1.0F, access).isEmpty());
        assertTrue(index.reserveStable(destination(13.0), 1.0F, access).isEmpty());
    }

    private static EntityRelocationExitIndex.DestinationKey destination(double x) {
        //? if >=1.21.11 {
        /*return new EntityRelocationExitIndex.DestinationKey(
            new UUID(1L, 2L), Identifier.withDefaultNamespace("overworld"), x, 64.0, 8.0);
        *///?} else {
        return new EntityRelocationExitIndex.DestinationKey(
            new UUID(1L, 2L), ResourceLocation.withDefaultNamespace("overworld"), x, 64.0, 8.0);
        //?}
    }

    private static EntityRelocationExitIndex.ExitReference reference() {
        //? if >=1.21.11 {
        /*return new EntityRelocationExitIndex.ExitReference(
            UUID.randomUUID(), Identifier.withDefaultNamespace("overworld"));
        *///?} else {
        return new EntityRelocationExitIndex.ExitReference(
            UUID.randomUUID(), ResourceLocation.withDefaultNamespace("overworld"));
        //?}
    }

    private static final class FakeAccess implements EntityRelocationExitIndex.CandidateAccess {
        private final Map<UUID, EntityRelocationExitIndex.Candidate> states = new HashMap<>();
        private final Map<UUID, Float> reservedSides = new HashMap<>();

        @Override
        public EntityRelocationExitIndex.Candidate inspect(
                EntityRelocationExitIndex.ExitReference exit) {
            return states.getOrDefault(exit.portalId(), EntityRelocationExitIndex.Candidate.missing());
        }

        @Override
        public boolean tryReserve(EntityRelocationExitIndex.ExitReference exit, float requiredSide) {
            reservedSides.put(exit.portalId(), requiredSide);
            return true;
        }
    }
}
