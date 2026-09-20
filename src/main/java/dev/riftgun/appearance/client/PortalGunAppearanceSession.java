package dev.riftgun.appearance.client;

import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.fuel.PortalGunVisualState;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Owns one screen's target and correlates replies; never consults a newly held item. */
public final class PortalGunAppearanceSession {
    private final CompoundTag reference;
    private final PortalGunAppearanceSelection selection = new PortalGunAppearanceSelection();
    private final ItemStack preview = new ItemStack(RiftContent.PORTAL_GUN.get());
    private String requestId = "";
    private String error = "";
    private boolean opened;
    private boolean invalid;
    private long requestedAt;

    public PortalGunAppearanceSession(CompoundTag context) {
        reference = Nbt.getCompound(context, "GunReference").copy();
        preview.set(PortalGunComponents.VISUAL_STATE, new PortalGunVisualState(0, false, 0xFFFFFF));
    }

    public void open() {
        if (opened) return;
        opened = true;
        send(PortalAction.OPEN_APPEARANCE, "");
    }

    private void send(PortalAction action, String skin) {
        error = "";
        requestId = UUID.randomUUID().toString();
        requestedAt = System.nanoTime();
        PortalNetworking.sendRequest(action, tag -> {
            tag.put("GunReference", reference.copy());
            tag.putString("RequestId", requestId);
            tag.putString("Skin", skin);
        });
    }

    public boolean receive(CompoundTag response) {
        if (requestId.isEmpty() || !requestId.equals(Nbt.getString(response, "RequestId"))) return false;
        requestId = "";
        error = Nbt.getString(response, "Error");
        if (!error.isEmpty()) {
            selection.reject();
            invalid = error.equals("screen.riftgun.appearance.invalid_gun")
                || error.equals("message.riftgun.skin_module_required")
                || error.equals("message.riftgun.spectator_denied");
            return true;
        }
        String skin = Nbt.getString(response, "Skin");
        if (!selection.ready()) selection.initialize(skin);
        else selection.acknowledge(skin);
        preview.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Nbt.getBoolean(response, "Foil"));
        preview.set(PortalGunComponents.VISUAL_STATE, new PortalGunVisualState(
            Nbt.getInt(response, "LiquidTint"), Nbt.getBoolean(response, "CoreVisible"),
            Nbt.getInt(response, "FuelRgb"), Nbt.getBoolean(response, "PairingMode")));
        PortalGunSkin.set(preview, PortalGunSkin.validId(selection.selected())
            ? selection.selected() : PortalGunSkin.DEFAULT);
        return true;
    }

    public void tick() {
        if (!requestId.isEmpty() && System.nanoTime() - requestedAt > 10_000_000_000L) {
            // Keep the request ID: a delayed authoritative reply can still recover this screen.
            error = "screen.riftgun.appearance.timeout";
        }
    }

    public void select(String id) {
        selection.select(id);
        PortalGunSkin.set(preview, id);
    }

    public void apply() {
        if (canApply()) send(PortalAction.SET_APPEARANCE, selection.submit());
    }

    public boolean canApply() {
        return !invalid && requestId.isEmpty() && selection.canApply()
            && PortalGunSkinCatalog.contains(selection.selected());
    }

    public PortalGunAppearanceSelection selection() { return selection; }
    public ItemStack preview() { return preview; }
    public String error() { return error; }
    public boolean loading() { return !requestId.isEmpty(); }
    public CompoundTag reference() { return reference.copy(); }

}
