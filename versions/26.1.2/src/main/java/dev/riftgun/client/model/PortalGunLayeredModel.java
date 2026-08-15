package dev.riftgun.client.model;

import com.google.common.base.Suppliers;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riftgun.fuel.PortalGunVisualState;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * Selects one of sixteen prefiltered Portal Gun quad sets from synchronized visual state.
 *
 * <p>The canonical model is baked once, then split by tint index into the same variants the
 * 1.21.x line computed in {@code BakedModelWrapper} subclasses. {@link #update} fills the
 * layer's tint slots (tint index 1 is the untinted glass, 2-8 the liquid columns, 9/10 the
 * zero-point core) with the per-slot ARGB the synchronized state derives; the renderer looks
 * each quad's tint index up in that slot list, where {@code -1} leaves the quad untinted,
 * {@code 0} makes it fully transparent and any other value tints it.
 */
public record PortalGunLayeredModel(
    List<QuadCollection> variants,
    List<Supplier<Vector3fc[]>> extents,
    ModelRenderProperties properties,
    Matrix4fc transformation
) implements ItemModel {

    @Override
    public void update(
        ItemStackRenderState output,
        ItemStack item,
        ItemModelResolver resolver,
        ItemDisplayContext displayContext,
        @Nullable ClientLevel level,
        @Nullable ItemOwner owner,
        int seed
    ) {
        output.appendModelIdentityElement(this);
        PortalGunVisualState visual = PortalGunVisualState.current(item);
        int geometryKey = visual.geometryKey();
        QuadCollection quads = this.variants.get(geometryKey);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        if (item.hasFoil()) {
            layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
            output.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
        }

        IntList tintLayers = layer.tintLayers();
        var snapshot = visual.snapshot();
        for (int tint = 0; tint <= PortalGunModelLayers.INNER_CORE_TINT; tint++) {
            tintLayers.add(snapshot.color(tint));
        }

        layer.setExtents(this.extents.get(geometryKey));
        layer.setLocalTransform(this.transformation);
        this.properties.applyToLayer(layer, displayContext);
        layer.prepareQuadList().addAll(quads.getAll());
        if (quads.hasMaterialFlag(BakedQuad.FLAG_ANIMATED)) {
            output.setAnimated();
        }
    }

    public record Unbaked(Identifier model, Optional<Transformation> transformation)
            implements ItemModel.Unbaked {
        public static final MapCodec<PortalGunLayeredModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Identifier.CODEC.fieldOf("model").forGetter(PortalGunLayeredModel.Unbaked::model),
                    Transformation.EXTENDED_CODEC.optionalFieldOf("transformation")
                        .forGetter(PortalGunLayeredModel.Unbaked::transformation)
                )
                .apply(i, PortalGunLayeredModel.Unbaked::new)
        );

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(this.model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel resolvedModel = baker.getModel(this.model);
            TextureSlots textureSlots = resolvedModel.getTopTextureSlots();
            QuadCollection quads = resolvedModel.bakeTopGeometry(textureSlots, baker, BlockModelRotation.IDENTITY);
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolvedModel, textureSlots);
            Matrix4fc modelTransform = Transformation.compose(transformation, this.transformation);
            List<QuadCollection> variants = new ArrayList<>(PortalGunModelLayers.VARIANT_COUNT);
            List<Supplier<Vector3fc[]>> extents = new ArrayList<>(PortalGunModelLayers.VARIANT_COUNT);
            for (int key = 0; key < PortalGunModelLayers.VARIANT_COUNT; key++) {
                QuadCollection.Builder builder = new QuadCollection.Builder();
                for (BakedQuad quad : quads.getAll()) {
                    if (PortalGunModelLayers.includesTint(key, quad.materialInfo().tintIndex())) {
                        builder.addCulledFace(quad.direction(), quad);
                    }
                }
                QuadCollection variant = builder.build();
                variants.add(variant);
                extents.add(Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(variant.getAll())));
            }
            return new PortalGunLayeredModel(variants, extents, properties, modelTransform);
        }

        @Override
        public MapCodec<PortalGunLayeredModel.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
