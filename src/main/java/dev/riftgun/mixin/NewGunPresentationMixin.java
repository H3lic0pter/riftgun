package dev.riftgun.mixin;

import dev.riftgun.appearance.GunPresentation;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.portal.PortalGunItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only fresh stacks use this constructor; decode/copy constructors preserve absence in legacy saves. */
@Mixin(ItemStack.class)
abstract class NewGunPresentationMixin {
    //? if >=1.21.11 {
    /*@Inject(method = "<init>(Lnet/minecraft/core/Holder;I)V", at = @At("RETURN"))
    private void riftgun$markNew(net.minecraft.core.Holder<net.minecraft.world.item.Item> item,
            int count, CallbackInfo callback) {
        if (item.value() instanceof PortalGunItem) {
    *///?} else {
    @Inject(method = "<init>(Lnet/minecraft/world/level/ItemLike;I)V", at = @At("RETURN"))
    private void riftgun$markNew(ItemLike item, int count, CallbackInfo callback) {
        if (item.asItem() instanceof PortalGunItem) {
    //?}
            ((ItemStack) (Object) this).set(PortalGunComponents.PRESENTATION, GunPresentation.NEW);
        }
    }
}
