package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.riftgun.core.config.GunRecoilConfig;
import dev.riftgun.core.config.GunShotAnimation;
import dev.riftgun.fuel.PortalGunMode;
import dev.riftgun.network.PortalAction;
import dev.riftgun.portal.PortalGunItem;
import dev.riftgun.service.PortalGunIdentity;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class PortalGunHandAnimationTest {
    @BeforeAll
    static void bootstrap() {
        try (var flags = mockStatic(net.neoforged.neoforge.common.util.flag.FeatureFlagLoader.class)) {
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
        }
    }

    @Test
    void idleLeavesNativeTransformsUntouched() {
        PoseStack poses = new PoseStack();
        var original = new org.joml.Matrix4f(poses.last().pose());
        PortalGunHandAnimation.applyPose(poses, HumanoidArm.RIGHT, 0.7F, 0.0F, false, GunRecoilConfig.defaults());
        assertEquals(original, poses.last().pose());
    }

    @Test
    void disabledRecoilCancelsOnlyTheUseEquipDrop() {
        PoseStack poses = new PoseStack();
        PortalGunHandAnimation.applyPose(poses, HumanoidArm.RIGHT, 0.8F, 0.0F, true, GunRecoilConfig.defaults());
        poses.translate(0.56F, -0.52F - 0.8F * 0.6F, -0.72F);
        Vector3f origin = poses.last().pose().transformPosition(new Vector3f());
        assertEquals(-0.52F, origin.y, 0.00001F);
        assertEquals(-0.72F, origin.z, 0.00001F);
    }

    @Test
    void customDisplacementAndPitchReachTheRenderedPose() {
        PoseStack poses = new PoseStack();
        var parameters = new GunRecoilConfig(35, 145, 0.72, 0.2, 10.0, 200);
        PortalGunHandAnimation.applyPose(poses, HumanoidArm.RIGHT, 0.0F, 0.5F, false, parameters);
        poses.translate(0.56F, -0.52F, -0.72F);
        Vector3f origin = poses.last().pose().transformPosition(new Vector3f());
        Vector3f forward = poses.last().pose().transformDirection(new Vector3f(0, 0, -1));
        assertEquals(-0.62F, origin.z, 0.00001F);
        assertEquals(Math.sin(Math.toRadians(5.0)), forward.y, 0.00001);
    }

    @Test
    void recoilMovesBothHandsBackwardWithTheSameUpwardPitch() {
        for (HumanoidArm arm : HumanoidArm.values()) {
            PoseStack poses = new PoseStack();
            PortalGunHandAnimation.applyPose(poses, arm, 0.0F, 1.0F, false, GunRecoilConfig.defaults());
            float x = arm == HumanoidArm.RIGHT ? 0.56F : -0.56F;
            poses.translate(x, -0.52F, -0.72F);
            Vector3f origin = poses.last().pose().transformPosition(new Vector3f());
            Vector3f forward = poses.last().pose().transformDirection(new Vector3f(0, 0, -1));
            assertEquals(x, origin.x, 0.00001F);
            assertEquals(-0.52F, origin.y, 0.00001F);
            assertTrue(origin.z > -0.72F, "Gun moves toward the camera");
            assertTrue(forward.y > 0.0F, "Muzzle tilts upward");
        }
    }

    @Test
    void offStopsAnActiveRecoilAndSuppressesLateVanillaRecoveryInBothHands() {
        ItemStack gun = mock(ItemStack.class);
        when(gun.getItem()).thenReturn(mock(PortalGunItem.class));
        try (var modes = mockStatic(PortalGunMode.class);
             var identities = mockStatic(PortalGunIdentity.class)) {
            for (HumanoidArm arm : HumanoidArm.values()) {
                var animation = new PortalGunHandAnimation.HandAnimation();
                animation.fire(gun, 0L, true, 0, GunShotAnimation.RECOIL, GunRecoilConfig.defaults());
                assertTrue(animation.matches(gun));
                PoseStack recoilPose = new PoseStack();
                assertTrue(animation.apply(recoilPose, arm, 0.8F, 35_000_000L, GunShotAnimation.RECOIL));
                assertTrue(recoilPose.last().pose().m32() > -0.72F);
                for (long now : new long[] {40_000_000L, 500_000_000L, 2_000_000_000L}) {
                    PoseStack offPose = new PoseStack();
                    assertTrue(animation.apply(offPose, arm, 0.8F, now, GunShotAnimation.OFF),
                        "OFF must skip vanilla shot transforms as well as recoil");
                    Vector3f origin = offPose.last().pose().transformPosition(new Vector3f());
                    assertEquals(arm == HumanoidArm.RIGHT ? 0.56F : -0.56F, origin.x, 0.00001F);
                    assertEquals(-0.52F, origin.y, 0.00001F);
                    assertEquals(-0.72F, origin.z, 0.00001F);
                    Vector3f forward = offPose.last().pose().transformDirection(new Vector3f(0, 0, -1));
                    assertEquals(0.0F, forward.y, 0.00001F);
                }
                // Once use recovery ends, a later equip drop is allowed.
                animation.apply(new PoseStack(), arm, 0.0F, 2_100_000_000L, GunShotAnimation.OFF);
                PoseStack laterEquip = new PoseStack();
                animation.apply(laterEquip, arm, 0.5F, 2_200_000_000L, GunShotAnimation.OFF);
                assertEquals(-0.82F, laterEquip.last().pose().m31(), 0.00001F);
                animation.clear();
                assertFalse(animation.matches(gun), "Unequip or an attack releases the hand");
            }
        }
    }

    @Test
    void handStateUsesExplicitModeAndRecoilParameters() {
        var parameters = new GunRecoilConfig(50, 300, 1.0, 0.2, 0.0, 200);
        ItemStack gun = mock(ItemStack.class);
        when(gun.getItem()).thenReturn(mock(PortalGunItem.class));
        try (var modes = mockStatic(PortalGunMode.class);
             var identities = mockStatic(PortalGunIdentity.class)) {
            var animation = new PortalGunHandAnimation.HandAnimation();
            animation.fire(gun, 0L, true, 0, GunShotAnimation.RECOIL, parameters);
            PoseStack poses = new PoseStack();
            assertTrue(animation.apply(poses, HumanoidArm.RIGHT, 0.0F, 50_000_000L, GunShotAnimation.RECOIL));
            assertEquals(-0.52F, poses.last().pose().m32(), 0.00001F);
            animation.fire(gun, 60_000_000L, true, 1, GunShotAnimation.OFF, parameters);
            PoseStack stopped = new PoseStack();
            assertTrue(animation.apply(stopped, HumanoidArm.RIGHT, 0.0F, 110_000_000L, GunShotAnimation.RECOIL));
            assertEquals(-0.72F, stopped.last().pose().m32(), 0.00001F);
        }
    }

    @Test
    void onlyDispatchedShotShortcutsSwing() throws ReflectiveOperationException {
        try (var scene = new ShotScene();
             var network = mockStatic(dev.riftgun.core.network.RiftNetwork.class)) {
            scene.holdGun(net.minecraft.world.InteractionHand.MAIN_HAND);
            when(scene.config.gunAnimation()).thenReturn(GunShotAnimation.OFF);
            var shots = java.util.EnumSet.of(PortalAction.OPEN_SELECTED, PortalAction.OPEN_SELECTED_SURFACE_FACE,
                PortalAction.OPEN_SELECTED_PRECISION, PortalAction.RELOCATE_ENTITY, PortalAction.PLACE_PAIRING_ENDPOINT);
            dev.riftgun.network.PortalNetworking.setClientRequestListener(PortalGunHandAnimation::onRequest);
            try {
                int expected = 0;
                for (var action : PortalAction.values()) {
                    dev.riftgun.network.PortalNetworking.sendRequest(action);
                    verify(scene.player, org.mockito.Mockito.times(expected)).swing(scene.hand);
                    dev.riftgun.network.PortalNetworking.sendShortcutRequest(action);
                    if (shots.contains(action)) expected++;
                    verify(scene.player, org.mockito.Mockito.times(expected)).swing(scene.hand);
                }
            } finally {
                dev.riftgun.network.PortalNetworking.setClientRequestListener((action, request) -> {});
            }
        }
    }

    @Test
    void swingDelegatesToVanillaWithoutResidualRecoilOrEquipCompensation() {
        ItemStack gun = mock(ItemStack.class);
        try (var modes = mockStatic(PortalGunMode.class);
             var identities = mockStatic(PortalGunIdentity.class)) {
            var animation = new PortalGunHandAnimation.HandAnimation();
            animation.fire(gun, 0L, true, 0, GunShotAnimation.RECOIL, GunRecoilConfig.defaults());
            PoseStack poses = new PoseStack();
            var original = new org.joml.Matrix4f(poses.last().pose());
            assertFalse(animation.apply(poses, HumanoidArm.RIGHT, 0.8F, 35_000_000L, GunShotAnimation.SWING));
            assertEquals(original, poses.last().pose());
        }
    }

    @Test
    void shortcutsSwingInThirdPersonForEveryModeAndOffStillReplacesOnlyTheFirstPersonPose()
            throws ReflectiveOperationException {
        var minecraft = mock(net.minecraft.client.Minecraft.class);
        var player = mock(net.minecraft.client.player.LocalPlayer.class);
        minecraft.player = player;
        minecraft.level = mock(net.minecraft.client.multiplayer.ClientLevel.class);
        var optionsField = net.minecraft.client.Minecraft.class.getField("options");
        optionsField.setAccessible(true);
        optionsField.set(minecraft, mock(net.minecraft.client.Options.class));
        when(player.isAlive()).thenReturn(true);
        when(player.getInventory()).thenReturn(mock(net.minecraft.world.entity.player.Inventory.class));
        when(player.getMainArm()).thenReturn(HumanoidArm.RIGHT);
        ItemStack gun = mock(ItemStack.class);
        when(gun.getItem()).thenReturn(mock(PortalGunItem.class));
        when(player.getMainHandItem()).thenReturn(gun);
        when(player.getOffhandItem()).thenReturn(ItemStack.EMPTY);
        when(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND)).thenReturn(gun);
        var config = mock(dev.riftgun.core.config.ClientVisualConfig.class);
        when(config.gunRecoil()).thenReturn(GunRecoilConfig.defaults());
        var request = new net.minecraft.nbt.CompoundTag();
        request.putBoolean("KeyboardShortcut", true);
        request.putString("Action", "OPEN_SELECTED");
        try (var singletons = mockStatic(net.minecraft.client.Minecraft.class);
             var configs = mockStatic(dev.riftgun.core.config.RiftConfigs.class);
             var modes = mockStatic(PortalGunMode.class);
             var identities = mockStatic(PortalGunIdentity.class)) {
            singletons.when(net.minecraft.client.Minecraft::getInstance).thenReturn(minecraft);
            configs.when(dev.riftgun.core.config.RiftConfigs::client).thenReturn(config);
            for (var mode : GunShotAnimation.values()) {
                when(config.gunAnimation()).thenReturn(mode);
                when(minecraft.options.getCameraType()).thenReturn(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                PortalGunHandAnimation.onRequest(PortalAction.OPEN_SELECTED, request);
            }
            verify(player, org.mockito.Mockito.times(GunShotAnimation.values().length))
                .swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            when(config.gunAnimation()).thenReturn(GunShotAnimation.OFF);
            when(minecraft.options.getCameraType()).thenReturn(net.minecraft.client.CameraType.FIRST_PERSON);
            PoseStack poses = new PoseStack();
            assertTrue(new PortalGunHandAnimation().applyForgeHandTransform(
                poses, player, HumanoidArm.RIGHT, gun, 0.5F, 0.0F, 0.7F));
            assertEquals(-0.72F, poses.last().pose().m32(), 0.00001F);
            when(minecraft.isPaused()).thenReturn(true);
            assertTrue(new PortalGunHandAnimation().applyForgeHandTransform(
                new PoseStack(), player, HumanoidArm.RIGHT, gun, 0.5F, 0.0F, 0.7F),
                "Opening a paused settings screen must not restore the native shot swing");
            when(minecraft.isPaused()).thenReturn(false);
            // A following left-click must recover the vanilla attack path immediately.
            var attack = mock(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered.class);
            when(attack.isAttack()).thenReturn(true);
            PortalGunHandAnimation.interaction(attack);
            assertFalse(new PortalGunHandAnimation().applyForgeHandTransform(
                new PoseStack(), player, HumanoidArm.RIGHT, gun, 0.5F, 0.0F, 0.7F));
        }
    }

    @Test
    void usingTheOtherHandDoesNotDisableOffOrRecoil() throws ReflectiveOperationException {
        try (var scene = new ShotScene()) {
            for (var hand : net.minecraft.world.InteractionHand.values()) {
                scene.holdGun(hand);
                when(scene.player.isUsingItem()).thenReturn(true);
                when(scene.player.getUsedItemHand()).thenReturn(hand == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND);
                for (var mode : new GunShotAnimation[] {
                        GunShotAnimation.OFF, GunShotAnimation.RECOIL, GunShotAnimation.LOWER}) {
                    scene.fire(mode);
                    assertTrue(scene.render(0.7F), "Using the other hand must not release the gun");
                }
                when(scene.player.getUsedItemHand()).thenReturn(hand);
                assertFalse(scene.render(0.7F), "Using the gun hand itself still delegates to vanilla");
            }
        }
    }

    @Test
    void ordinaryRightClickReleasesTheShotBeforeBlockOrEntityInteraction() throws ReflectiveOperationException {
        try (var scene = new ShotScene()) {
            for (var hand : net.minecraft.world.InteractionHand.values()) {
                scene.holdGun(hand);
                scene.fire(GunShotAnimation.OFF);
                assertTrue(scene.render(0.7F));
                var use = mock(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered.class);
                when(use.isUseItem()).thenReturn(true);
                when(use.getHand()).thenReturn(hand);
                PortalGunHandAnimation.interaction(use);
                assertFalse(scene.render(0.7F), "A lever/button/entity use must get its native swing");
                // If the same input falls through to shooting, it takes control again.
                scene.fire(GunShotAnimation.OFF);
                assertTrue(scene.render(0.7F));
            }
        }
    }

    @Test
    void completedShotReleasesTheHandWithoutRequiringAnAttackOrUnequip() throws ReflectiveOperationException {
        try (var scene = new ShotScene()) {
            scene.holdGun(net.minecraft.world.InteractionHand.MAIN_HAND);
            scene.fire(GunShotAnimation.OFF);
            assertTrue(scene.render(0.7F));
            scene.player.tickCount += 20;
            assertTrue(scene.render(0.1F), "Keep suppressing the last interpolated frame of the shot swing");
            assertFalse(scene.render(0.0F), "A settled shot must stop owning later native transforms");
        }
    }

    @Test
    void releasingAnInputAndFiringAgainPreservesContinuousRecoil() {
        ItemStack gun = mock(ItemStack.class);
        when(gun.getItem()).thenReturn(mock(PortalGunItem.class));
        try (var modes = mockStatic(PortalGunMode.class);
             var identities = mockStatic(PortalGunIdentity.class)) {
            var animation = new PortalGunHandAnimation.HandAnimation();
            animation.fire(gun, 0L, true, 0, GunShotAnimation.RECOIL, GunRecoilConfig.defaults());
            var before = new PoseStack();
            animation.apply(before, HumanoidArm.RIGHT, 0.0F, 80_000_000L, GunShotAnimation.RECOIL);
            animation.release();
            animation.fire(gun, 80_000_000L, true, 1, GunShotAnimation.RECOIL, GunRecoilConfig.defaults());
            var after = new PoseStack();
            assertTrue(animation.apply(after, HumanoidArm.RIGHT, 0.0F, 80_000_000L, GunShotAnimation.RECOIL));
            assertTrue(before.last().pose().equals(after.last().pose(), 0.00001F),
                "A normal repeated right-click shot must not restart from rest");
            animation.release();
            assertFalse(animation.apply(new PoseStack(), HumanoidArm.RIGHT, 0.0F, 90_000_000L, GunShotAnimation.RECOIL));
            assertFalse(animation.matches(gun), "An actual ordinary interaction discards the old envelope");
            animation.fire(gun, 100_000_000L, true, 2, GunShotAnimation.RECOIL, GunRecoilConfig.defaults());
            var fresh = new PoseStack();
            animation.apply(fresh, HumanoidArm.RIGHT, 0.0F, 100_000_000L, GunShotAnimation.RECOIL);
            assertEquals(-0.72F, fresh.last().pose().m32(), 0.00001F);
        }
    }

    @Test
    void completionWaitsForNativeSwingEquipRecoveryAndConfiguredRecoil() {
        ItemStack gun = mock(ItemStack.class);
        when(gun.getItem()).thenReturn(mock(PortalGunItem.class));
        try (var modes = mockStatic(PortalGunMode.class);
             var identities = mockStatic(PortalGunIdentity.class)) {
            var animation = new PortalGunHandAnimation.HandAnimation();
            animation.fire(gun, 0L, true, 10, GunShotAnimation.RECOIL, GunRecoilConfig.defaults());
            animation.finishIfSettled(10, false, 0.0F, 0L);
            assertTrue(animation.matches(gun), "The input frame can precede vanilla's swing initialization");
            animation.finishIfSettled(11, false, 0.0F, 35_000_000L);
            assertTrue(animation.matches(gun), "Recoil is still playing");
            animation.finishIfSettled(12, true, 0.0F, 2_000_000_000L);
            assertTrue(animation.matches(gun), "Slow native swing still needs suppression");
            animation.finishIfSettled(13, false, 0.8F, 2_100_000_000L);
            assertTrue(animation.matches(gun), "Slow equip recovery still needs suppression");
            animation.finishIfSettled(14, false, 0.0F, 2_200_000_000L);
            assertFalse(animation.matches(gun), "Release even if no frame observed the original equip drop");
        }
    }

    @Test
    void lowerFollowsNativeDownAndUpHeightWithoutRecoilOrSwing() {
        ItemStack gun = mock(ItemStack.class);
        when(gun.getItem()).thenReturn(mock(PortalGunItem.class));
        try (var modes = mockStatic(PortalGunMode.class);
             var identities = mockStatic(PortalGunIdentity.class)) {
            for (var arm : HumanoidArm.values()) {
                var animation = new PortalGunHandAnimation.HandAnimation();
                animation.fire(gun, 0L, true, 0, GunShotAnimation.RECOIL, GunRecoilConfig.defaults());
                long now = 0L;
                for (float nativeHeight : new float[] {0.0F, 0.5F, 1.0F, 0.5F, 0.0F}) {
                    var poses = new PoseStack();
                    assertTrue(animation.apply(poses, arm, nativeHeight, now, GunShotAnimation.LOWER));
                    var origin = poses.last().pose().transformPosition(new Vector3f());
                    assertEquals(arm == HumanoidArm.RIGHT ? 0.56F : -0.56F, origin.x, 0.00001F);
                    assertEquals(-0.52F - nativeHeight * 0.6F, origin.y, 0.00001F);
                    assertEquals(-0.72F, origin.z, 0.00001F);
                    var forward = poses.last().pose().transformDirection(new Vector3f(0.0F, 0.0F, -1.0F));
                    assertEquals(new Vector3f(0.0F, 0.0F, -1.0F), forward);
                    now += 50_000_000L;
                }
            }
        }
    }

    @Test
    void lowerShortcutsTriggerNativeRecoveryOnceAndMouseUseDoesNotDoubleTrigger()
            throws ReflectiveOperationException {
        try (var scene = new ShotScene()) {
            for (var hand : net.minecraft.world.InteractionHand.values()) {
                scene.holdGun(hand);
                for (var mode : GunShotAnimation.values()) scene.fire(mode);
                verify(scene.itemRenderer).itemUsed(hand);
                var rightClick = mock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem.class);
                when(rightClick.getEntity()).thenReturn(scene.player);
                when(rightClick.getHand()).thenReturn(hand);
                PortalGunHandAnimation.rightClickItem(rightClick);
                verify(scene.itemRenderer).itemUsed(hand);
                assertTrue(scene.render(0.7F), "LOWER replaces the swing while keeping the native height");
            }
            org.mockito.Mockito.verifyNoMoreInteractions(scene.itemRenderer);
        }
    }

    private static final class ShotScene implements AutoCloseable {
        final net.minecraft.client.Minecraft minecraft = mock(net.minecraft.client.Minecraft.class);
        final net.minecraft.client.player.LocalPlayer player = mock(net.minecraft.client.player.LocalPlayer.class);
        final dev.riftgun.core.config.ClientVisualConfig config = mock(dev.riftgun.core.config.ClientVisualConfig.class);
        final ItemStack gun = mock(ItemStack.class);
        final net.minecraft.client.renderer.ItemInHandRenderer itemRenderer =
            mock(net.minecraft.client.renderer.ItemInHandRenderer.class);
        final org.mockito.MockedStatic<net.minecraft.client.Minecraft> singletons = mockStatic(net.minecraft.client.Minecraft.class);
        final org.mockito.MockedStatic<dev.riftgun.core.config.RiftConfigs> configs = mockStatic(dev.riftgun.core.config.RiftConfigs.class);
        final org.mockito.MockedStatic<PortalGunMode> modes = mockStatic(PortalGunMode.class);
        final org.mockito.MockedStatic<PortalGunIdentity> identities = mockStatic(PortalGunIdentity.class);
        net.minecraft.world.InteractionHand hand;

        ShotScene() throws ReflectiveOperationException {
            minecraft.player = player;
            minecraft.level = mock(net.minecraft.client.multiplayer.ClientLevel.class);
            var options = net.minecraft.client.Minecraft.class.getField("options");
            options.setAccessible(true);
            options.set(minecraft, mock(net.minecraft.client.Options.class));
            var gameRenderer = mock(net.minecraft.client.renderer.GameRenderer.class);
            var itemRendererField = net.minecraft.client.renderer.GameRenderer.class.getField("itemInHandRenderer");
            itemRendererField.setAccessible(true);
            itemRendererField.set(gameRenderer, itemRenderer);
            var gameRendererField = net.minecraft.client.Minecraft.class.getField("gameRenderer");
            gameRendererField.setAccessible(true);
            gameRendererField.set(minecraft, gameRenderer);
            when(minecraft.options.getCameraType()).thenReturn(net.minecraft.client.CameraType.FIRST_PERSON);
            when(player.isAlive()).thenReturn(true);
            when(player.getInventory()).thenReturn(mock(net.minecraft.world.entity.player.Inventory.class));
            when(player.getMainArm()).thenReturn(HumanoidArm.RIGHT);
            when(gun.getItem()).thenReturn(mock(PortalGunItem.class));
            when(config.gunRecoil()).thenReturn(GunRecoilConfig.defaults());
            singletons.when(net.minecraft.client.Minecraft::getInstance).thenReturn(minecraft);
            configs.when(dev.riftgun.core.config.RiftConfigs::client).thenReturn(config);
        }

        void holdGun(net.minecraft.world.InteractionHand hand) {
            this.hand = hand;
            when(player.getMainHandItem()).thenReturn(hand == net.minecraft.world.InteractionHand.MAIN_HAND ? gun : ItemStack.EMPTY);
            when(player.getOffhandItem()).thenReturn(hand == net.minecraft.world.InteractionHand.OFF_HAND ? gun : ItemStack.EMPTY);
            when(player.getItemInHand(hand)).thenReturn(gun);
        }

        void fire(GunShotAnimation mode) {
            when(config.gunAnimation()).thenReturn(mode);
            var request = new net.minecraft.nbt.CompoundTag();
            request.putBoolean("KeyboardShortcut", true);
            request.putString("Action", "OPEN_SELECTED");
            PortalGunHandAnimation.onRequest(PortalAction.OPEN_SELECTED, request);
        }

        boolean render(float swing) {
            return new PortalGunHandAnimation().applyForgeHandTransform(new PoseStack(), player,
                hand == net.minecraft.world.InteractionHand.MAIN_HAND ? HumanoidArm.RIGHT : HumanoidArm.LEFT,
                gun, 0.5F, 0.0F, swing);
        }

        @Override
        public void close() {
            identities.close();
            modes.close();
            configs.close();
            singletons.close();
        }
    }
}
