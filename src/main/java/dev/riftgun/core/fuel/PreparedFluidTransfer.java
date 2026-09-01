package dev.riftgun.core.fuel;

/** Prepares a copied container before committing a matching drain from an external source. */
public final class PreparedFluidTransfer {
    /**
     * The container must belong to a disposable copy: preparation can mutate it even when the
     * source later refuses the drain. Callers may publish that copy only when the outcome succeeds.
     */
    public static <R> Outcome<R> execute(Source<R> source, Container<R> container, int maximum) {
        if (maximum <= 0) return Outcome.failure();
        Offer<R> offered = source.simulate(maximum);
        if (!offered.available()) return Outcome.failure();

        int accepted = container.simulateFill(offered.resource(), offered.amount());
        if (accepted <= 0) return Outcome.failure();
        accepted = Math.min(accepted, offered.amount());
        if (container.fill(offered.resource(), accepted) != accepted) return Outcome.failure();

        int drained = source.drain(offered.resource(), accepted);
        if (drained <= 0 || drained > accepted) return Outcome.failure();
        int excess = accepted - drained;
        if (excess > 0 && container.drain(offered.resource(), excess) != excess) {
            throw new IllegalStateException("Prepared container could not discard excess fluid");
        }
        return new Outcome<>(offered.resource(), drained);
    }

    public interface Source<R> {
        Offer<R> simulate(int maximum);

        int drain(R resource, int maximum);
    }

    public interface Container<R> {
        int simulateFill(R resource, int maximum);

        int fill(R resource, int maximum);

        int drain(R resource, int maximum);
    }

    public record Offer<R>(R resource, int amount) {
        public boolean available() {
            return resource != null && amount > 0;
        }

        public static <R> Offer<R> empty() {
            return new Offer<>(null, 0);
        }
    }

    public record Outcome<R>(R resource, int amount) {
        public boolean success() {
            return resource != null && amount > 0;
        }

        public static <R> Outcome<R> failure() {
            return new Outcome<>(null, 0);
        }
    }

    private PreparedFluidTransfer() {}
}
