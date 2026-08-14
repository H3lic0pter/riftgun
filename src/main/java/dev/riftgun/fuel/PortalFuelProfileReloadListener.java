package dev.riftgun.fuel;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import org.slf4j.Logger;

/** Builds the immutable datapack fuel index after fluid tags are available. */
public final class PortalFuelProfileReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private final RegistryAccess registries;

    public PortalFuelProfileReloadListener(RegistryAccess registries) {
        super(GSON, "riftgun/portal_fuels");
        this.registries = registries;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager manager, ProfilerFiller profiler) {
        Registry<Fluid> fluids = registries.registryOrThrow(Registries.FLUID);
        Map<Fluid, PortalFuelProfile> index = new HashMap<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                PortalFuelDefinition definition = PortalFuelDefinition.parse(
                    entry.getKey(), entry.getValue().getAsJsonObject());
                if (definition.fluid() != null) {
                    Fluid fluid = fluids.get(definition.fluid());
                    if (fluid == null) {
                        LOGGER.warn("Ignoring portal fuel {}: unknown fluid {}",
                            definition.id(), definition.fluid());
                    } else {
                        add(index, fluid, definition);
                    }
                } else {
                    TagKey<Fluid> tag = TagKey.create(Registries.FLUID, definition.tag());
                    fluids.getTagOrEmpty(tag).forEach(holder -> add(index, holder.value(), definition));
                }
            } catch (RuntimeException exception) {
                LOGGER.error("Ignoring invalid portal fuel definition {}", entry.getKey(), exception);
            }
        });
        PortalFuelProfiles.installDataProfiles(index);
        LOGGER.info("Loaded {} datapack portal fuel mappings", index.size());
    }

    private static void add(Map<Fluid, PortalFuelProfile> index, Fluid fluid,
                            PortalFuelDefinition definition) {
        PortalFuelProfile previous = index.putIfAbsent(fluid, definition.profile());
        if (previous != null) {
            LOGGER.error("Portal fuel selector conflict for {}: keeping {}, ignoring {}",
                fluid, previous.id(), definition.id());
        }
    }
}
