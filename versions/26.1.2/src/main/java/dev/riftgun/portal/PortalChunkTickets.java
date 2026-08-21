package dev.riftgun.portal;

import dev.riftgun.core.RiftConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.TicketType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 26.1.2 chunk tickets used by open portals. Ticket types are registry entries
 * (serialized by id into level.dat), so they must be registered instead of
 * constructed ad hoc.
 */
public final class PortalChunkTickets {
    private static final int PORTAL_FLAGS =
        TicketType.FLAG_PERSIST | TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION
            | TicketType.FLAG_KEEP_DIMENSION_ACTIVE;
    private static final DeferredRegister<TicketType> TICKET_TYPES =
        DeferredRegister.create(BuiltInRegistries.TICKET_TYPE, RiftConstants.MOD_ID);

    public static final DeferredHolder<TicketType, TicketType> PORTAL = TICKET_TYPES.register(
        "portal", () -> new TicketType(TicketType.NO_TIMEOUT, PORTAL_FLAGS));
    public static final DeferredHolder<TicketType, TicketType> RELOCATION_EXIT = TICKET_TYPES.register(
        "entity_relocation_exit", () -> new TicketType(TicketType.NO_TIMEOUT, PORTAL_FLAGS));
    public static final DeferredHolder<TicketType, TicketType> RELOCATION_PREPARATION = TICKET_TYPES.register(
        "entity_relocation_preparation", () -> new TicketType(TicketType.NO_TIMEOUT, PORTAL_FLAGS));
    public static final DeferredHolder<TicketType, TicketType> RANDOM_RIFT_PREPARATION = TICKET_TYPES.register(
        "random_rift_preparation", () -> new TicketType(TicketType.NO_TIMEOUT, PORTAL_FLAGS));

    private PortalChunkTickets() {}

    public static void register(IEventBus modBus) {
        TICKET_TYPES.register(modBus);
    }
}
