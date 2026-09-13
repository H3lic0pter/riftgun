package dev.riftgun.client.appearance;

import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.appearance.client.PortalGunSkinDefinition;
import dev.riftgun.appearance.client.PortalGunSkinCatalog;
import dev.riftgun.client.model.PortalGunLayeredModel;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

/** Selects a baked skin before the existing dynamic geometry selection runs. */
public final class PortalGunAppearanceModel extends BakedModelWrapper<BakedModel> {
    private static Map<String, PortalGunSkinDefinition> prepared = Map.of();
    private final Map<String, BakedModel> skins;
    private final ItemOverrides overrides = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel original, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            BakedModel model = skins.getOrDefault(PortalGunSkin.current(stack), originalModel);
            BakedModel resolved = model.getOverrides().resolve(model, stack, level, entity, seed);
            return resolved == null ? model : resolved;
        }
    };

    private PortalGunAppearanceModel(BakedModel fallback, Map<String, BakedModel> skins) {
        super(fallback);
        this.skins = Map.copyOf(skins);
    }

    public static void register(ModelEvent.RegisterAdditional event) {
        // 1.21.1 can bake a separate missing-model instance, so identity checks after baking are insufficient.
        prepared = PortalGunSkinCatalog.load(Minecraft.getInstance().getResourceManager(), json ->
            net.minecraft.client.renderer.block.model.BlockModel.fromStream(new java.io.StringReader(json.toString())));
        for (var definition : prepared.values()) {
            event.register(ModelResourceLocation.standalone(ResourceLocation.parse(definition.model())));
        }
    }

    public static BakedModel bake(BakedModel original, Map<ModelResourceLocation, BakedModel> models) {
        BakedModel fallback = new PortalGunLayeredModel(original);
        Map<String, BakedModel> baked = new LinkedHashMap<>();
        Map<String, PortalGunSkinDefinition> available = new LinkedHashMap<>();
        baked.put(PortalGunSkin.DEFAULT, fallback);
        available.put(PortalGunSkin.DEFAULT, PortalGunSkinDefinition.DEFAULT);
        BakedModel missing = models.get(net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_VARIANT);
        for (var definition : prepared.values()) {
            BakedModel model = models.get(ModelResourceLocation.standalone(ResourceLocation.parse(definition.model())));
            if (model == null || model == missing) {
                PortalGunSkinCatalog.failed(definition.id(), new IllegalArgumentException("Missing model " + definition.model()));
                continue;
            }
            baked.put(definition.id(), definition.layered()
                ? new PortalGunLayeredModel(model, definition) : model);
            available.put(definition.id(), definition);
        }
        PortalGunSkinCatalog.install(available);
        return new PortalGunAppearanceModel(baked.get(PortalGunSkin.DEFAULT), baked);
    }

    @Override
    public ItemOverrides getOverrides() { return overrides; }
}
