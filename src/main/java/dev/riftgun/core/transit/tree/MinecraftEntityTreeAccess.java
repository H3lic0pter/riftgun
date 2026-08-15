package dev.riftgun.core.transit.tree;
import dev.riftgun.core.nbt.Nbt;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Shared Minecraft entity adapter for portal and relocation passenger trees. */
public enum MinecraftEntityTreeAccess
        implements PassengerTreeTransfer.Access<Entity, UUID> {
    INSTANCE;

    @Override
    public UUID identity(Entity entity) {
        return entity.getUUID();
    }

    @Override
    public List<Entity> passengers(Entity entity) {
        return List.copyOf(entity.getPassengers());
    }

    @Override
    public void detachPassengers(Entity entity) {
        entity.ejectPassengers();
    }

    @Override
    public boolean attach(Entity passenger, Entity vehicle) {
        return passenger.level() == vehicle.level()
            && passenger.startRiding(vehicle, true);
    }

    @Override
    public boolean synchronizeRoot(Entity root) {
        if (root.getControllingPassenger() instanceof ServerPlayer player) {
            player.connection.send(new ClientboundMoveVehiclePacket(root));
        }
        return true;
    }
}
