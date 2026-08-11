package dev.riftgun.data;

import dev.riftgun.RiftGun;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Ordered internal registry for privacy capabilities. Public API exposure can be added later. */
public final class PortalPermissions {
    private static final Map<ResourceLocation, PortalPermissionDefinition> DEFINITIONS = new LinkedHashMap<>();

    public static final ResourceLocation PLAYER_PORTAL = id("player_portal");
    public static final ResourceLocation ENTITY_RELOCATION_DESTINATION = id("entity_relocation_destination");
    public static final ResourceLocation ENTITY_RELOCATION_SUBJECT = id("entity_relocation_subject");
    public static final ResourceLocation FOREIGN_EXIT_TRANSIT = id("foreign_exit_transit");

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

    public static PortalPermissionDefinition definition(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }

    static void register(PortalPermissionDefinition definition) {
        if (DEFINITIONS.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate portal permission " + definition.id());
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, path);
    }

    private PortalPermissions() {}
}
