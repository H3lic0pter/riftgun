package dev.riftgun.service;

import dev.riftgun.data.PortalPermissions;
import net.minecraft.resources.ResourceLocation;

/** Separates transient permission grants that authorize materially different actions. */
public enum PortalRequestPurpose {
    PORTAL(PortalPermissions.PLAYER_PORTAL, ""),
    ENTITY_RELOCATION_DESTINATION(
        PortalPermissions.ENTITY_RELOCATION_DESTINATION, ".entity_relocation_destination"),
    ENTITY_RELOCATION_SUBJECT(
        PortalPermissions.ENTITY_RELOCATION_SUBJECT, ".entity_relocation");

    private final ResourceLocation permissionId;
    private final String languageSuffix;

    PortalRequestPurpose(ResourceLocation permissionId, String languageSuffix) {
        this.permissionId = permissionId;
        this.languageSuffix = languageSuffix;
    }

    public ResourceLocation permissionId() {
        return permissionId;
    }

    String languageSuffix() {
        return languageSuffix;
    }
}
