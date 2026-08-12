package dev.riftgun.network;

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
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.relocation.EntityRelocationManager;
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
        if (player.isSpectator()) {
            player.displayClientMessage(Component.translatable("message.riftgun.spectator_denied"), true);
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
        PortalGunLocator.LocatedGun gun = keyboardShortcut
            ? PortalShortcutGunSelection.locate(player).orElse(null)
            : locateGun(player, request);
        if (gun == null) {
            if (!keyboardShortcut && request.contains("GunReference")) {
                PortalNetworking.sendGunReferenceInvalid(player);
            }
            if (action == PortalAction.OPEN_GUI) {
                String message = keyboardShortcut
                    && PortalShortcutGunSelection.mode() == PortalShortcutGunMode.HELD_HANDS
                    ? "message.riftgun.portal_gun_must_be_held"
                    : "message.riftgun.no_portal_gun";
                player.displayClientMessage(Component.translatable(message), true);
            } else if (!keyboardShortcut && action != PortalAction.CYCLE_PLACEMENT_MODE) {
                player.displayClientMessage(Component.translatable("message.riftgun.no_portal_gun"), true);
            }
            return;
        }
        if (action == PortalAction.OPEN_GUI) {
            PortalNetworking.sendSnapshot(player, true, gun);
            return;
        }
        if (action == PortalAction.OPEN_MODULES) {
            PortalModuleMenu.open(player, gun);
            return;
        }

        PortalPlayerData data = PortalDataStore.load(player);
        try {
            boolean changed = dispatch(player, data, gun, action, request);
            if (changed) sendChangedState(player, data, gun, action);
        } catch (PortalRequestException exception) {
            player.displayClientMessage(Component.translatable(exception.translationKey()), true);
        } catch (NumberFormatException exception) {
            player.displayClientMessage(Component.translatable("message.riftgun.invalid_coordinate"), true);
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
        if (mode == PortalPlacementMode.ENTITY_RELOCATION) {
            EntityRelocationManager.tryStart(player, data, gun, true);
            return;
        }
        if (mode == PortalPlacementMode.SMART && capabilities.entityRelocationSmartRouting()
            && EntityRelocationManager.tryStart(player, data, gun, false)) return;
        if (PortalPlayerTargetActions.openSelected(
            player, data, mode, gun, false)) return;
        UUID selected = data.selectedDestinationId();
        if (selected == null) {
            player.displayClientMessage(Component.translatable("message.riftgun.no_destination_selected"), true);
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
            case EDIT_DESTINATION -> PortalDestinationActions.edit(player, data, request, gun.stack());
            case DELETE_DESTINATION -> PortalDestinationActions.delete(data, request);
            case TOGGLE_PIN -> PortalDestinationActions.togglePin(data, request);
            case VIEW_DESTINATION -> PortalDestinationActions.view(data, request);
            case SELECT_DESTINATION -> PortalDestinationActions.select(data, request);
            case SELECT_PLAYER -> PortalPlayerTargetActions.select(player, data, request);
            case OPEN_PORTAL -> {
                PortalOpenCoordinator.request(player, data,
                    PortalRequestFields.id(request, "Destination"), true,
                    PortalOpenOrigin.GUI.resolvePlacement(data.settings().placementMode()), gun);
                yield false;
            }
            case OPEN_SELECTED -> {
                openSelected(player, data, requestedPlacement(request), gun);
                yield false;
            }
            case RELOCATE_ENTITY -> {
                EntityRelocationManager.tryStart(player, data, gun, true);
                yield false;
            }
            case CYCLE_PLACEMENT_MODE -> PortalGunActions.cyclePlacementMode(
                player, data, gun.stack(), request.getBoolean("Reverse"));
            case CREATE_GROUP -> PortalDestinationActions.createGroup(data, request);
            case RENAME_GROUP -> PortalDestinationActions.renameGroup(data, request);
            case DELETE_GROUP -> PortalDestinationActions.deleteGroup(data, request);
            case MOVE_GROUP -> PortalDestinationActions.moveGroup(data, request);
            case MOVE_DESTINATION_GROUP -> PortalDestinationActions.moveDestinationGroup(data, request);
            case SET_GROUP_EXPANDED -> PortalDestinationActions.setExpanded(data, request);
            case SET_SETTINGS -> PortalGunActions.updatePlayerSettings(player, data, request);
            case SET_GUN_MODULE_SETTINGS -> PortalGunActions.updateModuleSettings(data, request, gun.stack());
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
            case OPEN_GUI, OPEN_MODULES, SET_PRIVACY, SET_PRIVACY_OVERRIDE,
                 REQUEST_PRIVACY_PLAYERS -> false;
        };
    }

    private static void openSelected(ServerPlayer player, PortalPlayerData data,
                                     PortalPlacementMode mode, PortalGunLocator.LocatedGun gun) {
        if (PortalPlayerTargetActions.openSelected(player, data, mode, gun, false)) return;
        UUID selected = data.selectedDestinationId();
        if (selected == null) {
            throw PortalRequestFields.error("message.riftgun.no_destination_selected");
        }
        PortalOpenCoordinator.request(player, data, selected, false,
            PortalOpenOrigin.ITEM.resolvePlacement(mode), gun);
    }

    private static void sendChangedState(ServerPlayer player, PortalPlayerData data,
                                         PortalGunLocator.LocatedGun gun, PortalAction action) {
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
        if (player.getServer() != null) {
            PortalEntity.closeOwnedPortals(player.getServer(), player.getUUID());
        }
        player.displayClientMessage(Component.translatable("message.riftgun.portals_closed"), true);
    }

    private static PortalPlacementMode requestedPlacement(CompoundTag request) {
        PortalPlacementMode mode = PortalPlacementMode.parse(request.getString("PlacementMode"));
        return mode == PortalPlacementMode.SMART ? PortalPlacementMode.FRONT : mode;
    }

    private static PortalAction parseAction(CompoundTag request) {
        try {
            return PortalAction.valueOf(request.getString("Action"));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static PortalGunLocator.LocatedGun locateGun(ServerPlayer player, CompoundTag request) {
        return (request.contains("GunReference")
            ? PortalGunLocator.resolveReference(player, request.getCompound("GunReference"))
            : PortalGunLocator.first(player)).orElse(null);
    }

    private static boolean isKeyboardShortcut(PortalAction action, CompoundTag request) {
        return action.isExclusiveKeyboardShortcut()
            || action == PortalAction.OPEN_GUI && request.getBoolean("KeyboardShortcut");
    }

    private PortalRequestHandler() {}
}
