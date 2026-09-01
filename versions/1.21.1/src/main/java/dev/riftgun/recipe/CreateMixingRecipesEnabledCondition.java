package dev.riftgun.recipe;

import com.mojang.serialization.MapCodec;
import dev.riftgun.config.IntegrationConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

/** Load-time gate that keeps disabled Create recipes out of the recipe manager. */
public final class CreateMixingRecipesEnabledCondition implements ICondition {
    public static final CreateMixingRecipesEnabledCondition INSTANCE =
        new CreateMixingRecipesEnabledCondition();
    public static final MapCodec<CreateMixingRecipesEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    private CreateMixingRecipesEnabledCondition() {}

    @Override
    public boolean test(IContext context) {
        return IntegrationConfig.createMixingRecipesEnabled();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
