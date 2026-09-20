package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalOrientation;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class VoidPortalPlacementTest {
    @BeforeAll
    static void bootstrap() throws ClassNotFoundException {
        try (var flags = mockStatic(net.neoforged.neoforge.common.util.flag.FeatureFlagLoader.class)
            //? if >=1.21.11 {
            /*; var environment = mockStatic(net.neoforged.fml.loading.FMLEnvironment.class)
            *///?}
        ) {
            //? if >=1.21.11 {
            /*environment.when(net.neoforged.fml.loading.FMLEnvironment::isProduction).thenReturn(true);
            *///?}
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
            Class.forName("net.neoforged.neoforge.attachment.AttachmentHolder");
        }
    }

    @Test
    void remoteVoidPlacementSurvivesFinalAndStoredPlacementValidation() {
        ServerLevel level = mock(ServerLevel.class);
        ServerPlayer player = mock(ServerPlayer.class);
        Vec3 eye = new Vec3(0.0, -200.0, 0.0);
        Vec3 end = eye.add(0.0, -12.0, 0.0);
        when(player.level()).thenReturn(level);
        //? if <1.21.11 {
        when(player.serverLevel()).thenReturn(level);
        //?}
        when(player.getEyePosition()).thenReturn(eye);
        when(player.getLookAngle()).thenReturn(new Vec3(0.0, -1.0, 0.0));
        when(level.clip(any(ClipContext.class))).thenReturn(
            BlockHitResult.miss(end, Direction.DOWN, BlockPos.containing(end)));
        when(level.getBlockCollisions(isNull(), any(AABB.class))).thenReturn(List.of());
        var constraints = new PortalPlacementConstraints(12, 12.0,
            PortalPredictionMode.OFF, PortalAperture.EXPANDED);

        for (PortalOrientation orientation : PortalOrientation.values()) {
            var placement = RemotePortalPlacementResolver.resolve(level, player, 12.0,
                PortalAperture.EXPANDED, 60.0F, orientation, 0.5).orElseThrow();

            assertEquals(end, placement.center());
            assertEquals(orientation, placement.orientation());
            assertTrue(placement.geometry().expanded());
            assertTrue(new VanillaPortalPlacementResolver().resolveEntry(player,
                PortalPlacementIntent.remote(placement), constraints).successful());
            assertTrue(PortalStoredPlacementValidator.valid(player, level, placement));
        }
    }
}
