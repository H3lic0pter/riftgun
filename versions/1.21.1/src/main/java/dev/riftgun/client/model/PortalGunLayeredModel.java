package dev.riftgun.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riftgun.client.render.PortalRenderFrameState;
import dev.riftgun.fuel.PortalGunVisualState;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Selects one of sixteen prefiltered Portal Gun models from synchronized visual state. */
public final class PortalGunLayeredModel extends BakedModelWrapper<BakedModel> {
    private static final Comparator<BakedQuad> LAYER_ORDER = Comparator.comparingInt(
        quad -> PortalGunModelLayers.renderLayer(quad.getTintIndex()).ordinal());

    private final BakedModel[] legacyVariants =
        new BakedModel[PortalGunModelLayers.VARIANT_COUNT];
    private final BakedModel[] shaderVariants =
        new BakedModel[PortalGunModelLayers.VARIANT_COUNT];
    private final ItemOverrides overrides = new VisualOverrides();

    public PortalGunLayeredModel(BakedModel originalModel) {
        super(originalModel);
        for (int key = 0; key < legacyVariants.length; key++) {
            CachedGeometry geometry = new CachedGeometry(originalModel, key);
            legacyVariants[key] = new LegacyVariant(originalModel, geometry);
            shaderVariants[key] = new ShaderVariant(originalModel, geometry);
        }
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    private final class VisualOverrides extends ItemOverrides {
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            BakedModel resolved = originalModel.getOverrides()
                .resolve(originalModel, stack, level, entity, seed);
            if (resolved != originalModel) return resolved;
            int geometryKey = PortalGunVisualState.current(stack).geometryKey();
            return PortalRenderFrameState.current().shaderPackActive()
                ? shaderVariants[geometryKey]
                : legacyVariants[geometryKey];
        }
    }

    private static final class CachedGeometry {
        private final EnumMap<Direction, List<BakedQuad>> directional =
            new EnumMap<>(Direction.class);
        private final List<BakedQuad> unculled;

        private CachedGeometry(BakedModel originalModel, int geometryKey) {
            for (Direction direction : Direction.values()) {
                directional.put(direction, filtered(originalModel, direction, geometryKey));
            }
            unculled = filtered(originalModel, null, geometryKey);
        }

        private List<BakedQuad> quads(@Nullable Direction side) {
            return side == null ? unculled : directional.get(side);
        }

        private static List<BakedQuad> filtered(BakedModel model, @Nullable Direction side,
                                                int geometryKey) {
            RandomSource random = RandomSource.create(42L);
            return model.getQuads(null, side, random, ModelData.EMPTY, null).stream()
                .filter(quad -> PortalGunModelLayers.includesTint(geometryKey, quad.getTintIndex()))
                .toList();
        }
    }

    private static final class LegacyVariant extends BakedModelWrapper<BakedModel> {
        private final CachedGeometry geometry;
        private final List<BakedModel> renderPasses = List.of(this);

        private LegacyVariant(BakedModel originalModel, CachedGeometry geometry) {
            super(originalModel);
            this.geometry = geometry;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                        RandomSource random) {
            return geometry.quads(side);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                        RandomSource random, ModelData data,
                                        @Nullable RenderType renderType) {
            return getQuads(state, side, random);
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poses, boolean leftHand) {
            originalModel.applyTransform(context, poses, leftHand);
            return this;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return renderPasses;
        }

    }

    private static final class ShaderVariant extends BakedModelWrapper<BakedModel> {
        private final List<BakedModel> renderPasses;

        private ShaderVariant(BakedModel originalModel, CachedGeometry geometry) {
            super(originalModel);
            renderPasses = List.of(
                new Layer(originalModel, geometry, PortalGunModelLayers.RenderPass.OPAQUE,
                    PortalGunRenderTypes.opaque()),
                new Layer(originalModel, geometry, PortalGunModelLayers.RenderPass.TRANSLUCENT,
                    PortalGunRenderTypes.translucentLayer())
            );
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poses,
                                         boolean leftHand) {
            originalModel.applyTransform(context, poses, leftHand);
            return this;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return renderPasses;
        }
    }

    private static final class Layer extends BakedModelWrapper<BakedModel> {
        private final EnumMap<Direction, List<BakedQuad>> directional =
            new EnumMap<>(Direction.class);
        private final List<BakedQuad> unculled;
        private final List<RenderType> renderTypes;

        private Layer(BakedModel originalModel, CachedGeometry geometry,
                      PortalGunModelLayers.RenderPass pass, RenderType renderType) {
            super(originalModel);
            for (Direction direction : Direction.values()) {
                directional.put(direction, filtered(geometry.quads(direction), pass));
            }
            unculled = filtered(geometry.quads(null), pass);
            renderTypes = List.of(renderType);
        }

        private static List<BakedQuad> filtered(List<BakedQuad> quads,
                                                PortalGunModelLayers.RenderPass pass) {
            return quads.stream()
                .filter(quad -> PortalGunModelLayers.renderPass(quad.getTintIndex()) == pass)
                .sorted(LAYER_ORDER)
                .toList();
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                        RandomSource random) {
            return side == null ? unculled : directional.get(side);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                        RandomSource random, ModelData data,
                                        @Nullable RenderType renderType) {
            return getQuads(state, side, random);
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return renderTypes;
        }
    }
}
