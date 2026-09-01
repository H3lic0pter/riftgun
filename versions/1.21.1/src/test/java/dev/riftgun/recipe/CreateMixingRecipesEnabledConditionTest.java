package dev.riftgun.recipe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.neoforged.neoforge.common.conditions.ICondition;
import org.junit.jupiter.api.Test;

final class CreateMixingRecipesEnabledConditionTest {
    @Test
    void conditionIsSafeBeforeNeoForgeLoadsConfigs() {
        boolean enabled = assertDoesNotThrow(() ->
            CreateMixingRecipesEnabledCondition.INSTANCE.test(ICondition.IContext.EMPTY));

        assertTrue(enabled);
    }
}
