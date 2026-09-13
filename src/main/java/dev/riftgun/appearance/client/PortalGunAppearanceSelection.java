package dev.riftgun.appearance.client;

/** A preview never becomes current until the matching server acknowledgement arrives. */
public final class PortalGunAppearanceSelection {
    private String current;
    private String selected;
    private String pending;

    public void initialize(String skin) {
        current = skin;
        selected = skin;
        pending = null;
    }

    public void select(String skin) { selected = skin; }
    public String current() { return current; }
    public String selected() { return selected; }
    public boolean pending() { return pending != null; }
    public boolean ready() { return current != null; }

    public boolean canApply() {
        return ready() && !pending() && selected != null && !selected.equals(current);
    }

    public String submit() {
        if (!canApply()) throw new IllegalStateException("No unapplied skin selection");
        pending = selected;
        return pending;
    }

    public void acknowledge(String skin) {
        current = skin;
        pending = null;
    }

    public void reject() { pending = null; }
}
