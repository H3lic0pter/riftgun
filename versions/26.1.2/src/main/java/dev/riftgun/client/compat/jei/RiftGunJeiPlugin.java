package dev.riftgun.client.compat.jei;

import dev.riftgun.core.RiftConstants;
import dev.riftgun.recipe.FluidTransmutationRecipe;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Optional JEI bridge. This class is discovered only when JEI is installed.
 *
 * <p>The special crafting recipes (advanced basic module chain) are non-special
 * with a placeable {@code placementInfo}, so JEI's crafting category collects
 * them automatically exactly like the 1.21.1 build; this plugin only adds the
 * fluid-transmutation category that JEI cannot infer on its own.
 */
@JeiPlugin
public final class RiftGunJeiPlugin implements IModPlugin {
    private static final Identifier UID = id("jei_plugin");
    private static final RecipeType<RecipeHolder<FluidTransmutationRecipe>> TRANSMUTATION =
        RecipeType.createRecipeHolderType(id("fluid_transmutation"));

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(RiftConstants.MOD_ID, path);
    }

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FluidTransmutationCategory(
            registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager manager = recipeManager();
        if (manager == null) return;
        registerTransmutationRecipes(registration, fluidRecipes(manager));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Items.WATER_BUCKET, TRANSMUTATION);
    }

    // Package-visible seam so the recipe registration is testable without a client level.
    static void registerTransmutationRecipes(IRecipeRegistration registration,
                                             List<RecipeHolder<FluidTransmutationRecipe>> recipes) {
        if (!recipes.isEmpty()) registration.addRecipes(TRANSMUTATION, recipes);
    }

    private static RecipeManager recipeManager() {
        // The 26.1.2 client no longer carries a full recipe map; the integrated
        // server's manager is authoritative in singleplayer. Multiplayer still
        // needs a server -> client recipe sync, which JEI 26 performs itself.
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        return server == null ? null : server.getRecipeManager();
    }

    private static List<RecipeHolder<FluidTransmutationRecipe>> fluidRecipes(RecipeManager manager) {
        return manager.getRecipes().stream()
            .filter(holder -> holder.value() instanceof FluidTransmutationRecipe)
            .map(holder -> (RecipeHolder<FluidTransmutationRecipe>) (RecipeHolder<?>) holder)
            .toList();
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
        public IRecipeType<RecipeHolder<FluidTransmutationRecipe>> getRecipeType() {
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
        public Identifier getIdentifier(RecipeHolder<FluidTransmutationRecipe> recipe) {
            return recipe.id().identifier();
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder,
                              RecipeHolder<FluidTransmutationRecipe> holder,
                              IFocusGroup focuses) {
            builder.addInputSlot(2, 7)
                .addIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(Fluids.WATER, 1000))
                .setFluidRenderer(1000, true, 16, 40)
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(
                    Component.translatable("jei.riftgun.fluid_transmutation.water_source")));

            var ingredients = holder.value().ingredients();
            for (int index = 0; index < ingredients.size(); index++) {
                int x = 30 + (index % 2) * 18;
                int y = 2 + (index / 2) * 18;
                builder.addInputSlot(x, y)
                    .addItemStacks(ingredients.get(index).ingredient().items()
                        .map(holder2 -> new ItemStack(holder2.value()))
                        .toList())
                    .setStandardSlotBackground();
            }

            builder.addOutputSlot(112, 7)
                .addIngredient(NeoForgeTypes.FLUID_STACK, holder.value().result())
                .setFluidRenderer(1000, true, 16, 40);
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder extras,
                                       RecipeHolder<FluidTransmutationRecipe> recipe,
                                       IFocusGroup focuses) {
            extras.addDrawable(arrow, 78, 20);
        }
    }
}
