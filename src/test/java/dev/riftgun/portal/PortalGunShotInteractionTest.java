package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.fuel.PortalGunMode;
import dev.riftgun.service.PortalGunIdentity;
import java.util.UUID;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class PortalGunShotInteractionTest {
    @BeforeAll
    static void bootstrap() {
//? if >=1.21.11 {
        /*try (var environment = mockStatic(net.neoforged.fml.loading.FMLEnvironment.class)) {
            environment.when(net.neoforged.fml.loading.FMLEnvironment::isProduction).thenReturn(true);
*///?}
        try (var flags = mockStatic(net.neoforged.neoforge.common.util.flag.FeatureFlagLoader.class)) {
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
        }
//? if >=1.21.11 {
        /*}
*///?}
    }

    @Test
    void portalAndBucketUseKeepTheNativeSwingForThirdPersonObservers() {
        PortalGunItem item = mock(PortalGunItem.class, CALLS_REAL_METHODS);
        Level level = mock(Level.class);
        Player player = mock(Player.class);
        ItemStack stack = mock(ItemStack.class);
        when(level.isClientSide()).thenReturn(true);
        when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(stack);
        try (var modes = mockStatic(PortalGunMode.class)) {
            modes.when(() -> PortalGunMode.bucketMode(stack)).thenReturn(false);
            var shot = item.use(level, player, InteractionHand.MAIN_HAND);
//? if >=1.21.11 {
            /*assertSame(InteractionResult.SUCCESS, shot);
*///?} else {
            assertSame(InteractionResult.SUCCESS, shot.getResult());
            assertTrue(shot.getResult().shouldSwing());
            assertSame(stack, shot.getObject());
//?}
            modes.when(() -> PortalGunMode.bucketMode(stack)).thenReturn(true);
            var bucket = item.use(level, player, InteractionHand.MAIN_HAND);
//? if >=1.21.11 {
            /*assertSame(InteractionResult.SUCCESS, bucket);
*///?} else {
            assertSame(InteractionResult.SUCCESS, bucket.getResult());
//?}
        }
    }

    @Test
    void synchronizedGunDataStaysSteadyButSwitchingGunSlotModeOrSkinStillEquips() {
        PortalGunItem item = mock(PortalGunItem.class, CALLS_REAL_METHODS);
        ItemStack oldStack = mock(ItemStack.class);
        ItemStack newStack = mock(ItemStack.class);
        when(newStack.is(item)).thenReturn(true);
        UUID identity = UUID.randomUUID();
        try (var identities = mockStatic(PortalGunIdentity.class);
             var modes = mockStatic(PortalGunMode.class)) {
            identities.when(() -> PortalGunIdentity.existing(oldStack)).thenReturn(identity);
            identities.when(() -> PortalGunIdentity.existing(newStack)).thenReturn(identity);
            assertFalse(item.shouldCauseReequipAnimation(oldStack, newStack, false));
            assertTrue(item.shouldCauseReequipAnimation(oldStack, newStack, true));
            identities.when(() -> PortalGunIdentity.existing(newStack)).thenReturn(UUID.randomUUID());
            assertTrue(item.shouldCauseReequipAnimation(oldStack, newStack, false));
            identities.when(() -> PortalGunIdentity.existing(newStack)).thenReturn(identity);
            modes.when(() -> PortalGunMode.bucketMode(newStack)).thenReturn(true);
            assertTrue(item.shouldCauseReequipAnimation(oldStack, newStack, false));
            modes.when(() -> PortalGunMode.bucketMode(newStack)).thenReturn(false);
            when(newStack.get(PortalGunComponents.SKIN)).thenReturn("custom");
            assertTrue(item.shouldCauseReequipAnimation(oldStack, newStack, false));
        }
    }
}
