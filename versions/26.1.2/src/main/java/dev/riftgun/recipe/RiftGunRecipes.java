package dev.riftgun.recipe;

import dev.riftgun.core.RiftConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RiftGunRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, RiftConstants.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, RiftConstants.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FluidTransmutationRecipe>>
        FLUID_TRANSMUTATION_SERIALIZER = SERIALIZERS.register(
            "fluid_transmutation", FluidTransmutationRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<FluidTransmutationRecipe>>
        FLUID_TRANSMUTATION_TYPE = TYPES.register("fluid_transmutation", () -> new RecipeType<>() {
            @Override
            public String toString() {
                return RiftConstants.MOD_ID + ":fluid_transmutation";
            }
        });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AdvancedBasicModuleRecipe>>
        ADVANCED_BASIC_MODULE_SERIALIZER = SERIALIZERS.register(
            "advanced_basic_module", () -> new SimpleCraftingRecipeSerializer<>(AdvancedBasicModuleRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ZeroPointFuelModuleRecipe>>
        ZERO_POINT_FUEL_MODULE_SERIALIZER = SERIALIZERS.register(
            "zero_point_fuel_module", () -> new SimpleCraftingRecipeSerializer<>(ZeroPointFuelModuleRecipe::new));

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
        TYPES.register(bus);
    }

    private RiftGunRecipes() {}
}
