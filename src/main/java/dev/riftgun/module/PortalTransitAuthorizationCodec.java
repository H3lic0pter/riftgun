package dev.riftgun.module;

import dev.riftgun.api.PortalTransitAuthorization;
import dev.riftgun.api.RiftResourceId;
import dev.riftgun.core.nbt.Nbt;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;

/** Version-neutral NBT codec for the opaque public-API transit snapshot. */
public final class PortalTransitAuthorizationCodec {
    public static CompoundTag save(PortalTransitAuthorization authorization) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Authority", authorization.authorityId().toString());
        tag.putString("DestinationDimension", authorization.destinationDimension().toString());
        return tag;
    }

    public static Optional<PortalTransitAuthorization> load(CompoundTag tag) {
        if (tag.isEmpty()) return Optional.empty();
        try {
            return Optional.of(new PortalTransitAuthorization(
                RiftResourceId.parse(Nbt.getString(tag, "Authority")),
                RiftResourceId.parse(Nbt.getString(tag, "DestinationDimension"))));
        } catch (IllegalArgumentException exception) {
            com.mojang.logging.LogUtils.getLogger().warn(
                "Ignoring invalid portal transit authorization snapshot", exception);
            return Optional.empty();
        }
    }

    private PortalTransitAuthorizationCodec() {}
}
