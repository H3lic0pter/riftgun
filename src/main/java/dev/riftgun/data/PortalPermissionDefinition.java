package dev.riftgun.data;

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/** Metadata used by authority checks, persistence and the data-driven Privacy Terminal. */
public record PortalPermissionDefinition(
//? if >=1.21.11 {
    /*Identifier id,
*///?} else {
    ResourceLocation id,
//?}
    boolean supportsAsk,
    String translationKey,
    PortalPermissionPolicy fallbackGlobalPolicy
) {
    public PortalPermissionDefinition {
        if (id == null || translationKey == null || translationKey.isBlank()) {
            throw new IllegalArgumentException("Permission id and translation key are required");
        }
        if (fallbackGlobalPolicy == PortalPermissionPolicy.FOLLOW_GLOBAL
            || !supportsAsk && fallbackGlobalPolicy == PortalPermissionPolicy.ASK) {
            throw new IllegalArgumentException("Invalid global fallback for " + id);
        }
    }
}
