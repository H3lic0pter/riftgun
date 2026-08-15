package dev.riftgun.core.registry;

import java.util.Objects;
import java.util.function.Supplier;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/** Once-installed registry reference used by the platform bootstrap. */
final class RegistrySlot<T> implements RegistryRef<T> {
//? if >=1.21.11 {
    /*private final Identifier id;
*///?} else {
    private final ResourceLocation id;
//?}
    private volatile Supplier<? extends T> supplier;

//? if >=1.21.11 {
    /*RegistrySlot(Identifier id) {
*///?} else {
    RegistrySlot(ResourceLocation id) {
//?}
        this.id = Objects.requireNonNull(id, "id");
    }

    synchronized void install(Supplier<? extends T> supplier) {
        if (this.supplier != null) throw new IllegalStateException("registry ref already installed: " + id);
        this.supplier = Objects.requireNonNull(supplier, "supplier");
    }

    @Override
//? if >=1.21.11 {
    /*public Identifier id() {
*///?} else {
    public ResourceLocation id() {
//?}
        return id;
    }

    @Override
    public T get() {
        Supplier<? extends T> installed = supplier;
        if (installed == null) throw new IllegalStateException("registry ref not installed: " + id);
        return Objects.requireNonNull(installed.get(), () -> "registered value unavailable: " + id);
    }
}
