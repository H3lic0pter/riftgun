package dev.riftgun.fuel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import dev.riftgun.core.visual.PortalGunVisualSnapshot;

/** Synchronized, derived state used by the Portal Gun item renderer. */
public record PortalGunVisualState(int liquidTint, boolean coreVisible, int fuelRgb) {
    public static final int UNINITIALIZED_TINT = -1;
    public static final PortalGunVisualState UNINITIALIZED =
        new PortalGunVisualState(UNINITIALIZED_TINT, false, PortalFuelProfiles.DIMENSIONAL_RGB);
    public static final Codec<PortalGunVisualState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("liquid_tint", UNINITIALIZED_TINT)
            .forGetter(PortalGunVisualState::liquidTint),
        Codec.BOOL.optionalFieldOf("core_visible", false)
            .forGetter(PortalGunVisualState::coreVisible),
        Codec.INT.optionalFieldOf("fuel_rgb", PortalFuelProfiles.DIMENSIONAL_RGB)
            .forGetter(PortalGunVisualState::fuelRgb)
    ).apply(instance, PortalGunVisualState::new));
    public static final StreamCodec<ByteBuf, PortalGunVisualState> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, PortalGunVisualState::liquidTint,
        ByteBufCodecs.BOOL, PortalGunVisualState::coreVisible,
        ByteBufCodecs.VAR_INT, PortalGunVisualState::fuelRgb,
        PortalGunVisualState::new
    );

    public PortalGunVisualState {
        if (liquidTint != UNINITIALIZED_TINT && liquidTint != 0
            && !isLiquidTint(liquidTint)) liquidTint = 0;
        fuelRgb &= 0xFFFFFF;
    }

    public boolean initialized() {
        return liquidTint != UNINITIALIZED_TINT;
    }

    /** Indexes eight liquid states times two core states. */
    public int geometryKey() {
        return PortalGunVisualSnapshot.geometryKey(liquidTint, coreVisible);
    }

    public PortalGunVisualSnapshot snapshot() {
        return PortalGunVisualSnapshot.create(liquidTint, coreVisible, fuelRgb);
    }

    public static PortalGunVisualState current(ItemStack gun) {
        PortalGunVisualState stored = gun.get(PortalGunComponents.VISUAL_STATE);
        if (stored != null && stored.initialized()) return stored;
        PortalGunVisualState derived = derive(gun);
        gun.set(PortalGunComponents.VISUAL_STATE, derived);
        return derived;
    }

    public static void refresh(ItemStack gun) {
        PortalGunVisualState derived = derive(gun);
        if (!derived.equals(gun.get(PortalGunComponents.VISUAL_STATE))) {
            gun.set(PortalGunComponents.VISUAL_STATE, derived);
        }
    }

    public static void ensureInitialized(ItemStack gun) {
        PortalGunVisualState stored = gun.get(PortalGunComponents.VISUAL_STATE);
        if (stored == null || !stored.initialized()) refresh(gun);
    }

    public static int liquidTintIndex(int amount, int nominalCapacity) {
        if (amount <= 0 || nominalCapacity <= 0) return 0;
        double ratio = (double) amount / nominalCapacity;
        if (ratio >= 0.95) return 2;
        if (ratio >= 0.80) return 3;
        if (ratio >= 0.60) return 4;
        if (ratio >= 0.40) return 5;
        if (ratio >= 0.20) return 6;
        if (ratio >= 0.05) return 7;
        return 8;
    }

    public static boolean isLiquidTint(int tintIndex) {
        return tintIndex >= 2 && tintIndex <= 8;
    }

    private static PortalGunVisualState derive(ItemStack gun) {
        PortalGunTank tank = new PortalGunTank(gun);
        var fluid = tank.getFluid();
        int tint = fluid.isEmpty() ? 0 : liquidTintIndex(fluid.getAmount(), tank.nominalCapacity());
        int rgb = PortalFuelProfiles.resolve(fluid)
            .map(PortalFuelProfile::rgb).orElse(PortalFuelProfiles.DIMENSIONAL_RGB);
        return new PortalGunVisualState(tint, PortalFuelManager.hasInfiniteFuel(gun), rgb);
    }
}
