package dev.riftgun.data;

import dev.riftgun.core.RiftConstants;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/** Ordered internal registry for privacy capabilities. Public API exposure can be added later. */
public final class PortalPermissions {
//? if >=1.21.11 {
    /*private static final Map<Identifier, PortalPermissionDefinition> DEFINITIONS = new LinkedHashMap<>();
*///?} else {
    private static final Map<ResourceLocation, PortalPermissionDefinition> DEFINITIONS = new LinkedHashMap<>();
//?}

//? if >=1.21.11 {
    /*public static final Identifier PLAYER_PORTAL = id("player_portal");
*///?} else {
    public static final ResourceLocation PLAYER_PORTAL = id("player_portal");
//?}
//? if >=1.21.11 {
    /*public static final Identifier ENTITY_RELOCATION_DESTINATION = id("entity_relocation_destination");
*///?} else {
    public static final ResourceLocation ENTITY_RELOCATION_DESTINATION = id("entity_relocation_destination");
//?}
//? if >=1.21.11 {
    /*public static final Identifier ENTITY_RELOCATION_SUBJECT = id("entity_relocation_subject");
*///?} else {
    public static final ResourceLocation ENTITY_RELOCATION_SUBJECT = id("entity_relocation_subject");
//?}
//? if >=1.21.11 {
    /*public static final Identifier FOREIGN_EXIT_TRANSIT = id("foreign_exit_transit");
*///?} else {
    public static final ResourceLocation FOREIGN_EXIT_TRANSIT = id("foreign_exit_transit");
//?}

    static {
        register(new PortalPermissionDefinition(PLAYER_PORTAL, true,
            "screen.riftgun.permission.player_portal", PortalPermissionPolicy.ALLOW));
        register(new PortalPermissionDefinition(ENTITY_RELOCATION_DESTINATION, true,
            "screen.riftgun.permission.entity_relocation_destination", PortalPermissionPolicy.ASK));
        register(new PortalPermissionDefinition(ENTITY_RELOCATION_SUBJECT, true,
            "screen.riftgun.permission.entity_relocation_subject", PortalPermissionPolicy.ASK));
        register(new PortalPermissionDefinition(FOREIGN_EXIT_TRANSIT, false,
            "screen.riftgun.permission.foreign_exit_transit", PortalPermissionPolicy.ALLOW));
    }

    public static Collection<PortalPermissionDefinition> definitions() {
        return java.util.List.copyOf(DEFINITIONS.values());
    }

//? if >=1.21.11 {
    /*public static PortalPermissionDefinition definition(Identifier id) {
*///?} else {
    public static PortalPermissionDefinition definition(ResourceLocation id) {
//?}
        return DEFINITIONS.get(id);
    }

    static void register(PortalPermissionDefinition definition) {
        if (DEFINITIONS.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate portal permission " + definition.id());
        }
    }

//? if >=1.21.11 {
    /*private static Identifier id(String path) {
*///?} else {
    private static ResourceLocation id(String path) {
//?}
//? if >=1.21.11 {
        /*return Identifier.fromNamespaceAndPath(RiftConstants.MOD_ID, path);
*///?} else {
        return ResourceLocation.fromNamespaceAndPath(RiftConstants.MOD_ID, path);
//?}
    }

    private PortalPermissions() {}
}
