package dev.riftgun.data;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Per-requester aggregate mode plus custom permission values, including unknown IDs. */
public final class PlayerPermissionProfile {
    private PlayerPermissionProfileMode mode = PlayerPermissionProfileMode.FOLLOW_GLOBAL;
    private final Map<ResourceLocation, PortalPermissionPolicy> values = new LinkedHashMap<>();

    public PlayerPermissionProfileMode mode() {
        return mode;
    }

    public void mode(PlayerPermissionProfileMode value) {
        mode = value == null ? PlayerPermissionProfileMode.FOLLOW_GLOBAL : value;
        if (mode != PlayerPermissionProfileMode.CUSTOM) values.clear();
    }

    public Map<ResourceLocation, PortalPermissionPolicy> values() {
        return values;
    }

    public PortalPermissionPolicy configured(ResourceLocation permissionId) {
        return switch (mode) {
            case FOLLOW_GLOBAL -> PortalPermissionPolicy.FOLLOW_GLOBAL;
            case ALLOW_ALL -> PortalPermissionPolicy.ALLOW;
            case DENY_ALL -> PortalPermissionPolicy.DENY;
            case CUSTOM -> values.getOrDefault(permissionId, PortalPermissionPolicy.FOLLOW_GLOBAL);
        };
    }

    public void customize(ResourceLocation permissionId, PortalPermissionPolicy policy) {
        if (mode == PlayerPermissionProfileMode.ALLOW_ALL
            || mode == PlayerPermissionProfileMode.DENY_ALL) {
            PortalPermissionPolicy materialized = mode == PlayerPermissionProfileMode.ALLOW_ALL
                ? PortalPermissionPolicy.ALLOW : PortalPermissionPolicy.DENY;
            for (PortalPermissionDefinition definition : PortalPermissions.definitions()) {
                values.put(definition.id(), materialized);
            }
        } else if (mode != PlayerPermissionProfileMode.CUSTOM) {
            values.clear();
        }
        mode = PlayerPermissionProfileMode.CUSTOM;
        if (policy == PortalPermissionPolicy.FOLLOW_GLOBAL) values.remove(permissionId);
        else values.put(permissionId, policy);
    }
}
