package dev.riftgun.relocation;

import dev.riftgun.crisis.PortalCrisisConfigurationSnapshot;
import dev.riftgun.fuel.PortalFuelProfile;
import dev.riftgun.service.PortalPrivacyService;
import dev.riftgun.sound.PortalSoundSnapshot;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Owns one relocation request while it moves through preparation and opening. */
final class EntityRelocationSession {
    private final Context context;
    private State state;

    private EntityRelocationSession(Context context, State state) {
        this.context = context;
        this.state = state;
    }

    static EntityRelocationSession preparing(
            Context context, CompoundTag gunReference,
            EntityRelocationManager.ResolvedDestination destination,
            List<EntityRelocationManager.PermissionSnapshot> permissions,
            EntityRelocationPreparation preparation) {
        return new EntityRelocationSession(context,
            new Preparing(gunReference, destination, permissions, preparation));
    }

    static EntityRelocationSession opening(
            Context context, ItemStack gun,
            EntityRelocationManager.ResolvedDestination destination,
            UUID visualId, EntityRelocationExitService.Handle exitSetup,
            EntityRelocationManager.PreparedRoute preparedRoute, long startedAt) {
        return new EntityRelocationSession(context,
            new Opening(gun, destination, visualId, exitSetup, preparedRoute, startedAt));
    }

    void transitionToOpening(
            ItemStack gun, EntityRelocationManager.ResolvedDestination destination,
            UUID visualId, EntityRelocationExitService.Handle exitSetup,
            EntityRelocationManager.PreparedRoute preparedRoute, long startedAt) {
        if (!(state instanceof Preparing preparing)) {
            throw new IllegalStateException("relocation session is not preparing");
        }
        preparing.preparation().close();
        state = new Opening(gun, destination, visualId, exitSetup, preparedRoute, startedAt);
    }

    boolean preparing() {
        return state instanceof Preparing;
    }

    boolean opening() {
        return state instanceof Opening;
    }

    EntityRelocationRegistry.Reservation reservation() { return context.reservation(); }
    UUID ownerId() { return context.ownerId(); }
    UUID targetId() { return context.targetId(); }
    ResourceKey<Level> sourceDimension() { return context.sourceDimension(); }
    PortalFuelProfile profile() { return context.profile(); }
    PortalSoundSnapshot sounds() { return context.sounds(); }
    boolean fallGuard() { return context.fallGuard(); }
    boolean entityFallGuard() { return context.entityFallGuard(); }
    PortalCrisisConfigurationSnapshot crises() { return context.crises(); }
    List<PortalPrivacyService.GrantReservation> privacyReservations() {
        return context.privacyReservations();
    }
    int openingTicks() { return context.openingTicks(); }
    EntityRelocationFuelPolicy.Quote fuelQuote() { return context.fuelQuote(); }
    boolean virtualFuel() { return context.virtualFuel(); }
    EntityRelocationTree tree() { return context.tree(); }

    CompoundTag gunReference() { return preparingState().gunReference(); }
    List<EntityRelocationManager.PermissionSnapshot> permissions() {
        return preparingState().permissions();
    }
    EntityRelocationPreparation preparation() { return preparingState().preparation(); }

    ItemStack gun() { return openingState().gun(); }
    UUID visualId() { return openingState().visualId(); }
    EntityRelocationExitService.Handle exitSetup() { return openingState().exitSetup(); }
    EntityRelocationManager.PreparedRoute preparedRoute() {
        return openingState().preparedRoute();
    }
    long startedAt() { return openingState().startedAt(); }

    EntityRelocationManager.ResolvedDestination destination() {
        return switch (state) {
            case Preparing preparing -> preparing.destination();
            case Opening opening -> opening.destination();
        };
    }

    private Preparing preparingState() {
        if (state instanceof Preparing preparing) return preparing;
        throw new IllegalStateException("relocation session is not preparing");
    }

    private Opening openingState() {
        if (state instanceof Opening opening) return opening;
        throw new IllegalStateException("relocation session is not opening");
    }

    record Context(
        EntityRelocationRegistry.Reservation reservation,
        UUID ownerId,
        UUID targetId,
        ResourceKey<Level> sourceDimension,
        PortalFuelProfile profile,
        PortalSoundSnapshot sounds,
        boolean fallGuard,
        boolean entityFallGuard,
        PortalCrisisConfigurationSnapshot crises,
        List<PortalPrivacyService.GrantReservation> privacyReservations,
        int openingTicks,
        EntityRelocationFuelPolicy.Quote fuelQuote,
        boolean virtualFuel,
        EntityRelocationTree tree
    ) {
        Context {
            privacyReservations = List.copyOf(privacyReservations);
        }
    }

    private sealed interface State permits Preparing, Opening {}

    private record Preparing(
        CompoundTag gunReference,
        EntityRelocationManager.ResolvedDestination destination,
        List<EntityRelocationManager.PermissionSnapshot> permissions,
        EntityRelocationPreparation preparation
    ) implements State {
        private Preparing {
            gunReference = gunReference.copy();
            permissions = List.copyOf(permissions);
        }
    }

    private record Opening(
        ItemStack gun,
        EntityRelocationManager.ResolvedDestination destination,
        UUID visualId,
        EntityRelocationExitService.Handle exitSetup,
        EntityRelocationManager.PreparedRoute preparedRoute,
        long startedAt
    ) implements State {}
}
