package dev.riftgun.crisis;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.core.config.RiftConfigs;
import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.world.level.material.Fluid;
import org.slf4j.Logger;

/** Weight and instability snapshot captured once when a portal opens. */
//? if >=1.21.11 {
/*public record PortalCrisisConfigurationSnapshot(boolean unstable, Map<Identifier, Integer> weights) {
*///?} else {
public record PortalCrisisConfigurationSnapshot(boolean unstable, Map<ResourceLocation, Integer> weights) {
//?}
    private static final Logger LOGGER = LogUtils.getLogger();

    public PortalCrisisConfigurationSnapshot {
        weights = Map.copyOf(weights);
    }

    public static PortalCrisisConfigurationSnapshot capture(Fluid fluid) {
        if (!PortalFluidInstability.isUnstable(fluid)) return stable();
        return new PortalCrisisConfigurationSnapshot(true, configuredWeights());
    }

    public static PortalCrisisConfigurationSnapshot stable() {
        return new PortalCrisisConfigurationSnapshot(false, Map.of());
    }

//? if >=1.21.11 {
    /*public int weight(Identifier id) {
*///?} else {
    public int weight(ResourceLocation id) {
//?}
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
        if (!Nbt.getBoolean(tag, "Unstable")) return stable();
//? if >=1.21.11 {
        /*Map<Identifier, Integer> weights = new LinkedHashMap<>();
*///?} else {
        Map<ResourceLocation, Integer> weights = new LinkedHashMap<>();
//?}
        for (Tag raw : Nbt.getList(tag, "Weights")) {
            CompoundTag entry = (CompoundTag) raw;
//? if >=1.21.11 {
            /*Identifier id = Identifier.tryParse(Nbt.getString(entry, "Id"));
*///?} else {
            ResourceLocation id = ResourceLocation.tryParse(Nbt.getString(entry, "Id"));
//?}
            int weight = Nbt.getInt(entry, "Weight");
            if (id != null && weight >= 0 && weight <= PortalCrisisEngine.TOTAL_WEIGHT) {
                weights.put(id, weight);
            }
        }
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        return total <= PortalCrisisEngine.TOTAL_WEIGHT
            ? new PortalCrisisConfigurationSnapshot(true, weights)
            : new PortalCrisisConfigurationSnapshot(true, PortalCrisisRegistry.defaultWeights());
    }

//? if >=1.21.11 {
    /*private static Map<Identifier, Integer> configuredWeights() {
*///?} else {
    private static Map<ResourceLocation, Integer> configuredWeights() {
//?}
//? if >=1.21.11 {
        /*Map<Identifier, Integer> defaults = PortalCrisisRegistry.defaultWeights();
*///?} else {
        Map<ResourceLocation, Integer> defaults = PortalCrisisRegistry.defaultWeights();
//?}
//? if >=1.21.11 {
        /*Map<Identifier, Integer> configured = new LinkedHashMap<>(defaults);
*///?} else {
        Map<ResourceLocation, Integer> configured = new LinkedHashMap<>(defaults);
//?}
        List<? extends String> entries = RiftConfigs.server().crises().weights();
        for (String entry : entries) {
            int separator = entry.lastIndexOf('=');
            if (separator <= 0) continue;
//? if >=1.21.11 {
            /*Identifier id = Identifier.tryParse(entry.substring(0, separator));
*///?} else {
            ResourceLocation id = ResourceLocation.tryParse(entry.substring(0, separator));
//?}
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
