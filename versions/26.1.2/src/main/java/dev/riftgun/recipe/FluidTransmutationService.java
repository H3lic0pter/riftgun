package dev.riftgun.recipe;

import dev.riftgun.fuel.PortalFuelProfile;
import dev.riftgun.fuel.PortalFuelProfiles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/** Server-only world reaction executor; recipe definitions stay entirely data driven. */
public final class FluidTransmutationService {
    private static final Comparator<RecipeHolder<FluidTransmutationRecipe>> PRIORITY =
        Comparator.<RecipeHolder<FluidTransmutationRecipe>>comparingInt(
                holder -> holder.value().totalIngredientCount()).reversed()
            .thenComparing(Comparator.<RecipeHolder<FluidTransmutationRecipe>>comparingInt(
                holder -> holder.value().ingredients().size()).reversed())
            .thenComparing(RecipeHolder::id);
    private static final Map<ServerLevel, Map<Long, Long>> LAST_CHECKS = new WeakHashMap<>();

    public static void itemTick(ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level) || !item.isAlive()
            || item.getItem().isEmpty()) return;
        BlockPos sourcePos = item.blockPosition();
        if (!isIndependentWaterSource(level, sourcePos) || !reserveCheck(level, sourcePos)) return;
        transmute(level, sourcePos);
    }

    public static void reset() {
        LAST_CHECKS.clear();
    }

    static boolean isIndependentWaterSource(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.WATER)
            && state.getFluidState().is(Fluids.WATER)
            && state.getFluidState().isSource();
    }

    private static boolean reserveCheck(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        Map<Long, Long> checks = LAST_CHECKS.computeIfAbsent(level, ignored -> new HashMap<>());
        Long previous = checks.put(pos.asLong(), now);
        if (now % 200L == 0L) checks.entrySet().removeIf(entry -> entry.getValue() < now - 1L);
        return previous == null || previous != now;
    }

    private static boolean transmute(ServerLevel level, BlockPos sourcePos) {
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(sourcePos),
            entity -> entity.isAlive() && !entity.getItem().isEmpty()
                && entity.blockPosition().equals(sourcePos));
        if (entities.isEmpty()) return false;
        List<ItemStack> stacks = entities.stream().map(ItemEntity::getItem).toList();
        FluidTransmutationInput input = new FluidTransmutationInput(stacks);
        List<RecipeHolder<FluidTransmutationRecipe>> recipes = new ArrayList<>(
            level.getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value() instanceof FluidTransmutationRecipe recipe
                    && recipe.getType() == RiftGunRecipes.FLUID_TRANSMUTATION_TYPE.get())
                .map(holder -> new RecipeHolder<>(holder.id(), (FluidTransmutationRecipe) holder.value()))
                .toList());
        recipes.sort(PRIORITY);
        for (RecipeHolder<FluidTransmutationRecipe> holder : recipes) {
            FluidTransmutationRecipe recipe = holder.value();
            var plan = recipe.consumptionPlan(input.items());
            if (plan.isEmpty()) continue;
            if (apply(level, sourcePos, entities, recipe, plan.get())) return true;
        }
        return false;
    }

    private static boolean apply(ServerLevel level, BlockPos sourcePos, List<ItemEntity> entities,
                                 FluidTransmutationRecipe recipe, int[] consumption) {
        if (!isIndependentWaterSource(level, sourcePos) || consumption.length != entities.size()) return false;
        for (int index = 0; index < entities.size(); index++) {
            if (!entities.get(index).isAlive()
                || entities.get(index).getItem().getCount() < consumption[index]) return false;
        }

        Fluid output = recipe.result().getFluid();
        BlockState outputState = output.defaultFluidState().createLegacyBlock();
        if (!outputState.getFluidState().isSource()
            || outputState.getFluidState().getType() != output) return false;
        if (!level.setBlock(sourcePos, outputState, Block.UPDATE_ALL)) return false;

        for (int index = 0; index < entities.size(); index++) {
            int amount = consumption[index];
            if (amount > 0) entities.get(index).getItem().shrink(amount);
        }
        playFeedback(level, sourcePos, output);
        return true;
    }

    private static void playFeedback(ServerLevel level, BlockPos pos, Fluid output) {
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS,
            0.75F, 1.15F);
        int rgb = PortalFuelProfiles.resolve(output)
            .map(PortalFuelProfile::rgb).orElse(0x7FD9E8);
        level.sendParticles(new DustParticleOptions(rgb, 0.85F),
            pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5,
            14, 0.32, 0.22, 0.32, 0.015);
    }

    private FluidTransmutationService() {}
}
