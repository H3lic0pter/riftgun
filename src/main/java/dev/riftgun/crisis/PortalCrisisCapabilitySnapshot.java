package dev.riftgun.crisis;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.RiftGun;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.module.PortalGunCapabilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** One inventory/status scan shared by every crisis eligibility predicate for one transit. */
public record PortalCrisisCapabilitySnapshot(
    boolean mountedTransit,
    boolean fallRescue,
    boolean fallGuard,
    boolean lavaResistant,
    boolean spatialTearReady
) {
    public static final TagKey<net.minecraft.world.item.Item> FALL_RESCUE_ITEMS = TagKey.create(
        Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, "fall_rescue_items"));
    private static final List<PortalCrisisInventorySource> INVENTORY_SOURCES = new CopyOnWriteArrayList<>();

    static {
        registerInventorySource(player -> Stream.iterate(0, slot -> slot + 1)
            .limit(player.getInventory().getContainerSize())
            .map(player.getInventory()::getItem));
    }

    public static void registerInventorySource(PortalCrisisInventorySource source) {
        INVENTORY_SOURCES.add(source);
    }

    public static PortalCrisisCapabilitySnapshot capture(ServerPlayer player, ServerLevel targetLevel,
                                                          boolean mountedTransit) {
        List<ItemStack> items = INVENTORY_SOURCES.stream()
            .flatMap(source -> source.items(player))
            .filter(stack -> !stack.isEmpty())
            .toList();
        boolean ultraWarm = targetLevel.dimensionType().ultraWarm();
        boolean rescueItem = items.stream().anyMatch(stack ->
            stack.is(FALL_RESCUE_ITEMS) && (!stack.is(Items.WATER_BUCKET) || !ultraWarm));
        int smartDistance = PortalDataStore.load(player).settings().smartDistance();
        boolean guard = items.stream().anyMatch(stack -> stack.is(RiftGun.PORTAL_GUN.get())
            && PortalGunCapabilities.resolve(stack, smartDistance).fallGuard());

        boolean firePotion = items.stream().anyMatch(PortalCrisisCapabilitySnapshot::providesFireResistance);
        Holder<Enchantment> fireProtection = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.FIRE_PROTECTION);
        int fireProtectionLevel = 0;
        for (ItemStack armor : player.getArmorSlots()) {
            fireProtectionLevel += armor.getEnchantmentLevel(fireProtection);
        }
        boolean lava = player.hasEffect(MobEffects.FIRE_RESISTANCE)
            || firePotion
            || player.getArmorValue() >= RiftConfigs.server().crises().lavaMinimumArmor()
            || fireProtectionLevel >= RiftConfigs.server().crises().lavaMinimumFireProtection();

        boolean harmful = player.getActiveEffects().stream().anyMatch(effect ->
            effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
        float minimumHealth = Math.max((float) RiftConfigs.server().crises().spatialTearMinimumHealth(),
            player.getMaxHealth() * (float) RiftConfigs.server().crises().spatialTearMinimumHealthRatio());
        return new PortalCrisisCapabilitySnapshot(
            mountedTransit,
            rescueItem || guard,
            guard,
            lava,
            !harmful && player.getHealth() >= minimumHealth
        );
    }

    private static boolean providesFireResistance(ItemStack stack) {
        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION)) return false;
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        for (var effect : contents.getAllEffects()) {
            if (effect.getEffect().equals(MobEffects.FIRE_RESISTANCE)) return true;
        }
        return false;
    }
}
