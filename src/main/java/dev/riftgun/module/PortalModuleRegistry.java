package dev.riftgun.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public final class PortalModuleRegistry {
    private static final List<PortalModuleDefinition> DEFINITIONS = new ArrayList<>();

    public static PortalModuleDefinition register(PortalModuleDefinition definition) {
        if (DEFINITIONS.stream().anyMatch(existing -> existing.id().equals(definition.id()))) {
            throw new IllegalArgumentException("Duplicate portal module: " + definition.id());
        }
        DEFINITIONS.add(definition);
        return definition;
    }

    public static List<PortalModuleDefinition> definitions() {
        return Collections.unmodifiableList(DEFINITIONS);
    }

    public static Optional<PortalModuleDefinition> find(ItemStack stack) {
        return DEFINITIONS.stream().filter(definition -> definition.matches(stack)).findFirst();
    }

    public static Optional<PortalModuleDefinition> find(PortalModuleKind kind) {
        return DEFINITIONS.stream().filter(definition -> definition.kind() == kind).findFirst();
    }

    public static boolean isModule(ItemStack stack) {
        return find(stack).isPresent();
    }

    private PortalModuleRegistry() {}
}
