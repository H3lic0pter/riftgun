package dev.riftgun.relocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/** Indexed directory of visual exits that may be leased by later saved-destination relocations. */
public final class EntityRelocationExitIndex {
    private final Map<DestinationKey, Set<ExitReference>> exitsByDestination = new HashMap<>();
    private final Map<UUID, DestinationKey> destinationByExit = new HashMap<>();

    public void register(DestinationKey destination, ExitReference exit) {
        DestinationKey oldDestination = destinationByExit.put(exit.portalId(), destination);
        if (oldDestination != null && !oldDestination.equals(destination)) {
            removeFromDestination(oldDestination, exit.portalId());
        }
        exitsByDestination.computeIfAbsent(destination, ignored -> new LinkedHashSet<>()).add(exit);
    }

    public Optional<Lease> reserveStable(DestinationKey destination, float requiredSide,
                                         CandidateAccess access) {
        Set<ExitReference> registered = exitsByDestination.get(destination);
        if (registered == null || registered.isEmpty()) return Optional.empty();

        ArrayList<RankedExit> stable = new ArrayList<>();
        Iterator<ExitReference> iterator = registered.iterator();
        while (iterator.hasNext()) {
            ExitReference exit = iterator.next();
            Candidate candidate = access.inspect(exit);
            if (candidate.state() == CandidateState.MISSING
                || candidate.state() == CandidateState.CLOSING) {
                iterator.remove();
                destinationByExit.remove(exit.portalId(), destination);
            } else if (candidate.state() == CandidateState.OPEN) {
                stable.add(new RankedExit(exit, candidate.remainingOpenTicks()));
            }
        }
        if (registered.isEmpty()) exitsByDestination.remove(destination);

        stable.sort(Comparator.comparingInt(RankedExit::remainingOpenTicks).reversed());
        for (RankedExit candidate : stable) {
            if (access.tryReserve(candidate.exit(), requiredSide)) {
                return Optional.of(new Lease(destination, candidate.exit()));
            }
        }
        return Optional.empty();
    }

    public void unregister(UUID portalId) {
        DestinationKey destination = destinationByExit.remove(portalId);
        if (destination != null) removeFromDestination(destination, portalId);
    }

    public void clear() {
        exitsByDestination.clear();
        destinationByExit.clear();
    }

    private void removeFromDestination(DestinationKey destination, UUID portalId) {
        Set<ExitReference> exits = exitsByDestination.get(destination);
        if (exits == null) return;
        exits.removeIf(exit -> exit.portalId().equals(portalId));
        if (exits.isEmpty()) exitsByDestination.remove(destination);
    }

    public interface CandidateAccess {
        Candidate inspect(ExitReference exit);

        boolean tryReserve(ExitReference exit, float requiredSide);
    }

    public enum CandidateState {
        OPENING,
        OPEN,
        CLOSING,
        MISSING
    }

    public record Candidate(CandidateState state, int remainingOpenTicks) {
        public Candidate {
            remainingOpenTicks = Math.max(0, remainingOpenTicks);
        }

        public static Candidate opening() {
            return new Candidate(CandidateState.OPENING, 0);
        }

        public static Candidate open(int remainingOpenTicks) {
            return new Candidate(CandidateState.OPEN, remainingOpenTicks);
        }

        public static Candidate closing() {
            return new Candidate(CandidateState.CLOSING, 0);
        }

        public static Candidate missing() {
            return new Candidate(CandidateState.MISSING, 0);
        }
    }

//? if >=1.21.11 {
    /*public record DestinationKey(UUID destinationId, Identifier dimension,
*///?} else {
    public record DestinationKey(UUID destinationId, ResourceLocation dimension,
//?}
                                 double x, double y, double z) {}

//? if >=1.21.11 {
    /*public record ExitReference(UUID portalId, Identifier dimension) {}
*///?} else {
    public record ExitReference(UUID portalId, ResourceLocation dimension) {}
//?}

    public record Lease(DestinationKey destination, ExitReference exit) {}

    private record RankedExit(ExitReference exit, int remainingOpenTicks) {}
}
