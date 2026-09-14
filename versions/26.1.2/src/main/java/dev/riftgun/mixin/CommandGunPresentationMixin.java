package dev.riftgun.mixin;

import dev.riftgun.appearance.GunPresentation;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.portal.PortalGunItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Creation through a component patch bypasses the fresh-stack constructor in this version. */
@Mixin(net.minecraft.commands.arguments.item.ItemInput.class)
abstract class CommandGunPresentationMixin {
    @Inject(method = "createItemStack", at = @At("RETURN"))
    private void riftgun$markCreated(CallbackInfoReturnable<ItemStack> callback) {
        ItemStack stack = callback.getReturnValue();
        if (stack.getItem() instanceof PortalGunItem && !stack.has(PortalGunComponents.PRESENTATION)) {
            stack.set(PortalGunComponents.PRESENTATION, GunPresentation.NEW);
        }
    }
}
