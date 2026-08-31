package dev.riftgun.network;
import dev.riftgun.core.msg.Msg;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.module.PortalModuleMenu;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.service.PortalOpenCoordinator;
import dev.riftgun.service.PortalOpenOrigin;
import dev.riftgun.service.PortalShortcutGunMode;
import dev.riftgun.service.PortalShortcutGunSelection;
import dev.riftgun.service.VanillaInventoryPortalGunLocator;
import dev.riftgun.service.RandomRiftManager;
import dev.riftgun.service.CoordinateSharingService;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.relocation.EntityRelocationManager;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingEndpoint;
import dev.riftgun.pairing.PortalPairingManager;
import dev.riftgun.pairing.PortalPairingPendingEndpoints;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/** Validates request context, delegates domain actions and owns the common response policy. */
public final class PortalRequestHandler {
    public static void handle(ServerPlayer player, CompoundTag request) {
        PortalAction action = parseAction(request);
        if (action == null) return;
        // Chat sharing is intentionally available to spectators and does not require a Portal Gun.
        if (action == PortalAction.SHARE_DESTINATION_CHAT) {
            if (Nbt.hasUUID(request, "Destination")) {
                CoordinateSharingService.shareToChat(player, Nbt.getUUID(request, "Destination"));
            } else {
                Msg.displayClientMessage(player, Component.translatable("message.riftgun.invalid_request"), true);
            }
            return;
        }
        if (player.isSpectator()) {
            if (action == PortalAction.OPEN_MODE_RADIAL) {
                PortalNetworking.sendRadialUnavailable(player,
                    Nbt.getInt(request, "RadialRequestId"));
            }
            Msg.displayClientMessage(player, Component.translatable("message.riftgun.spectator_denied"), true);
            return;
        }
        if (action == PortalAction.CLOSE_PORTALS) {
            closePortals(player);
            return;
        }
        if (!action.requiresPortalGun()) {
            PortalPrivacyRequestHandler.handle(player, action, request);
            return;
        }

        boolean keyboardShortcut = isKeyboardShortcut(action, request);
        PortalGunLocator.LocatedGun gun = action == PortalAction.ADJUST_SURFACE_RANGE
            && !request.contains("GunReference")
            ? VanillaInventoryPortalGunLocator.locateMainHand(player).orElse(null)
            : keyboardShortcut && request.contains("GunReference")
                ? PortalGunLocator.resolveReference(
                    player, Nbt.getCompound(request, "GunReference")).orElse(null)
                : keyboardShortcut ? PortalShortcutGunSelection.locate(player).orElse(null)
                : locateGun(player, request);
        if (gun == null) {
            if (action == PortalAction.OPEN_MODE_RADIAL) {
                PortalNetworking.sendRadialUnavailable(player,
                    Nbt.getInt(request, "RadialRequestId"));
            }
            if (!keyboardShortcut && request.contains("GunReference")) {
                PortalNetworking.sendGunReferenceInvalid(player);
            }
            if (keyboardShortcut || action == PortalAction.OPEN_GUI) {
                String message = keyboardShortcut
                    && PortalShortcutGunSelection.mode() == PortalShortcutGunMode.HELD_HANDS
                    ? "message.riftgun.portal_gun_must_be_held"
                    : "message.riftgun.no_portal_gun";
                Msg.displayClientMessage(player, Component.translatable(message), true);
            } else if (!keyboardShortcut && action != PortalAction.CYCLE_PLACEMENT_MODE) {
                Msg.displayClientMessage(player, Component.translatable("message.riftgun.no_portal_gun"), true);
            }
            return;
        }
        if (action == PortalAction.OPEN_GUI) {
            PortalNetworking.sendSnapshot(player, true, gun);
            return;
        }
        if (action == PortalAction.OPEN_MODE_RADIAL) {
            PortalPlayerData radialData = PortalDataStore.load(player);
            PortalGunCapabilities radialCapabilities = PortalGunCapabilities.resolve(
                gun.stack(), radialData.settings().smartDistance());
            if (Nbt.getBoolean(request, "PrecisionPreview")
                && (!radialCapabilities.precisionPlacement()
                    || radialCapabilities.effectivePlacementMode(
                        radialData.settings().placementMode()) == PortalPlacementMode.ENTITY_RELOCATION)) {
                PortalNetworking.sendRadialUnavailable(player, Nbt.getInt(request, "RadialRequestId"));
                Msg.displayClientMessage(player, Component.translatable(
                    radialCapabilities.precisionPlacement()
                        ? "message.riftgun.precision_placement_unavailable"
                        : "message.riftgun.precision_placement_module_required"), true);
                return;
            }
            PortalNetworking.sendRadialSnapshot(player, gun,
                Nbt.getInt(request, "RadialRequestId"));
            return;
        }
        if (action == PortalAction.OPEN_MODULES) {
            PortalModuleMenu.open(player, gun);
            return;
        }

        PortalPlayerData data = PortalDataStore.load(player);
        try {
            boolean changed = dispatch(player, data, gun, action, request);
            if (changed) sendChangedState(player, data, gun, action, request);
        } catch (PortalRequestException exception) {
            Msg.displayClientMessage(player, Component.translatable(exception.translationKey()), true);
        } catch (NumberFormatException exception) {
            Msg.displayClientMessage(player, Component.translatable("message.riftgun.invalid_coordinate"), true);
        }
    }

