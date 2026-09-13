package dev.riftgun.appearance.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.riftgun.appearance.PortalGunSkin;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

/** Prepared during model discovery and published only after the corresponding models bake. */
public final class PortalGunSkinCatalog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "portal_gun_skins/";
    private static volatile Map<String, PortalGunSkinDefinition> available =
        Map.of(PortalGunSkin.DEFAULT, PortalGunSkinDefinition.DEFAULT);

    public static Map<String, PortalGunSkinDefinition> load(ResourceManager resources) {
        return load(resources, ignored -> {});
    }

    public static Map<String, PortalGunSkinDefinition> load(ResourceManager resources,
                                                           Consumer<JsonObject> modelValidator) {
        Map<String, PortalGunSkinDefinition> definitions = new LinkedHashMap<>();
        definitions.put(PortalGunSkin.DEFAULT, PortalGunSkinDefinition.DEFAULT);
        resources.listResources("portal_gun_skins", path -> path.getPath().endsWith(".json"))
            .forEach((location, resource) -> {
                String path = location.getPath();
                String id = location.getNamespace() + ":" + path.substring(DIRECTORY.length(), path.length() - 5);
                try (Reader reader = resource.openAsReader()) {
                    definitions.put(id, PortalGunSkinDefinition.parse(id,
                        JsonParser.parseReader(reader).getAsJsonObject()));
                } catch (Exception exception) {
                    LOGGER.warn("Ignoring invalid portal gun skin {}", id, exception);
                }
            });
        Map<String, Resource> models = new HashMap<>();
        resources.listResources("models", path -> path.getPath().endsWith(".json"))
            .forEach((location, resource) -> models.put(location.toString(), resource));
        definitions.entrySet().removeIf(entry -> {
            try {
                validateModel(entry.getValue().model(), models, new HashSet<>(), modelValidator);
                return false;
            } catch (Exception exception) {
                failed(entry.getKey(), exception);
                return true;
            }
        });
        definitions.putIfAbsent(PortalGunSkin.DEFAULT, PortalGunSkinDefinition.DEFAULT);
        return Map.copyOf(definitions);
    }

    private static void validateModel(String model, Map<String, Resource> models, Set<String> chain,
                                      Consumer<JsonObject> validator)
            throws java.io.IOException {
        if (!model.contains(":")) model = "minecraft:" + model;
        if (model.equals("minecraft:builtin/generated") || model.equals("minecraft:builtin/entity")) return;
        if (!chain.add(model)) throw new IllegalArgumentException("Cyclic model parent: " + model);
        int colon = model.indexOf(':');
        String file = model.substring(0, colon) + ":models/" + model.substring(colon + 1) + ".json";
        Resource resource = models.get(file);
        if (resource == null) throw new IllegalArgumentException("Missing model: " + model);
        try (Reader reader = resource.openAsReader()) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            validator.accept(json);
            if (json.has("parent")) validateModel(json.get("parent").getAsString(), models, chain, validator);
        }
    }

    public static void install(Map<String, PortalGunSkinDefinition> definitions) {
        available = Map.copyOf(definitions);
    }

    public static Collection<PortalGunSkinDefinition> all() { return available.values(); }
    /** Immutable reload snapshot; identity changes only when a baked catalog is published. */
    public static Map<String, PortalGunSkinDefinition> snapshot() { return available; }
    public static boolean contains(String id) { return id != null && available.containsKey(id); }
    public static PortalGunSkinDefinition resolve(String id) {
        return available.getOrDefault(id == null ? PortalGunSkin.DEFAULT : id, available.getOrDefault(PortalGunSkin.DEFAULT,
            PortalGunSkinDefinition.DEFAULT));
    }

    public static void failed(String id, Exception exception) {
        LOGGER.warn("Cannot bake portal gun skin {}; using default appearance", id, exception);
    }

    private PortalGunSkinCatalog() {}
}
