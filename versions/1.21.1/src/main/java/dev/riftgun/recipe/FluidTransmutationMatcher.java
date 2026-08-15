package dev.riftgun.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** Finds an exact, non-greedy allocation across item stacks, including overlapping tag ingredients. */
public final class FluidTransmutationMatcher {
    public static Optional<int[]> plan(List<SizedIngredient> requirements, List<ItemStack> stacks) {
        List<Ingredient> units = new ArrayList<>();
        for (SizedIngredient requirement : requirements) {
            for (int count = 0; count < requirement.count(); count++) {
                units.add(requirement.ingredient());
            }
        }
        boolean[][] candidates = new boolean[units.size()][stacks.size()];
        for (int unit = 0; unit < units.size(); unit++) {
            for (int stack = 0; stack < stacks.size(); stack++) {
                candidates[unit][stack] = units.get(unit).test(stacks.get(stack));
            }
        }
        return plan(candidates, stacks.stream().mapToInt(ItemStack::getCount).toArray());
    }

    static Optional<int[]> plan(boolean[][] candidates, int[] capacities) {
        if (Arrays.stream(capacities).anyMatch(capacity -> capacity < 0)) {
            throw new IllegalArgumentException("stack capacities cannot be negative");
        }
        Integer[] order = new Integer[candidates.length];
        for (int unit = 0; unit < candidates.length; unit++) {
            if (candidates[unit].length != capacities.length) {
                throw new IllegalArgumentException("candidate width must match stack count");
            }
            order[unit] = unit;
        }
        Arrays.sort(order, Comparator.comparingInt(unit -> candidateCapacity(candidates[unit], capacities)));
        int[] remaining = capacities.clone();
        int[] consumed = new int[capacities.length];
        return allocate(candidates, order, 0, remaining, consumed)
            ? Optional.of(consumed) : Optional.empty();
    }

    private static boolean allocate(boolean[][] candidates, Integer[] order, int orderIndex,
                                    int[] remaining, int[] consumed) {
        if (orderIndex >= order.length) return true;
        boolean[] unitCandidates = candidates[order[orderIndex]];
        for (int stackIndex = 0; stackIndex < remaining.length; stackIndex++) {
            if (remaining[stackIndex] <= 0 || !unitCandidates[stackIndex]) continue;
            remaining[stackIndex]--;
            consumed[stackIndex]++;
            if (allocate(candidates, order, orderIndex + 1, remaining, consumed)) return true;
            remaining[stackIndex]++;
            consumed[stackIndex]--;
        }
        return false;
    }

    private static int candidateCapacity(boolean[] candidates, int[] capacities) {
        int capacity = 0;
        for (int stack = 0; stack < candidates.length; stack++) {
            if (candidates[stack]) capacity += capacities[stack];
        }
        return capacity;
    }

    private FluidTransmutationMatcher() {}
}
