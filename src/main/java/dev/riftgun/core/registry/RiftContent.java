package dev.riftgun.core.registry;

import dev.riftgun.block.PrivacyTerminalBlock;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalGunItem;
import dev.riftgun.relocation.EntityRelocationPortalEntity;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.particles.SimpleParticleType;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;

/** Loader-neutral references for root RiftGun content. */
public final class RiftContent {
    private static final RegistrySlot<PrivacyTerminalBlock> PRIVACY_TERMINAL_SLOT = slot("privacy_terminal");
    private static final RegistrySlot<BlockItem> PRIVACY_TERMINAL_ITEM_SLOT = slot("privacy_terminal");
    private static final RegistrySlot<PortalGunItem> PORTAL_GUN_SLOT = slot("portal_gun");
    private static final RegistrySlot<EntityType<PortalEntity>> PORTAL_SLOT = slot("portal");
    private static final RegistrySlot<EntityType<EntityRelocationPortalEntity>> RELOCATION_PORTAL_SLOT =
        slot("entity_relocation_portal");
    private static final RegistrySlot<SimpleParticleType> PORTAL_SPLASH_SLOT = slot("portal_splash");
    private static boolean installed;

    public static final RegistryRef<PrivacyTerminalBlock> PRIVACY_TERMINAL = PRIVACY_TERMINAL_SLOT;
    public static final RegistryRef<BlockItem> PRIVACY_TERMINAL_ITEM = PRIVACY_TERMINAL_ITEM_SLOT;
    public static final RegistryRef<PortalGunItem> PORTAL_GUN = PORTAL_GUN_SLOT;
    public static final RegistryRef<EntityType<PortalEntity>> PORTAL = PORTAL_SLOT;
    public static final RegistryRef<EntityType<EntityRelocationPortalEntity>> ENTITY_RELOCATION_PORTAL =
        RELOCATION_PORTAL_SLOT;
    public static final RegistryRef<SimpleParticleType> PORTAL_SPLASH = PORTAL_SPLASH_SLOT;

    public static synchronized void install(Installation installation) {
        if (installed) throw new IllegalStateException("RiftGun content refs already installed");
        PRIVACY_TERMINAL_SLOT.install(installation.privacyTerminal());
        PRIVACY_TERMINAL_ITEM_SLOT.install(installation.privacyTerminalItem());
        PORTAL_GUN_SLOT.install(installation.portalGun());
        PORTAL_SLOT.install(installation.portal());
        RELOCATION_PORTAL_SLOT.install(installation.entityRelocationPortal());
        PORTAL_SPLASH_SLOT.install(installation.portalSplash());
        installed = true;
    }

    public record Installation(
        Supplier<PrivacyTerminalBlock> privacyTerminal,
        Supplier<BlockItem> privacyTerminalItem,
        Supplier<PortalGunItem> portalGun,
        Supplier<EntityType<PortalEntity>> portal,
        Supplier<EntityType<EntityRelocationPortalEntity>> entityRelocationPortal,
        Supplier<SimpleParticleType> portalSplash
    ) {
        public Installation {
            Objects.requireNonNull(privacyTerminal, "privacyTerminal");
            Objects.requireNonNull(privacyTerminalItem, "privacyTerminalItem");
            Objects.requireNonNull(portalGun, "portalGun");
            Objects.requireNonNull(portal, "portal");
            Objects.requireNonNull(entityRelocationPortal, "entityRelocationPortal");
            Objects.requireNonNull(portalSplash, "portalSplash");
        }
    }

    private static <T> RegistrySlot<T> slot(String path) {
//? if >=1.21.11 {
        /*return new RegistrySlot<>(Identifier.fromNamespaceAndPath("riftgun", path));
*///?} else {
        return new RegistrySlot<>(ResourceLocation.fromNamespaceAndPath("riftgun", path));
//?}
    }

    private RiftContent() {}
}
