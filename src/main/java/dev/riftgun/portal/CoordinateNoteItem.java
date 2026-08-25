package dev.riftgun.portal;

import dev.riftgun.service.CoordinateSharingService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.21.11 {
/*import net.minecraft.world.InteractionResult;
*///?} else {
import net.minecraft.world.InteractionResultHolder;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Single-use physical coordinate snapshot. */
public final class CoordinateNoteItem extends Item {
    public CoordinateNoteItem(Properties properties) { super(properties); }

    @Override
//? if >=1.21.11 {
    /*public InteractionResult use(Level level, Player player, InteractionHand hand) {
*///?} else {
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
//?}
        ItemStack stack = player.getItemInHand(hand);
        boolean success = false;
        if (player instanceof ServerPlayer serverPlayer) {
            success = CoordinateSharingService.importNote(serverPlayer, stack)
                == CoordinateSharingService.Result.SUCCESS;
            if (success) stack.shrink(1);
        }
//? if >=1.21.11 {
        /*return success ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
*///?} else {
        return success ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide())
            : InteractionResultHolder.fail(stack);
//?}
    }
}
