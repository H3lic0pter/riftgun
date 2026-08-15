package dev.riftgun.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riftgun.fuel.PortalGunVisualState;
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
    private final BakedModel[] variants = new BakedModel[PortalGunModelLayers.VARIANT_COUNT];
    private final ItemOverrides overrides = new VisualOverrides();

    public PortalGunLayeredModel(BakedModel originalModel) {
        super(originalModel);
        for (int key = 0; key < variants.length; key++) {
            variants[key] = new Variant(originalModel, key);
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
            return variants[PortalGunVisualState.current(stack).geometryKey()];
        }
    }

    private static final class Variant extends BakedModelWrapper<BakedModel> {
        private final EnumMap<Direction, List<BakedQuad>> directional = new EnumMap<>(Direction.class);
        private final List<BakedQuad> unculled;

        private Variant(BakedModel originalModel, int geometryKey) {
            super(originalModel);
            for (Direction direction : Direction.values()) {
                directional.put(direction, filtered(originalModel, direction, geometryKey));
            }
            unculled = filtered(originalModel, null, geometryKey);
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
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poses, boolean leftHand) {
            originalModel.applyTransform(context, poses, leftHand);
            return this;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }

        private static List<BakedQuad> filtered(BakedModel model, @Nullable Direction side,
                                                int geometryKey) {
            RandomSource random = RandomSource.create(42L);
            return model.getQuads(null, side, random, ModelData.EMPTY, null).stream()
                .filter(quad -> PortalGunModelLayers.includesTint(geometryKey, quad.getTintIndex()))
                .toList();
        }
    }
}
