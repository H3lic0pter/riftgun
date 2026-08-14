package dev.riftgun.core.registry;

import net.minecraft.resources.ResourceLocation;

/** Loader-neutral reference to one registered Minecraft value. */
public interface RegistryRef<T> {
    ResourceLocation id();

    T get();
}
