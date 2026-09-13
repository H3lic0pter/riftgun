package dev.riftgun.client.appearance;

import com.mojang.serialization.MapCodec;
import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.appearance.client.PortalGunSkinDefinition;
import dev.riftgun.appearance.client.PortalGunSkinCatalog;
import dev.riftgun.client.model.PortalGunLayeredModel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.cuboid.MissingCuboidModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/** All item display contexts resolve the same stored identity and reload-scoped model map. */
public record PortalGunAppearanceModel(Map<String, ItemModel> skins) implements ItemModel {
    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                       ItemDisplayContext context, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
        ItemModel model = skins.getOrDefault(PortalGunSkin.current(item), skins.get(PortalGunSkin.DEFAULT));
        model.update(output, item, resolver, context, level, owner, seed);
    }

    public static final class Unbaked implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);
        private Map<String, PortalGunSkinDefinition> definitions = Map.of();

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            definitions = PortalGunSkinCatalog.load(Minecraft.getInstance().getResourceManager());
            resolver.markDependency(Identifier.parse(PortalGunSkinDefinition.DEFAULT.model()));
            for (var definition : definitions.values()) {
                resolver.markDependency(Identifier.parse(definition.model()));
            }
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            Map<String, ItemModel> skins = new LinkedHashMap<>();
            Map<String, PortalGunSkinDefinition> available = new LinkedHashMap<>();
            skins.put(PortalGunSkin.DEFAULT, leaf(PortalGunSkinDefinition.DEFAULT, context, transformation));
            available.put(PortalGunSkin.DEFAULT, PortalGunSkinDefinition.DEFAULT);
            var missing = context.blockModelBaker().getModel(MissingCuboidModel.LOCATION);
            for (var definition : definitions.values()) {
                try {
                    var model = context.blockModelBaker().getModel(Identifier.parse(definition.model()));
                    for (var parent = model; parent != null; parent = parent.parent()) {
                        if (parent == missing) throw new IllegalArgumentException("Missing model " + definition.model());
                    }
                    skins.put(definition.id(), leaf(definition, context, transformation));
                    available.put(definition.id(), definition);
                } catch (Exception exception) {
                    PortalGunSkinCatalog.failed(definition.id(), exception);
                }
            }
            PortalGunSkinCatalog.install(available);
            return new PortalGunAppearanceModel(Map.copyOf(skins));
        }

        private static ItemModel leaf(PortalGunSkinDefinition definition,
                                      ItemModel.BakingContext context, Matrix4fc transformation) {
            Identifier model = Identifier.parse(definition.model());
            if (!definition.layered()) {
                return new CuboidItemModelWrapper.Unbaked(model, Optional.empty(), List.of())
                    .bake(context, transformation);
            }
            PortalGunLayeredModel baked = (PortalGunLayeredModel)
                new PortalGunLayeredModel.Unbaked(model, Optional.empty()).bake(context, transformation);
            return new PortalGunLayeredModel(baked.variants(), baked.extents(), baked.properties(),
                baked.transformation(), definition);
        }

        @Override
        public MapCodec<Unbaked> type() { return MAP_CODEC; }
    }
}
