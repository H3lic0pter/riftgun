package dev.riftgun.service;

import dev.riftgun.data.PortalPermissions;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/** Separates transient permission grants that authorize materially different actions. */
public enum PortalRequestPurpose {
    PORTAL(PortalPermissions.PLAYER_PORTAL, ""),
    ENTITY_RELOCATION_DESTINATION(
        PortalPermissions.ENTITY_RELOCATION_DESTINATION, ".entity_relocation_destination"),
    ENTITY_RELOCATION_SUBJECT(
        PortalPermissions.ENTITY_RELOCATION_SUBJECT, ".entity_relocation");

//? if >=1.21.11 {
    /*private final Identifier permissionId;
*///?} else {
    private final ResourceLocation permissionId;
//?}
    private final String languageSuffix;

//? if >=1.21.11 {
    /*PortalRequestPurpose(Identifier permissionId, String languageSuffix) {
*///?} else {
    PortalRequestPurpose(ResourceLocation permissionId, String languageSuffix) {
//?}
        this.permissionId = permissionId;
        this.languageSuffix = languageSuffix;
    }

//? if >=1.21.11 {
    /*public Identifier permissionId() {
*///?} else {
    public ResourceLocation permissionId() {
//?}
        return permissionId;
    }

    String languageSuffix() {
        return languageSuffix;
    }
}
