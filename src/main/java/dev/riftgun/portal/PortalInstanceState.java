package dev.riftgun.portal;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.pairing.PortalPairingPendingEndpoint;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/** Persistent player state. Live portals remain entities indexed by PortalOwnerIndex. */
public record PortalInstanceState(@Nullable PortalPairingPendingEndpoint pending, boolean pairingManaged) {
    public static final PortalInstanceState EMPTY = new PortalInstanceState(null, false);

    public PortalInstanceState withPending(@Nullable PortalPairingPendingEndpoint next) {
        // Remember explicit clears so legacy guns/entities cannot resurrect an old marker.
        return new PortalInstanceState(next, true);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("PairingManaged", pairingManaged);
        if (pending != null) tag.put("Pending", pending.save());
        return tag;
    }

    public static PortalInstanceState load(CompoundTag tag) {
        var pending = tag.contains("Pending")
            ? PortalPairingPendingEndpoint.load(Nbt.getCompound(tag, "Pending")) : null;
        return new PortalInstanceState(pending, pending != null || Nbt.getBoolean(tag, "PairingManaged"));
    }
}
