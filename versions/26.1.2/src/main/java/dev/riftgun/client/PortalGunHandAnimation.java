package dev.riftgun.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.riftgun.RiftGun;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.core.config.GunRecoilConfig;
import dev.riftgun.core.config.GunShotAnimation;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.core.visual.PortalGunRecoil;
import dev.riftgun.fuel.PortalGunMode;
import dev.riftgun.network.PortalAction;
import dev.riftgun.portal.PortalGunItem;
import dev.riftgun.service.PortalGunIdentity;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** First-person transforms only; item models, cached geometry and render passes remain native. */
@EventBusSubscriber(modid = RiftGun.MOD_ID, value = Dist.CLIENT)
public final class PortalGunHandAnimation implements IClientItemExtensions {
    // Vanilla ItemInHandRenderer's resting hand origin, used as a rotation pivot.
    private static final float HAND_X = 0.56F;
    private static final float HAND_Y = -0.52F;
    private static final float HAND_Z = -0.72F;
    private static final float EQUIP_DROP = 0.6F;
    private static final HandAnimation MAIN = new HandAnimation();
    private static final HandAnimation OFF = new HandAnimation();
    private static LocalPlayer owner;
    private static ClientLevel level;
    private static int selectedSlot = -1;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() == minecraft.player && minecraft.screen == null) {
            fire(minecraft, event.getHand(), true);
        }
    }

    /** Called only for dispatched open commands, including a committed precision placement. */
    public static void onRequest(PortalAction action, CompoundTag request) {
        if (!Nbt.getBoolean(request, "KeyboardShortcut") || !action.isShotShortcut()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        // Match the server's main-hand-first selection; a bucket-mode main gun must not
        // accidentally animate a different gun in the offhand.
        InteractionHand hand = minecraft.player.getMainHandItem().getItem() instanceof PortalGunItem
            ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        // Explicit radial references take priority over the normal shortcut lookup.
        if (request.contains("GunReference")) {
            CompoundTag reference = Nbt.getCompound(request, "GunReference");
            if (PortalGunIdentity.matches(minecraft.player.getMainHandItem(), reference)) {
                hand = InteractionHand.MAIN_HAND;
            } else if (PortalGunIdentity.matches(minecraft.player.getOffhandItem(), reference)) {
                hand = InteractionHand.OFF_HAND;
            } else {
                return;
            }
        }
        fire(minecraft, hand, false);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        validate(Minecraft.getInstance());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isCanceled()) return;
        if (event.isAttack()) {
            // Left-click attacks must retain their native swing, even just after firing.
            MAIN.clear();
            OFF.clear();
        } else if (event.isUseItem()) {
            // Blocks/entities may consume this input before RightClickItem fires.
            // Release their native swing now; a shot later in this same input will
            // reclaim the hand while retaining the current recoil envelope.
            animation(event.getHand()).release();
        }
    }

    private static void fire(Minecraft minecraft, InteractionHand hand, boolean itemUse) {
        if (!validate(minecraft) || minecraft.isPaused()) return;
        ItemStack stack = minecraft.player.getItemInHand(hand);
        if (!isPortalGun(stack)) return;
        var config = RiftConfigs.client();
        var mode = dev.riftgun.client.appearance.SkinRecommendations.animation(stack);
        boolean lowerShortcut = !itemUse
            && minecraft.options.getCameraType().isFirstPerson()
            && mode == GunShotAnimation.LOWER;
        animation(hand).fire(stack, System.nanoTime(), itemUse || lowerShortcut, minecraft.player.tickCount,
            mode, config.gunRecoil());
        if (!minecraft.options.getCameraType().isFirstPerson()) animation(hand).recoil.reset();
        // Right-click SUCCESS already swings in Minecraft. Shortcuts need the same
        // native swing and packet explicitly, including in third person.
        if (!itemUse) minecraft.player.swing(hand);
        // Mouse use already calls itemUsed in Minecraft. Reuse that native
        // lowering/recovery for shortcuts instead of adding another animation curve.
        if (lowerShortcut) minecraft.gameRenderer.itemInHandRenderer.itemUsed(hand);
    }

    private static boolean validate(Minecraft minecraft) {
        if (owner != minecraft.player || level != minecraft.level) {
            MAIN.clear();
            OFF.clear();
            owner = minecraft.player;
            level = minecraft.level;
        }
        if (owner == null || level == null || !owner.isAlive()) {
            MAIN.clear();
            OFF.clear();
            return false;
        }
        int currentSlot = owner.getInventory().getSelectedSlot();
        if (selectedSlot != currentSlot) {
            MAIN.clear();
            selectedSlot = currentSlot;
        }
        MAIN.validate(owner.getMainHandItem());
        OFF.validate(owner.getOffhandItem());
        if (minecraft.isPaused()
            || !minecraft.options.getCameraType().isFirstPerson()) {
            MAIN.recoil.reset();
            OFF.recoil.reset();
        }
        return true;
    }

    private static boolean isPortalGun(ItemStack stack) {
        return stack.getItem() instanceof PortalGunItem && !PortalGunMode.bucketMode(stack);
    }

    private static HandAnimation animation(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? MAIN : OFF;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poses, LocalPlayer player, HumanoidArm arm,
            ItemStack stack, float partialTick, float equipProcess, float swingProcess) {
        if (!validate(Minecraft.getInstance())) return false;
        InteractionHand hand = arm == player.getMainArm()
            ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        HandAnimation animation = animation(hand);
        if (!animation.matches(stack)) return false;
        if ((player.isUsingItem() && player.getUsedItemHand() == hand)
            || player.isAutoSpinAttack() || player.isHandsBusy()) {
            animation.clear();
            return false;
        }
        long now = System.nanoTime();
        boolean swinging = (player.swinging && player.swingingArm == hand) || swingProcess > 0.0F;
        animation.finishIfSettled(player.tickCount, swinging, equipProcess, now);
        return animation.apply(poses, arm, equipProcess, now, dev.riftgun.client.appearance.SkinRecommendations.animation(stack));
    }

    static void applyPose(PoseStack poses, HumanoidArm arm, float equipProcess,
            float strength, boolean suppressUseEquip, GunRecoilConfig parameters) {
        // ItemInHandRenderer.itemUsed lowers the gun even for a no-swing interaction.
        // Compensation is independent of recoil strength, so OFF can keep the hand still.
        if (suppressUseEquip) poses.translate(0.0F, equipProcess * EQUIP_DROP, 0.0F);
        if (strength == 0.0F) return;
        float x = arm == HumanoidArm.RIGHT ? HAND_X : -HAND_X;
        float y = HAND_Y - equipProcess * EQUIP_DROP;
        poses.translate(x, y, HAND_Z + parameters.maxBackwardOffset() * strength);
        poses.mulPose(Axis.XP.rotationDegrees((float) parameters.maxPitchDegrees() * strength));
        poses.translate(-x, -y, -HAND_Z);
    }

    static final class HandAnimation {
        private final PortalGunRecoil recoil = new PortalGunRecoil();
        private GunRecoilConfig parameters = GunRecoilConfig.defaults();
        private ItemStack stack = ItemStack.EMPTY;
        private UUID id;
        private boolean itemUsed;
        private long itemUsedAt;
        private boolean observedEquipDrop;
        private boolean controlsHand;
        private int startedTick;

        void fire(ItemStack current, long now, boolean itemUse, int gameTick,
                  GunShotAnimation mode, GunRecoilConfig parameters) {
            validate(current);
            stack = current;
            id = PortalGunIdentity.existing(current);
            controlsHand = true;
            startedTick = gameTick;
            this.parameters = parameters;
            if (mode == GunShotAnimation.RECOIL) recoil.fire(now, parameters);
            else recoil.reset();
            if (itemUse) {
                itemUsed = true;
                itemUsedAt = now;
                observedEquipDrop = false;
            }
        }

        void release() {
            controlsHand = false;
        }

        void finishIfSettled(int gameTick, boolean swinging, float equipProcess, long now) {
            // Do not release during the first frame, a slow native swing/recovery,
            // or a long configured recoil. Tick progress also keeps pauses stable.
            if (gameTick > startedTick && !swinging && equipProcess <= 0.0F && recoil.sample(now) == 0.0) {
                clear();
            }
        }

        boolean apply(PoseStack poses, HumanoidArm arm, float equipProcess, long now, GunShotAnimation mode) {
            if (!controlsHand) {
                // No shot reclaimed this input before rendering: the ordinary
                // interaction owns the hand, so discard the old recoil as well.
                clear();
                return false;
            }
            if (mode != GunShotAnimation.RECOIL) recoil.reset();
            if (mode == GunShotAnimation.SWING) return false;
            if (itemUsed) {
                if (equipProcess > 0.0F) observedEquipDrop = true;
                else if (observedEquipDrop) itemUsed = false;
            }
            float strength = (float) recoil.sample(now);
            // OFF must remain still through the entire vanilla recovery, even at low
            // tick rates. A fixed timeout can expire while the hand is still lowered.
            boolean suppressUseEquip = itemUsed && (mode == GunShotAnimation.OFF
                || (mode == GunShotAnimation.RECOIL
                    && now - itemUsedAt < parameters.useEquipRecoveryMillis() * 1_000_000L));
            applyPose(poses, arm, equipProcess, strength, suppressUseEquip, parameters);
            float x = arm == HumanoidArm.RIGHT ? HAND_X : -HAND_X;
            poses.translate(x, HAND_Y - equipProcess * EQUIP_DROP, HAND_Z);
            // Replace only native hand transforms for this shot. NeoForge still calls
            // the same item/model renderer, including all cached geometry and light passes.
            return true;
        }

        boolean matches(ItemStack current) {
            if (stack.isEmpty() || !isPortalGun(current)) return false;
            // Fuel synchronization can replace the ItemStack while a shot is playing.
            return id == null ? current == stack : id.equals(PortalGunIdentity.existing(current));
        }

        void validate(ItemStack current) {
            if (!matches(current)) clear();
        }

        void clear() {
            recoil.reset();
            stack = ItemStack.EMPTY;
            id = null;
            itemUsed = false;
            observedEquipDrop = false;
            controlsHand = false;
        }
    }
}
