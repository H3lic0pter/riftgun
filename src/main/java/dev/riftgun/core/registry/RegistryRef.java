package dev.riftgun.core.registry;

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/** Loader-neutral reference to one registered Minecraft value. */
public interface RegistryRef<T> {
//? if >=1.21.11 {
    /*Identifier id();
*///?} else {
    ResourceLocation id();
//?}

    T get();
}
