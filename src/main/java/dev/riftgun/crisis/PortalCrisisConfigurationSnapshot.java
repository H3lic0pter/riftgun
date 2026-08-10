package dev.riftgun.crisis;

import com.mojang.logging.LogUtils;
import dev.riftgun.config.ServerConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import org.slf4j.Logger;

/** Weight and instability snapshot captured once when a portal opens. */
public record PortalCrisisConfigurationSnapshot(boolean unstable, Map<ResourceLocation, Integer> weights) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public PortalCrisisConfigurationSnapshot {
        weights = Map.copyOf(weights);
    }

    public static PortalCrisisConfigurationSnapshot capture(FluidStack fluid) {
        if (!PortalFluidInstability.isUnstable(fluid)) return stable();
        return new PortalCrisisConfigurationSnapshot(true, configuredWeights());
    }

    public static PortalCrisisConfigurationSnapshot stable() {
        return new PortalCrisisConfigurationSnapshot(false, Map.of());
    }

    public int weight(ResourceLocation id) {
        return weights.getOrDefault(id, 0);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Unstable", unstable);
        ListTag entries = new ListTag();
        weights.forEach((id, weight) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", id.toString());
            entry.putInt("Weight", weight);
            entries.add(entry);
        });
        tag.put("Weights", entries);
        return tag;
    }

    public static PortalCrisisConfigurationSnapshot load(CompoundTag tag) {
        if (!tag.getBoolean("Unstable")) return stable();
        Map<ResourceLocation, Integer> weights = new LinkedHashMap<>();
        for (Tag raw : tag.getList("Weights", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Id"));
            int weight = entry.getInt("Weight");
            if (id != null && weight >= 0 && weight <= PortalCrisisEngine.TOTAL_WEIGHT) {
                weights.put(id, weight);
            }
        }
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        return total <= PortalCrisisEngine.TOTAL_WEIGHT
            ? new PortalCrisisConfigurationSnapshot(true, weights)
            : new PortalCrisisConfigurationSnapshot(true, PortalCrisisRegistry.defaultWeights());
    }

    private static Map<ResourceLocation, Integer> configuredWeights() {
        Map<ResourceLocation, Integer> defaults = PortalCrisisRegistry.defaultWeights();
        Map<ResourceLocation, Integer> configured = new LinkedHashMap<>(defaults);
        List<? extends String> entries = ServerConfig.VALUES.crisisWeights.get();
        for (String entry : entries) {
            int separator = entry.lastIndexOf('=');
            if (separator <= 0) continue;
            ResourceLocation id = ResourceLocation.tryParse(entry.substring(0, separator));
            if (id == null || !configured.containsKey(id)) {
                LOGGER.warn("Ignoring unknown portal crisis weight entry: {}", entry);
                continue;
            }
            try {
                configured.put(id, Integer.parseInt(entry.substring(separator + 1)));
            } catch (NumberFormatException ignored) {
                LOGGER.warn("Ignoring malformed portal crisis weight entry: {}", entry);
            }
        }
        int total = configured.values().stream().mapToInt(Integer::intValue).sum();
        if (total > PortalCrisisEngine.TOTAL_WEIGHT) {
            LOGGER.error("Portal crisis weights total {} (>1000); using registered defaults", total);
            return defaults;
        }
        return configured;
    }
}