    public static boolean canUse(ServerPlayer player) {
        return !player.isSpectator() && PortalGunLocator.anyHasPortalGun(player);
    }

    public static void openSelectedFromItem(ServerPlayer player, InteractionHand hand) {
        if (player.isSpectator()) return;
        PortalGunLocator.LocatedGun gun = PortalGunLocator.first(player).orElse(null);
        if (gun == null || gun.stack() != player.getItemInHand(hand)) return;
        PortalPlayerData data = PortalDataStore.load(player);
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            gun.stack(), data.settings().smartDistance());
        PortalPlacementMode mode = data.settings().placementMode();
        if (capabilities.functionMode() == PortalFunctionMode.PORTAL_PAIRING) {
            if (mode == PortalPlacementMode.ENTITY_RELOCATION) {
                if (player.isShiftKeyDown()) PortalPairingManager.setRelocationTarget(player, data, gun);
                else EntityRelocationManager.tryStart(player, data, gun, true);
                return;
            }
            PortalPairingManager.place(player, data, gun, mode,
                player.isShiftKeyDown() ? PortalPairingEndpoint.A : PortalPairingEndpoint.B);
            return;
        }
        if (mode == PortalPlacementMode.ENTITY_RELOCATION) {
            EntityRelocationManager.tryStart(player, data, gun, true);
            return;
        }
        if (mode == PortalPlacementMode.SMART && capabilities.entityRelocationSmartRouting()
            && EntityRelocationManager.tryStart(player, data, gun, false)) return;
        if (PortalPlayerTargetActions.openSelected(
            player, data, mode, gun, false)) return;
        if (ExternalDestinationActions.openSelected(player, data, mode, gun, false)) return;
        UUID selected = data.selectedDestinationId();
        if (selected == null) {
            Msg.displayClientMessage(player, Component.translatable("message.riftgun.no_destination_selected"), true);
            return;
        }
        PortalOpenCoordinator.request(player, data, selected, false,
            PortalOpenOrigin.ITEM.resolvePlacement(data.settings().placementMode()), gun);
    }

    private static boolean dispatch(ServerPlayer player, PortalPlayerData data,
                                    PortalGunLocator.LocatedGun gun, PortalAction action,
                                    CompoundTag request) {
        return switch (action) {
            case CREATE_CURRENT -> PortalDestinationActions.createCurrent(player, data, request);
            case CREATE_COORDINATE -> PortalDestinationActions.createCoordinate(
                player, data, request, gun.stack());
            case CREATE_DIMENSIONAL_COORDINATE -> PortalDestinationActions.createDimensionalCoordinate(
                player, data, request, gun.stack());
            case EDIT_DESTINATION -> PortalDestinationActions.edit(player, data, request, gun.stack());
            case SHARE_DESTINATION_CHAT -> {
                CoordinateSharingService.shareToChat(player, PortalRequestFields.id(request, "Destination"));
                yield false;
            }
            case CREATE_COORDINATE_NOTE -> {
                CoordinateSharingService.createNote(player, PortalRequestFields.id(request, "Destination"));
                yield false;
            }
            case DELETE_DESTINATION -> PortalDestinationActions.delete(data, request);
            case TOGGLE_PIN -> PortalDestinationActions.togglePin(data, request);
            case VIEW_DESTINATION -> PortalDestinationActions.view(data, request);
            case SELECT_DESTINATION -> {
                ExternalDestinationActions.clearSelection(player.getUUID());
                yield PortalDestinationActions.select(data, request);
            }
            case SELECT_EXTERNAL_DESTINATION -> ExternalDestinationActions.select(player, data, request);
            case SELECT_PLAYER -> {
                ExternalDestinationActions.clearSelection(player.getUUID());
                yield PortalPlayerTargetActions.select(player, data, request);
            }
            case OPEN_PORTAL -> {
                PortalOpenCoordinator.request(player, data,
                    PortalRequestFields.id(request, "Destination"), true,
                    PortalOpenOrigin.GUI.resolvePlacement(data.settings().placementMode()), gun);
                yield false;
            }
            case OPEN_EXTERNAL_DESTINATION -> {
                ExternalDestinationActions.openSelected(player, data, requestedPlacement(request), gun, true);
                yield false;
            }
            case OPEN_RANDOM_RIFT -> {
                RandomRiftManager.request(player, gun);
                yield false;
            }
            case OPEN_DIMENSIONAL_RIFT -> {
                RandomRiftManager.requestDimensional(player, gun, Nbt.getString(request, "Dimension"));
                yield false;
            }
            case OPEN_SELECTED -> {
                openSelected(player, data, requestedPlacement(request), gun);
                yield false;
            }
            case OPEN_SELECTED_SURFACE_FACE -> {
                openSelectedSurfaceFace(player, data, gun, SurfaceFaceRequest.decode(request),
                    Nbt.getBoolean(request, "EndpointA"));
                yield false;
            }
            case OPEN_SELECTED_PRECISION -> {
                openSelectedPrecision(player, data, gun, PrecisionPlacementRequest.decode(request),
                    Nbt.getBoolean(request, "EndpointA"),
                    Nbt.getBoolean(request, "PairingShortcut"));
                yield false;
            }
            case CLEAR_EXTERNAL_DESTINATION -> {
                ExternalDestinationActions.clearSelection(player.getUUID());
                yield false;
            }
            case RELOCATE_ENTITY -> {
                EntityRelocationManager.tryStart(player, data, gun, true);
                yield false;
            }
            case PLACE_PAIRING_ENDPOINT -> {
                PortalGunCapabilities pairingCapabilities = PortalGunCapabilities.resolve(
                    gun.stack(), data.settings().smartDistance());
                if (!pairingCapabilities.portalPairing()) {
                    throw PortalRequestFields.error("message.riftgun.portal_pairing_module_required");
                }
                if (data.settings().placementMode() == PortalPlacementMode.ENTITY_RELOCATION) {
                    if (Nbt.getBoolean(request, "EndpointA")) {
                        PortalPairingManager.setRelocationTargetFromShortcut(player, data, gun);
                    } else {
                        EntityRelocationManager.tryStart(player, data, gun, true);
                    }
                } else {
                    PortalPairingManager.placeFromShortcut(
                        player, data, gun, data.settings().placementMode(),
                        Nbt.getBoolean(request, "EndpointA")
                            ? PortalPairingEndpoint.A : PortalPairingEndpoint.B);
                }
                yield false;
            }
            case TOGGLE_FUNCTION_MODE -> PortalGunActions.toggleFunctionMode(
                player, data, gun.stack());
            case CYCLE_PLACEMENT_MODE -> PortalGunActions.cyclePlacementMode(
                player, data, gun.stack(), Nbt.getBoolean(request, "Reverse"));
            case SET_RADIAL_MODE -> PortalGunActions.setRadialMode(player, data, gun.stack(), request);
            case ADJUST_SURFACE_RANGE -> PortalGunActions.adjustRemoteDistance(
                player, data, gun.stack(), Nbt.getInt(request, "Step"));
            case CREATE_GROUP -> PortalDestinationActions.createGroup(data, request);
            case RENAME_GROUP -> PortalDestinationActions.renameGroup(data, request);
            case DELETE_GROUP -> PortalDestinationActions.deleteGroup(data, request);
            case MOVE_GROUP -> PortalDestinationActions.moveGroup(data, request);
            case MOVE_DESTINATION_GROUP -> PortalDestinationActions.moveDestinationGroup(data, request);
            case SET_GROUP_EXPANDED -> PortalDestinationActions.setExpanded(data, request);
            case SET_SETTINGS -> PortalGunActions.updatePlayerSettings(
                player, data, request, gun.stack());
            case SET_GUN_MODULE_SETTINGS -> PortalGunActions.updateModuleSettings(
                player, data, request, gun.stack());
            case TOGGLE_BUCKET_MODE -> PortalGunActions.toggleBucketMode(player, gun.stack());
            case CLEAR_GUN_FLUID -> PortalGunActions.clearFluid(player, gun.stack());
            case REQUEST_PLAYERS -> {
                PortalPlayerTargetActions.sendList(player, gun.stack());
                yield false;
            }
            case OPEN_PLAYER_PORTAL -> {
                PortalPlayerTargetActions.openFromRequest(player, data, request, gun);
                yield false;
            }
            case TOGGLE_PLAYER_PIN -> PortalPlayerTargetActions.togglePin(data, request);
            case CLOSE_PORTALS -> {
                closePortals(player);
                yield false;
            }
            case OPEN_GUI, OPEN_MODE_RADIAL, OPEN_MODULES, SET_PRIVACY, SET_PRIVACY_OVERRIDE,
                 REQUEST_PRIVACY_PLAYERS -> false;
        };
    }

    private static void openSelected(ServerPlayer player, PortalPlayerData data,
                                     PortalPlacementMode mode, PortalGunLocator.LocatedGun gun) {
        if (PortalPlayerTargetActions.openSelected(player, data, mode, gun, false)) return;
        if (ExternalDestinationActions.openSelected(player, data, mode, gun, false)) return;
        UUID selected = data.selectedDestinationId();
        if (selected == null) {
            throw PortalRequestFields.error("message.riftgun.no_destination_selected");
        }
        PortalOpenCoordinator.request(player, data, selected, false,
            PortalOpenOrigin.ITEM.resolvePlacement(mode), gun);
    }

    private static void openSelectedSurfaceFace(ServerPlayer player, PortalPlayerData data,
                                                PortalGunLocator.LocatedGun gun,
                                                SurfaceFaceRequest request, boolean endpointA) {
        PortalPlacementMode mode = data.settings().placementMode();
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            gun.stack(), data.settings().smartDistance());
        if (!capabilities.precisionPlacement()) {
            throw PortalRequestFields.error("message.riftgun.precision_placement_module_required");
        }
        if (capabilities.functionMode() == PortalFunctionMode.PORTAL_PAIRING) {
            PortalPairingManager.placeSurfaceFace(player, data, gun, mode,
                endpointA ? PortalPairingEndpoint.A : PortalPairingEndpoint.B,
                request);
            return;
        }
        if (PortalPlayerTargetActions.openSelectedSurfaceFace(
            player, data, mode, gun, request)) return;
        if (ExternalDestinationActions.openSelectedSurfaceFace(
            player, data, mode, gun, request)) return;
        UUID selected = data.selectedDestinationId();
        if (selected == null) {
            throw PortalRequestFields.error("message.riftgun.no_destination_selected");
        }
        PortalOpenCoordinator.requestSurfaceFace(player, data, selected, mode, gun, request);
    }

    private static void openSelectedPrecision(ServerPlayer player, PortalPlayerData data,
                                              PortalGunLocator.LocatedGun gun,
                                              PrecisionPlacementRequest request,
                                              boolean endpointA,
                                              boolean pairingShortcut) {
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
            gun.stack(), data.settings().smartDistance());
        if (!capabilities.precisionPlacement()) {
            throw PortalRequestFields.error("message.riftgun.precision_placement_module_required");
        }
        PortalPlacementMode mode = capabilities.effectivePlacementMode(data.settings().placementMode());
        if (request.kind() == PrecisionPlacementRequest.Kind.SURFACE) {
            if (mode != PortalPlacementMode.SURFACE && mode != PortalPlacementMode.SMART) {
                throw PortalRequestFields.error("message.riftgun.surface_mode_required");
            }
        } else {
            if (mode == PortalPlacementMode.SURFACE || mode == PortalPlacementMode.ENTITY_RELOCATION) {
                throw PortalRequestFields.error("message.riftgun.precision_placement_unavailable");
            }
            if (mode == PortalPlacementMode.SMART) {
                var fallback = pairingShortcut
                    ? capabilities.pairingSmartFallback() : capabilities.activeSmartFallback();
                mode = fallback == dev.riftgun.pairing.PortalFloatingFallback.REMOTE
                    ? PortalPlacementMode.REMOTE : PortalPlacementMode.FRONT;
            }
        }
        if (pairingShortcut || capabilities.functionMode() == PortalFunctionMode.PORTAL_PAIRING) {
            PortalPairingManager.placePrecision(player, data, gun, mode,
                endpointA ? PortalPairingEndpoint.A : PortalPairingEndpoint.B, request);
            return;
        }
        if (PortalPlayerTargetActions.openSelectedPrecision(player, data, mode, gun, request)) return;
        if (ExternalDestinationActions.openSelectedPrecision(player, data, mode, gun, request)) return;
        UUID selected = data.selectedDestinationId();
        if (selected == null) throw PortalRequestFields.error("message.riftgun.no_destination_selected");
        PortalOpenCoordinator.requestPrecision(player, data, selected, mode, gun, request);
    }

    private static void sendChangedState(ServerPlayer player, PortalPlayerData data,
                                         PortalGunLocator.LocatedGun gun, PortalAction action,
                                         CompoundTag request) {
        if (action == PortalAction.SET_GUN_MODULE_SETTINGS
            && (Nbt.getString(request, "Setting").equals("RemoteDistance")
                || Nbt.getString(request, "Setting").equals("SurfaceRange"))) {
            PortalNetworking.sendGunSnapshot(player, data, gun);
            return;
        }
        PortalDataStore.save(player, data);
        if (action == PortalAction.SELECT_DESTINATION) {
            PortalNetworking.sendSelectionAccepted(player, data.selectedDestinationId());
            return;
        }
        PortalNetworking.sendSnapshot(player, false, gun);
        if (action == PortalAction.SET_SETTINGS) {
            PortalPlayerTargetActions.sendList(player, gun.stack());
        }
    }

    private static void closePortals(ServerPlayer player) {
        PortalPairingPendingEndpoints.clearAll(player);
//? if >=1.21.11 {
        /*if (player.level().getServer() != null) {
*///?} else {
        if (player.getServer() != null) {
//?}
//? if >=1.21.11 {
            /*PortalEntity.closeOwnedPortals(player.level().getServer(), player.getUUID());
*///?} else {
            PortalEntity.closeOwnedPortals(player.getServer(), player.getUUID());
//?}
        }
        Msg.displayClientMessage(player, Component.translatable("message.riftgun.portals_closed"), true);
    }

    private static PortalPlacementMode requestedPlacement(CompoundTag request) {
        PortalPlacementMode mode = PortalPlacementMode.parse(Nbt.getString(request, "PlacementMode"));
        return mode == PortalPlacementMode.SMART ? PortalPlacementMode.FRONT : mode;
    }

    private static PortalAction parseAction(CompoundTag request) {
        try {
            return PortalAction.valueOf(Nbt.getString(request, "Action"));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static PortalGunLocator.LocatedGun locateGun(ServerPlayer player, CompoundTag request) {
        return (request.contains("GunReference")
            ? PortalGunLocator.resolveReference(player, Nbt.getCompound(request, "GunReference"))
            : PortalGunLocator.first(player)).orElse(null);
    }

    private static boolean isKeyboardShortcut(PortalAction action, CompoundTag request) {
        return action.isExclusiveKeyboardShortcut()
            || action == PortalAction.OPEN_GUI && Nbt.getBoolean(request, "KeyboardShortcut");
    }

    private PortalRequestHandler() {}
}
