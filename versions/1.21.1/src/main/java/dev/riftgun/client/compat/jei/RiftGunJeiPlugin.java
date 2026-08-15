package dev.riftgun.client.compat.jei;

import dev.riftgun.RiftGun;
import dev.riftgun.recipe.FluidTransmutationRecipe;
import dev.riftgun.recipe.RiftGunRecipes;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluids;

/** Optional JEI bridge. This class is discovered only when JEI is installed. */
@JeiPlugin
public final class RiftGunJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = id("jei_plugin");
    private static final RecipeType<RecipeHolder<FluidTransmutationRecipe>> TRANSMUTATION =
        RecipeType.createRecipeHolderType(id("fluid_transmutation"));

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, path);
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FluidTransmutationCategory(
            registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        registration.addRecipes(TRANSMUTATION,
            List.copyOf(level.getRecipeManager().getAllRecipesFor(
                RiftGunRecipes.FLUID_TRANSMUTATION_TYPE.get())));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Items.WATER_BUCKET, TRANSMUTATION);
    }

    private static final class FluidTransmutationCategory
        implements IRecipeCategory<RecipeHolder<FluidTransmutationRecipe>> {
        private static final int WIDTH = 134;
        private static final int HEIGHT = 56;
        private final IDrawable icon;
        private final IDrawable arrow;

        private FluidTransmutationCategory(IGuiHelper guiHelper) {
            icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(Items.WATER_BUCKET));
            arrow = guiHelper.getRecipeArrow();
        }

        @Override
        public RecipeType<RecipeHolder<FluidTransmutationRecipe>> getRecipeType() {
            return TRANSMUTATION;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.riftgun.fluid_transmutation");
        }

        @Override
        public int getWidth() {
            return WIDTH;
        }

        @Override
        public int getHeight() {
            return HEIGHT;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public ResourceLocation getRegistryName(RecipeHolder<FluidTransmutationRecipe> recipe) {
            return recipe.id();
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder,
                              RecipeHolder<FluidTransmutationRecipe> holder,
                              IFocusGroup focuses) {
            builder.addInputSlot(2, 7)
                .addIngredient(NeoForgeTypes.FLUID_STACK,
                    new net.neoforged.neoforge.fluids.FluidStack(Fluids.WATER, 1000))
                .setFluidRenderer(1000, true, 16, 40)
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(
                    Component.translatable("jei.riftgun.fluid_transmutation.water_source")));

            var ingredients = holder.value().ingredients();
            for (int index = 0; index < ingredients.size(); index++) {
                int x = 30 + (index % 2) * 18;
                int y = 2 + (index / 2) * 18;
                builder.addInputSlot(x, y)
                    .addItemStacks(Arrays.asList(ingredients.get(index).getItems()))
                    .setStandardSlotBackground();
            }

            builder.addOutputSlot(112, 7)
                .addIngredient(NeoForgeTypes.FLUID_STACK, holder.value().result())
                .setFluidRenderer(1000, true, 16, 40);
        }

        @Override
        public void draw(RecipeHolder<FluidTransmutationRecipe> recipe,
                         mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                         GuiGraphics graphics, double mouseX, double mouseY) {
            arrow.draw(graphics, 78, 20);
        }
    }
}
